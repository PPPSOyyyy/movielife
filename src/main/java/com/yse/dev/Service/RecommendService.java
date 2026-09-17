package com.yse.dev.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import com.yse.dev.DTO.MovieDto;
import com.yse.dev.DTO.RecommendDto;
import com.yse.dev.Entity.Favorite;
import com.yse.dev.Entity.MovieViewHistory;
import com.yse.dev.Entity.Review;
import com.yse.dev.Repository.FavoriteRepository;
import com.yse.dev.Repository.ReviewRepository;

@Service
public class RecommendService {

    private final MovieService movieService;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final PreferenceService preferenceService;
    private final MovieViewService movieViewService;

    public RecommendService(
            MovieService movieService,
            ReviewRepository reviewRepository,
            FavoriteRepository favoriteRepository,
            PreferenceService preferenceService,
            MovieViewService movieViewService) {
        this.movieService = movieService;
        this.reviewRepository = reviewRepository;
        this.favoriteRepository = favoriteRepository;
        this.preferenceService = preferenceService;
        this.movieViewService = movieViewService;
    }

    /**
     * 개인화 추천 비율
     *
     * 신규/행동 0~2개:
     * 초기 취향 70% + 별점 10% + 찜 8% + 조회 7% + TMDB 5%
     *
     * 학습 중/행동 3~7개:
     * 초기 취향 35% + 별점 30% + 찜 20% + 조회 10% + TMDB 5%
     *
     * 행동 충분/8개 이상:
     * 초기 취향 20% + 별점 35% + 찜 25% + 조회 15% + TMDB 5%
     */
    @Transactional(readOnly = true)
    public Map<String, Object> autoRecommend(String userId) {

        boolean loggedIn = userId != null && !userId.isBlank();

        List<Integer> preferredGenres = loggedIn
                ? preferenceService.getGenreIds(userId)
                : List.of();

        List<Review> reviews = loggedIn
                ? new ArrayList<>(reviewRepository.findByUserIdOrderByCreatedAtDesc(userId))
                : new ArrayList<>();

        List<Favorite> favorites = loggedIn
                ? favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : List.of();

        List<MovieViewHistory> views = loggedIn
                ? movieViewService.topViews(userId, 8)
                : List.of();

        reviews.sort(
                Comparator.comparing(
                        Review::getRating,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ).thenComparing(
                        Review::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        int behaviorSignalCount = reviews.size() + favorites.size() + views.size();
        WeightProfile weights = selectWeights(loggedIn, behaviorSignalCount, preferredGenres.isEmpty());

        Map<Long, Candidate> candidates = new LinkedHashMap<>();
        Set<Long> excludeIds = new LinkedHashSet<>();
        Map<Long, SignalComponents> seedSignals = new LinkedHashMap<>();

        // 별점 기록: 7점 이상을 긍정 신호로 사용합니다.
        for (Review review : reviews) {
            if (review.getMovieId() == null || review.getRating() == null) {
                continue;
            }

            excludeIds.add(review.getMovieId());

            if (review.getRating() >= 7) {
                double strength = Math.min(1.0, review.getRating() / 10.0);
                seedSignals.computeIfAbsent(review.getMovieId(), id -> new SignalComponents())
                        .rating = Math.max(
                                seedSignals.get(review.getMovieId()).rating,
                                strength
                        );
            }
        }

        // 찜은 명확한 관심 신호라 1.0으로 둡니다.
        for (Favorite favorite : favorites) {
            if (favorite.getMovieId() == null) {
                continue;
            }

            excludeIds.add(favorite.getMovieId());
            seedSignals.computeIfAbsent(favorite.getMovieId(), id -> new SignalComponents()).favorite = 1.0;
        }

        // 상세 조회는 조회 횟수가 많을수록 강해지되 1.0을 넘지 않습니다.
        for (MovieViewHistory view : views) {
            if (view.getMovieId() == null) {
                continue;
            }

            int count = view.getViewCount() == null ? 1 : Math.max(1, view.getViewCount());
            double strength = Math.min(1.0, 0.35 + (Math.log1p(count) / Math.log(7.0)));
            seedSignals.computeIfAbsent(view.getMovieId(), id -> new SignalComponents()).view = strength;
        }

        // 1) 가입 당시 고른 장르와 일치하는 후보
        if (!preferredGenres.isEmpty() && weights.preference > 0) {
            addPreferenceCandidates(
                    candidates,
                    preferredGenres,
                    weights,
                    excludeIds
            );
        }

        // 2) 별점/찜/조회 영화의 TMDB 유사 추천 후보
        if (!seedSignals.isEmpty()) {
            addBehaviorCandidates(
                    candidates,
                    seedSignals,
                    weights,
                    excludeIds
            );
        }

        // 3) 부족한 후보는 평점/인기작으로 보충
        if (candidates.size() < 30) {
            addFallbackCandidates(candidates, weights, excludeIds);
        }

        List<Candidate> ranked = new ArrayList<>(candidates.values());
        ranked.sort(
                Comparator
                        .comparingDouble(
                                (Candidate candidate) ->
                                        candidate.finalScore(weights)
                        )
                        .reversed()
                        .thenComparing(
                                (Candidate candidate) ->
                                        candidate.movie.getVoteAverage(),
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
        );

        List<MovieDto> result = ranked.stream()
                .map(candidate -> candidate.movie)
                .limit(20)
                .toList();

        int visible = (result.size() / 5) * 5;
        if (visible == 0 && !result.isEmpty()) {
            visible = result.size();
        }
        result = new ArrayList<>(result.subList(0, visible));

        Map<Long, Double> memberRatings = memberRatings(result);
        List<RecommendDto> movies = result.stream()
                .map(movie -> new RecommendDto(movie, memberRatings.get(movie.getId())))
                .toList();

        String stage;
        String message;

        if (!loggedIn) {
            stage = "인기 기반 추천";
            message = "로그인하면 가입 취향과 활동 기록을 바탕으로 개인화 추천을 받을 수 있어요.";
        } else if (behaviorSignalCount < 3 && !preferredGenres.isEmpty()) {
            stage = "초기 취향 기반";
            message = "가입할 때 고른 장르 비중이 가장 높습니다. 사용 기록이 쌓일수록 실제 행동 비중이 커집니다.";
        } else if (behaviorSignalCount < 8) {
            stage = "취향 학습 중";
            message = "초기 취향과 별점·찜·상세 조회 기록을 함께 반영하고 있어요.";
        } else {
            stage = "행동 기반 개인화";
            message = "별점·찜·상세 조회 기록을 중심으로 추천하고 초기 취향은 보조 신호로 사용합니다.";
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("stage", stage);
        response.put("message", message);
        response.put("behaviorSignals", behaviorSignalCount);
        response.put("preferredGenres", preferredGenres);
        response.put("weights", weights.asMap());
        response.put("movies", movies);
        return response;
    }

    private WeightProfile selectWeights(boolean loggedIn, int signalCount, boolean noPreference) {
        if (!loggedIn) {
            return new WeightProfile(0, 0, 0, 0, 100);
        }

        if (noPreference) {
            if (signalCount < 8) {
                return new WeightProfile(0, 40, 30, 20, 10);
            }
            return new WeightProfile(0, 45, 30, 20, 5);
        }

        if (signalCount < 3) {
            return new WeightProfile(70, 10, 8, 7, 5);
        }

        if (signalCount < 8) {
            return new WeightProfile(35, 30, 20, 10, 5);
        }

        return new WeightProfile(20, 35, 25, 15, 5);
    }

    private void addPreferenceCandidates(
            Map<Long, Candidate> target,
            List<Integer> preferredGenres,
            WeightProfile weights,
            Set<Long> excluded) {

        String genreQuery = preferredGenres.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + "|" + b)
                .orElse("");

        for (int page = 1; page <= 2; page++) {
            try {
                Map<String, Object> raw = movieService.tmdb(
                        "/discover/movie",
                        Map.of(
                                "with_genres", genreQuery,
                                "include_adult", false,
                                "sort_by", "popularity.desc",
                                "vote_count.gte", 100,
                                "page", page
                        )
                );

                for (MovieDto movie : movieService.convertToMovieList(raw)) {
                    if (movie.getId() == null || excluded.contains(movie.getId())) {
                        continue;
                    }

                    long matches = movie.getGenreIds() == null
                            ? 0
                            : movie.getGenreIds().stream().filter(preferredGenres::contains).count();

                    double matchRatio = preferredGenres.isEmpty()
                            ? 0.0
                            : Math.min(1.0, (double) matches / preferredGenres.size());

                    candidate(target, movie).preference = Math.max(
                            candidate(target, movie).preference,
                            matchRatio
                    );
                }
            } catch (RestClientException ignored) {
                break;
            }
        }
    }

    private void addBehaviorCandidates(
            Map<Long, Candidate> target,
            Map<Long, SignalComponents> seedSignals,
            WeightProfile weights,
            Set<Long> excluded) {

        int calls = 0;

        for (Map.Entry<Long, SignalComponents> entry : seedSignals.entrySet()) {
            if (calls >= 9) {
                break;
            }
            calls++;

            Long seedId = entry.getKey();
            SignalComponents signal = entry.getValue();

            try {
                Map<String, Object> raw = movieService.tmdb(
                        "/movie/" + seedId + "/recommendations",
                        Map.of("page", 1)
                );

                for (MovieDto movie : movieService.convertToMovieList(raw)) {
                    if (movie.getId() == null || excluded.contains(movie.getId())) {
                        continue;
                    }

                    Candidate candidate = candidate(target, movie);
                    candidate.rating = Math.max(candidate.rating, signal.rating);
                    candidate.favorite = Math.max(candidate.favorite, signal.favorite);
                    candidate.view = Math.max(candidate.view, signal.view);
                }
            } catch (RestClientException ignored) {
                // 한 영화 추천 API 실패가 전체 추천을 막지 않게 합니다.
            }
        }
    }

    private void addFallbackCandidates(
            Map<Long, Candidate> target,
            WeightProfile weights,
            Set<Long> excluded) {

        for (int page = 1; page <= 2 && target.size() < 40; page++) {
            try {
                for (MovieDto movie : movieService.convertToMovieList(movieService.getTopRatedMovies(page))) {
                    if (movie.getId() != null && !excluded.contains(movie.getId())) {
                        candidate(target, movie);
                    }
                }
            } catch (RestClientException ignored) {
                break;
            }
        }

        for (int page = 1; page <= 2 && target.size() < 40; page++) {
            try {
                for (MovieDto movie : movieService.convertToMovieList(movieService.getPopularMovies(page))) {
                    if (movie.getId() != null && !excluded.contains(movie.getId())) {
                        candidate(target, movie);
                    }
                }
            } catch (RestClientException ignored) {
                break;
            }
        }
    }

    private Candidate candidate(Map<Long, Candidate> target, MovieDto movie) {
        return target.computeIfAbsent(movie.getId(), id -> new Candidate(movie));
    }

    private Map<Long, Double> memberRatings(List<MovieDto> movies) {
        Map<Long, Double> averages = new HashMap<>();
        if (movies.isEmpty()) {
            return averages;
        }

        List<Long> ids = movies.stream().map(MovieDto::getId).toList();
        for (Object[] row : reviewRepository.averageRatingsForMovies(ids)) {
            if (row.length >= 2 && row[0] instanceof Number id && row[1] instanceof Number average) {
                averages.put(id.longValue(), average.doubleValue());
            }
        }
        return averages;
    }

    private static double tmdbQuality(MovieDto movie) {
        double rating = movie.getVoteAverage() == null
                ? 0.0
                : Math.max(0.0, Math.min(1.0, movie.getVoteAverage() / 10.0));

        double votes = movie.getVoteCount() == null
                ? 0.0
                : Math.min(1.0, Math.log1p(movie.getVoteCount()) / Math.log(5001.0));

        double popularity = movie.getPopularity() == null
                ? 0.0
                : Math.min(1.0, Math.log1p(movie.getPopularity()) / Math.log(501.0));

        return (rating * 0.65) + (votes * 0.20) + (popularity * 0.15);
    }

    private static final class Candidate {
        private final MovieDto movie;
        private double preference;
        private double rating;
        private double favorite;
        private double view;

        private Candidate(MovieDto movie) {
            this.movie = movie;
        }

        private double finalScore(WeightProfile weights) {
            return (preference * weights.preference)
                    + (rating * weights.rating)
                    + (favorite * weights.favorite)
                    + (view * weights.view)
                    + (tmdbQuality(movie) * weights.tmdb);
        }
    }

    private static final class SignalComponents {
        private double rating;
        private double favorite;
        private double view;
    }

    private record WeightProfile(
            int preference,
            int rating,
            int favorite,
            int view,
            int tmdb) {

        private Map<String, Integer> asMap() {
            Map<String, Integer> result = new LinkedHashMap<>();
            result.put("initialPreference", preference);
            result.put("rating", rating);
            result.put("favorite", favorite);
            result.put("view", view);
            result.put("tmdb", tmdb);
            return result;
        }
    }
}
