package com.yse.dev.Service;

import com.yse.dev.DTO.CatalogFilter;
import com.yse.dev.DTO.MovieDto;
import com.yse.dev.Repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class CatalogModel {

    /** 한 화면에 묶어 보여주는 TMDB 페이지 수 (20편 × 4 = 최대 80편) */
    private static final int PAGES_PER_SCREEN = 4;

    private final MovieService movies;
    private final CatalogService catalog;
    private final ReviewRepository reviewRepository;


    public void populate(
            CatalogFilter f,
            String country,
            Model model) {

        f.normalize(country != null);

        List<MovieDto> list = List.of();
        String error = null;
        String peopleError = null;
        List<Map<?, ?>> people = List.of();

        int pages = 1;
        int totalResults = 0;

        try {

            f.setPage(Math.min(500 / PAGES_PER_SCREEN, f.getPage()));

            boolean searching = !f.getQuery().isBlank();

            int first = (f.getPage() - 1) * PAGES_PER_SCREEN + 1;

            // =================================================
            // 1차 병렬 요청: 영화 4페이지 (+ 검색 시 인물 4페이지)
            //
            // TMDB 는 total_pages 를 넘는 페이지에 빈 results 를 돌려주므로
            // 첫 페이지 응답을 기다리지 않고 한 번에 요청합니다.
            // =================================================

            List<Supplier<Map<String, Object>>> tasks = new ArrayList<>();

            for (int apiPage = first; apiPage < first + PAGES_PER_SCREEN; apiPage++) {
                int p = apiPage;
                tasks.add(() -> catalog.searchPage(f, country, p));
            }

            int movieTaskCount = tasks.size();

            if (searching) {

                for (int apiPage = first; apiPage < first + PAGES_PER_SCREEN; apiPage++) {
                    int p = apiPage;
                    tasks.add(() -> safePeople(f.getQuery(), p));
                }

                // 출연작 검색에 쓸 인물 후보는 항상 1페이지 기준으로 고릅니다.
                if (first != 1) {
                    tasks.add(() -> safePeople(f.getQuery(), 1));
                }
            }

            List<Map<String, Object>> responses = movies.parallel(tasks);

            List<Map<String, Object>> moviePages = responses.subList(0, movieTaskCount);
            List<Map<String, Object>> personPages = responses.subList(movieTaskCount, responses.size());

            Map<String, Object> result = moviePages.get(0);

            int last = Math.min(500, count(result, "total_pages"));

            pages = Math.max(1, (last + PAGES_PER_SCREEN - 1) / PAGES_PER_SCREEN);

            totalResults = count(result, "total_results");

            // 목록 페이지가 실제 범위를 넘으면 마지막 화면으로 되돌립니다.
            if (!searching && f.getPage() > pages) {

                f.setPage(pages);
                first = (pages - 1) * PAGES_PER_SCREEN + 1;

                List<Supplier<Map<String, Object>>> again = new ArrayList<>();

                for (int apiPage = first; apiPage < first + PAGES_PER_SCREEN; apiPage++) {
                    int p = apiPage;
                    again.add(() -> catalog.searchPage(f, country, p));
                }

                moviePages = movies.parallel(again);
            }

            List<Object> combined = new ArrayList<>();

            for (Map<String, Object> pageResult : moviePages) {
                if (pageResult.get("results") instanceof List<?> raw) {
                    combined.addAll(raw);
                }
            }

            // 제목 검색으로 받은 영화 (검색 + OTT 필터일 때 제공 여부를 따로 확인)
            int titleMatchCount = combined.size();

            // =================================================
            // 배우·감독 이름 검색
            //
            // 인물 검색 1페이지에서 검색어와 이름이 맞는 인물을 고르고
            // 그 인물들이 참여한 영화를 상세 필터와 함께 조회해 뒤에 붙입니다.
            // =================================================

            if (searching) {

                List<Map<?, ?>> strip = new ArrayList<>();
                int peopleRawPages = 0;
                boolean peopleFailed = false;
                Map<String, Object> firstPersonPage = null;

                for (int i = 0; i < personPages.size(); i++) {

                    Map<String, Object> personResult = personPages.get(i);

                    if (personResult == null) {
                        peopleFailed = true;
                        continue;
                    }

                    if (i < PAGES_PER_SCREEN) {
                        strip.addAll(catalog.visiblePeople(personResult));
                        peopleRawPages = Math.max(peopleRawPages, Math.min(500, count(personResult, "total_pages")));
                    }

                    if (count(personResult, "page") == 1) {
                        firstPersonPage = personResult;
                    }
                }

                people = strip;

                if (peopleFailed && strip.isEmpty()) {
                    peopleError = "인물 검색을 불러오지 못했습니다.";
                }

                pages = Math.max(pages, Math.max(1, (peopleRawPages + PAGES_PER_SCREEN - 1) / PAGES_PER_SCREEN));

                List<Long> personIds = firstPersonPage == null
                        ? List.of()
                        : catalog.matchingPeople(f.getQuery(), firstPersonPage);

                if (!personIds.isEmpty()) {

                    List<Supplier<Map<String, Object>>> creditTasks = new ArrayList<>();

                    for (int apiPage = first; apiPage < first + PAGES_PER_SCREEN; apiPage++) {
                        int p = apiPage;
                        creditTasks.add(() -> catalog.peopleMoviesPage(f, country, personIds, p));
                    }

                    try {

                        List<Map<String, Object>> creditPages = movies.parallel(creditTasks);

                        int creditLast = Math.min(500, count(creditPages.get(0), "total_pages"));

                        pages = Math.max(pages, Math.max(1, (creditLast + PAGES_PER_SCREEN - 1) / PAGES_PER_SCREEN));

                        totalResults += count(creditPages.get(0), "total_results");

                        for (Map<String, Object> pageResult : creditPages) {
                            if (pageResult.get("results") instanceof List<?> raw) {
                                combined.addAll(raw);
                            }
                        }

                    } catch (RestClientException e) {

                        error = "일부 영화를 불러오지 못했습니다. 다시 시도해 주세요.";
                    }
                }
            }

            list = movies.convertToMovieList(
                    Map.of("results", combined),
                    f.getCertification()
            );

            // =================================================
            // 제목 검색 결과에 상세 필터 적용
            //
            // TMDB 검색 API 는 장르·평점·연도·OTT 조건을 받지 않으므로 서버에서 거릅니다.
            // 인물 출연작(discover)은 이미 조건이 적용되어 있습니다.
            // =================================================

            Set<Long> titleMatches = new HashSet<>();

            for (Object raw : combined.subList(0, titleMatchCount)) {
                if (raw instanceof Map<?, ?> m && m.get("id") instanceof Number id) {
                    titleMatches.add(id.longValue());
                }
            }

            Set<Long> providerOk = searching && f.getProvider() != null
                    ? providerAvailability(list, titleMatches, f.getProvider())
                    : null;

            LinkedHashMap<Long, MovieDto> unique = new LinkedHashMap<>();

            for (MovieDto movie : list) {

                boolean keep = !searching
                        || !titleMatches.contains(movie.getId())
                        || (matches(movie, f) && (providerOk == null || providerOk.contains(movie.getId())));

                if (keep) {
                    unique.putIfAbsent(movie.getId(), movie);
                }
            }

            list = new ArrayList<>(unique.values());

            // =================================================
            // MovieLife 회원 평균 별점 / 참여 수 연결
            // =================================================

            applyMemberRatings(list);

            // =================================================
            // 추천순
            //
            // 1) 회원 별점이 있는 영화가 먼저
            // 2) 회원 평균 별점이 높은 순
            // 3) 같은 평균이면 참여 수가 많은 순
            // 4) 회원 별점이 없는 영화는 기존 TMDB 추천 점수 순
            // =================================================

            if ("recommended".equals(f.getSort()) && f.getQuery().isBlank()) {
                list.sort(this::compareRecommended);
            }

            // 데스크톱 5열 기준으로 마지막 줄이 1~4개만 남지 않도록
            // 화면에 넘기는 영화 수를 5의 배수로 맞춥니다.
            if (list.size() > 5 && list.size() % 5 != 0) {
                int fullRowCount = (list.size() / 5) * 5;
                list = new ArrayList<>(list.subList(0, fullRowCount));
            }

        } catch (RestClientException e) {

            error = "영화 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";
        }

        pages = Math.max(f.getPage(), pages);

        model.addAttribute("movies", list);
        model.addAttribute("catalogError", error);
        model.addAttribute("people", people);
        model.addAttribute("peopleError", peopleError);
        model.addAttribute("filter", f);
        model.addAttribute("query", f.getQuery());
        model.addAttribute("activeTab", f.getTab());
        model.addAttribute("selectedGenre", f.getGenre());
        model.addAttribute("selectedMinRating", f.getMinRating());
        model.addAttribute("selectedYear", f.getYear());
        model.addAttribute("selectedProvider", f.getProvider());
        model.addAttribute("currentYear", java.time.LocalDate.now().getYear());
        model.addAttribute("currentPage", f.getPage());
        model.addAttribute("totalPages", pages);
        model.addAttribute("totalResults", totalResults);

        int start = Math.max(1, Math.min(f.getPage() - 2, pages - 4));

        model.addAttribute("startPage", start);
        model.addAttribute("endPage", Math.min(pages, start + 4));
    }


    /**
     * 인물 검색은 실패해도 영화 목록을 막지 않도록 null 로 돌려줍니다.
     */
    private Map<String, Object> safePeople(String query, int apiPage) {

        try {
            return catalog.searchPeople(query, apiPage);
        } catch (RestClientException e) {
            return null;
        }
    }


    /**
     * 제목 검색 결과 중 선택한 OTT 에서 볼 수 있는 영화 ID 집합을 병렬로 확인합니다.
     */
    private Set<Long> providerAvailability(
            List<MovieDto> list,
            Set<Long> titleMatches,
            int providerId) {

        Set<Long> distinct = new java.util.LinkedHashSet<>();

        for (MovieDto movie : list) {
            if (movie.getId() != null && titleMatches.contains(movie.getId())) {
                distinct.add(movie.getId());
            }
        }

        List<Long> ids = new ArrayList<>(distinct);

        List<Supplier<Boolean>> tasks = new ArrayList<>();

        for (Long id : ids) {
            tasks.add(() -> movies.hasKrProvider(id, providerId));
        }

        List<Boolean> flags = movies.parallel(tasks);

        Set<Long> ok = new HashSet<>();

        for (int i = 0; i < ids.size(); i++) {
            if (Boolean.TRUE.equals(flags.get(i))) {
                ok.add(ids.get(i));
            }
        }

        return ok;
    }


    // =========================================================
    // 회원 별점 통계 연결
    // =========================================================

    private void applyMemberRatings(
            List<MovieDto> list) {

        if (
                list == null
                ||
                list.isEmpty()
        ) {

            return;
        }

        List<Long> movieIds =
                list.stream()
                        .map(MovieDto::getId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList();

        if (movieIds.isEmpty()) {
            return;
        }

        Map<Long, RatingStats> statsMap =
                new HashMap<>();

        for (
                Object[] row :
                reviewRepository.ratingStatsForMovies(
                        movieIds
                )
        ) {

            if (
                    row == null
                    ||
                    row.length < 3
                    ||
                    !(row[0] instanceof Number movieId)
            ) {

                continue;
            }

            double average =
                    row[1] instanceof Number n
                            ? n.doubleValue()
                            : 0.0;

            long ratingCount =
                    row[2] instanceof Number n
                            ? n.longValue()
                            : 0L;

            statsMap.put(
                    movieId.longValue(),
                    new RatingStats(
                            average,
                            ratingCount
                    )
            );
        }

        for (MovieDto movie : list) {

            RatingStats stats =
                    statsMap.get(
                            movie.getId()
                    );

            if (stats == null) {

                movie.setMemberRatingAverage(null);
                movie.setMemberRatingCount(0L);

            } else {

                movie.setMemberRatingAverage(
                        stats.average()
                );

                movie.setMemberRatingCount(
                        stats.count()
                );
            }
        }
    }


    /**
     * 찜 목록 등 다른 화면에서도 회원 별점을 붙일 수 있도록 공개합니다.
     */
    public void attachMemberRatings(List<MovieDto> list) {
        applyMemberRatings(list);
    }


    // =========================================================
    // 추천 정렬
    // =========================================================

    private int compareRecommended(
            MovieDto a,
            MovieDto b) {

        boolean aHasMemberRating =
                hasMemberRating(a);

        boolean bHasMemberRating =
                hasMemberRating(b);

        // 회원 평가가 있는 영화는 없는 영화보다 먼저
        if (
                aHasMemberRating
                !=
                bHasMemberRating
        ) {

            return aHasMemberRating
                    ? -1
                    : 1;
        }

        // 둘 다 회원 평가가 있다면 평균 별점 높은 순
        if (
                aHasMemberRating
                &&
                bHasMemberRating
        ) {

            int averageCompare =
                    Double.compare(
                            safeMemberAverage(
                                    b
                            ),
                            safeMemberAverage(
                                    a
                            )
                    );

            if (averageCompare != 0) {
                return averageCompare;
            }

            // 평균이 같으면 평가 참여 수 많은 순
            int countCompare =
                    Long.compare(
                            safeMemberCount(
                                    b
                            ),
                            safeMemberCount(
                                    a
                            )
                    );

            if (countCompare != 0) {
                return countCompare;
            }
        }

        // 회원 평가가 없거나 회원 평가가 동률이면
        // 기존 TMDB 추천 점수 사용
        return Double.compare(
                recommendScore(b),
                recommendScore(a)
        );
    }


    private boolean hasMemberRating(
            MovieDto movie) {

        return movie != null
                &&
                movie.getMemberRatingAverage() != null
                &&
                safeMemberCount(movie) > 0;
    }


    private double safeMemberAverage(
            MovieDto movie) {

        return movie.getMemberRatingAverage() == null
                ? 0.0
                : movie.getMemberRatingAverage();
    }


    private long safeMemberCount(
            MovieDto movie) {

        return movie.getMemberRatingCount() == null
                ? 0L
                : movie.getMemberRatingCount();
    }


    // =========================================================
    // 기존 TMDB 자동 추천 점수
    // =========================================================

    private double recommendScore(
            MovieDto movie) {

        double rating =
                movie.getVoteAverage() == null
                        ? 0.0
                        : movie.getVoteAverage();

        int votes =
                movie.getVoteCount() == null
                        ? 0
                        : Math.max(
                                0,
                                movie.getVoteCount()
                        );

        double popularity =
                movie.getPopularity() == null
                        ? 0.0
                        : Math.max(
                                0.0,
                                movie.getPopularity()
                        );

        // 평가 수가 적은 영화의 과도한 고평점을 완화
        double globalMean = 6.5;
        double minVotes = 200.0;

        double weightedRating =
                (votes / (votes + minVotes))
                        * rating
                        +
                (minVotes / (votes + minVotes))
                        * globalMean;

        // 평가 수와 인기도는 로그 보정
        double voteSignal =
                Math.log1p(votes);

        double popularitySignal =
                Math.log1p(popularity);

        return weightedRating * 10.0
                + voteSignal * 2.0
                + popularitySignal * 1.5;
    }


    // =========================================================
    // 제목 검색 결과 상세 필터 (OTT 제공 여부는 providerAvailability 에서 확인)
    // =========================================================

    private boolean matches(
            MovieDto m,
            CatalogFilter f) {

        if (
                f.getMinRating() != null
                &&
                (
                        m.getVoteAverage() == null
                        ||
                        m.getVoteAverage()
                                < f.getMinRating()
                )
        ) {

            return false;
        }

        if (
                f.getGenre() != null
                &&
                (
                        m.getGenreIds() == null
                        ||
                        !m.getGenreIds()
                                .contains(
                                        f.getGenre()
                                )
                )
        ) {

            return false;
        }

        double v =
                m.getVoteAverage() == null
                        ? 0
                        : m.getVoteAverage();

        if (
                "under5".equals(
                        f.getRating()
                )
                &&
                v >= 5
        ) {

            return false;
        }

        if (
                "five".equals(
                        f.getRating()
                )
                &&
                (
                        v < 5
                        ||
                        v >= 6
                )
        ) {

            return false;
        }

        if (
                List.of(
                        "5",
                        "6",
                        "7",
                        "8",
                        "9"
                ).contains(
                        f.getRating()
                )
                &&
                v < Double.parseDouble(
                        f.getRating()
                )
        ) {

            return false;
        }

        String d =
                m.getReleaseDate();

        int y =
                d != null
                &&
                d.matches(
                        "\\d{4}.*"
                )
                        ? Integer.parseInt(
                                d.substring(
                                        0,
                                        4
                                )
                        )
                        : 0;

        return (
                f.getYear() == null
                ||
                y == f.getYear()
        )
        &&
        (
                f.getStartYear() == null
                ||
                y >= f.getStartYear()
        )
        &&
        (
                f.getEndYear() == null
                ||
                y <= f.getEndYear()
        );
    }


    private int count(
            Map<String, Object> r,
            String key) {

        return r.get(key)
                instanceof Number n
                ? n.intValue()
                : 0;
    }


    private record RatingStats(
            double average,
            long count) {
    }
}
