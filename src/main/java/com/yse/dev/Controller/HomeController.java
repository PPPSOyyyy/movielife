package com.yse.dev.Controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.yse.dev.DTO.MovieDetailDto;
import com.yse.dev.DTO.MovieDto;
import com.yse.dev.Entity.Favorite;
import com.yse.dev.Service.CatalogModel;
import com.yse.dev.Service.FavoriteService;
import com.yse.dev.Service.MovieService;
import com.yse.dev.Service.PreferenceService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final FavoriteService favoriteService;
    private final MovieService movieService;
    private final CatalogModel catalogModel;
    private final PreferenceService preferenceService;

    private LocalDate pickDate;
    private MovieDto dailyPick;

    // 한국 시간 기준으로 하루에 한 편을 선택합니다.
    // 같은 날에는 같은 영화를 유지합니다.
    // 후보가 둘 이상이면 이전에 선택한 영화는 제외합니다.
    private synchronized MovieDto dailyMovie(List<MovieDto> movies) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        if (today.equals(pickDate) && dailyPick != null) {
            return dailyPick;
        }

        List<MovieDto> candidates = movies.stream()
                .filter(movie -> movie.getId() != null)
                .sorted(Comparator.comparing(MovieDto::getId))
                .toList();

        if (candidates.isEmpty()) {
            return dailyPick;
        }

        if (dailyPick != null && candidates.size() > 1) {
            Long previousId = dailyPick.getId();

            candidates = candidates.stream()
                    .filter(movie -> !movie.getId().equals(previousId))
                    .toList();
        }

        int index = (int) Math.floorMod(
                today.toEpochDay(),
                (long) candidates.size()
        );

        dailyPick = candidates.get(index);
        pickDate = today;

        return dailyPick;
    }

    // 홈
    @GetMapping("/")
    public String indexPage(HttpSession session, Model model) {
        List<MovieDto> popularMovies = new ArrayList<>();
        List<MovieDto> heroMovies = new ArrayList<>();
        MovieDto bestMovie = null;

        try {
            Map<String, Object> response = movieService.getPopularMovies(1);
            List<MovieDto> movies = movieService.convertToMovieList(response);

            popularMovies.addAll(
                    movies.subList(0, Math.min(5, movies.size()))
            );

            heroMovies.addAll(
                    movies.subList(0, Math.min(20, movies.size()))
            );

            bestMovie = dailyMovie(movies);

        } catch (Exception e) {
            // 영화 API 실패 시 화면의 빈 상태 안내를 사용합니다.
        }

        model.addAttribute("popularMovies", popularMovies);
        model.addAttribute("heroMovies", heroMovies);
        model.addAttribute("bestMovie", bestMovie);

        String userId = (String) session.getAttribute("loginUserId");
        boolean userLoggedIn = userId != null && !userId.isBlank();

        MovieDetailDto myMovie = null;

        if (userLoggedIn) {
            try {
                List<Favorite> favorites = favoriteService.getMyFavorites(userId);

                if (favorites != null && !favorites.isEmpty()) {
                    Favorite latestFavorite = favorites.stream()
                            .filter(favorite -> favorite.getCreatedAt() != null)
                            .max(Comparator.comparing(Favorite::getCreatedAt))
                            .orElse(favorites.get(favorites.size() - 1));

                    myMovie = movieService.getMovieDetail(
                            latestFavorite.getMovieId()
                    );
                }

            } catch (Exception e) {
                // 찜한 영화 정보를 가져오지 못하면 기본 안내를 표시합니다.
            }
        }

        model.addAttribute("userLoggedIn", userLoggedIn);
        model.addAttribute("myMovie", myMovie);

        model.addAttribute("netflixId", 8);
        model.addAttribute("watchaId", 97);
        model.addAttribute("wavveId", 309);
        model.addAttribute("tvingId", 356);

        return "index";
    }

    // 회원가입
    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    // 로그인
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // 마이페이지
    @GetMapping("/mypage")
    public String mypagePage(HttpSession session, Model model) {
        String userId = (String) session.getAttribute("loginUserId");

        if (userId == null) {
            return "redirect:/login?required=true&returnUrl=/mypage";
        }

        // 기존 찜 목록 조회를 재사용하여 마이페이지 카드에 표시합니다.
        favoriteMoviesPage(session, model);
        return "mypage";
    }

    // 취향 설정 페이지
    @GetMapping("/preferences/setup")
    public String preferenceSetupPage(
            HttpSession session,
            Model model) {

        String userId = (String) session.getAttribute("loginUserId");

        if (userId == null) {
            return "redirect:/login?returnUrl=/preferences/setup";
        }

        model.addAttribute("genreOptions", preferenceService.genreOptions());
        model.addAttribute("selectedGenres", preferenceService.getGenreIds(userId));

        return "preference-setup";
    }

    // 영화 추천
    @GetMapping({"/movie-recommend", "/recommend"})
    public String movieRecommendPage(HttpSession session) {
        String userId = (String) session.getAttribute("loginUserId");

        // 로그인 회원인데 아직 최초 취향 설정을 안 했다면 먼저 온보딩으로 이동합니다.
        if (userId != null && !preferenceService.isCompleted(userId)) {
            return "redirect:/preferences/setup";
        }

        return "movie-recommend";
    }

    // OTT 영화 API
    @GetMapping("/api/ott-movies")
    @ResponseBody
    public ResponseEntity<?> getOttMovies(
            @RequestParam(value = "provider", required = false) Integer provider,
            @RequestParam(value = "page", defaultValue = "1") int page) {

        try {
            return ResponseEntity.ok(
                    movieService.convertToMovieList(
                            movieService.getOttMovies(provider, page)
                    )
            );

        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();

            String message;

            if (status == 401 || status == 403) {
                message = "TMDB 인증에 실패했습니다. 서버의 tmdb.api.key 설정을 확인해 주세요.";
            } else if (status == 429) {
                message = "영화 API 요청이 많습니다. 잠시 후 다시 시도해 주세요.";
            } else {
                message = "영화 API가 오류를 반환했습니다. 잠시 후 다시 시도해 주세요.";
            }

            return ResponseEntity.status(502)
                    .body(Map.of("message", message));

        } catch (RestClientException e) {
            return ResponseEntity.status(503)
                    .body(Map.of(
                            "message",
                            "영화 API에 연결할 수 없습니다. 서버의 인터넷 연결을 확인한 뒤 다시 시도해 주세요."
                    ));
        }
    }

    // 인기 영화 / 평점 높은 영화
    @GetMapping("/popular")
    public String popularPage(
            @RequestParam(value = "tab", defaultValue = "popular") String tab,
            Model model) {

        List<MovieDto> rankingMovies = new ArrayList<>();

        try {
            Map<String, Object> response;

            if ("topRated".equals(tab)) {
                response = movieService.getTopRatedMovies(1);
            } else {
                tab = "popular";
                response = movieService.getPopularMovies(1);
            }

            List<MovieDto> movies = movieService.convertToMovieList(response);

            rankingMovies.addAll(
                    movies.subList(0, Math.min(10, movies.size()))
            );

        } catch (Exception e) {
            // 영화 API 실패 시 빈 상태 안내를 사용합니다.
        }

        model.addAttribute("rankingMovies", rankingMovies);
        model.addAttribute("activeTab", tab);

        List<MovieDto> upcomingMovies = new ArrayList<>();

        try {
            Map<String, Object> response = movieService.getUpcomingMovies(1);
            List<MovieDto> movies = movieService.convertToMovieList(response);

            upcomingMovies.addAll(
                    movies.subList(0, Math.min(4, movies.size()))
            );

        } catch (Exception e) {
            // 영화 API 실패 시 빈 상태 안내를 사용합니다.
        }

        model.addAttribute("upcomingMovies", upcomingMovies);

        return "popular";
    }

    // OTT 페이지
    @GetMapping("/ott")
    public String ottPage(Model model) {
        model.addAttribute("netflixId", 8);
        model.addAttribute("watchaId", 97);
        model.addAttribute("wavveId", 309);
        model.addAttribute("tvingId", 356);

        return "ott";
    }

    // 찜한 영화
    @GetMapping("/favorite-movies")
    public String favoriteMoviesPage(HttpSession session, Model model) {
        String userId = (String) session.getAttribute("loginUserId");

        if (userId == null) {
            return "redirect:/login?required=true&returnUrl=/favorite-movies";
        }

        List<Favorite> favorites = favoriteService.getMyFavorites(userId);

        // 찜한 영화는 카드에 필요한 요약 정보만 병렬로 조회합니다.
        // (기존: 영화마다 상세 5회 순차 호출)
        List<Supplier<MovieDto>> tasks = new ArrayList<>();

        for (Favorite favorite : favorites) {
            tasks.add(() -> {
                try {
                    return movieService.getMovieSummary(favorite.getMovieId());

                } catch (Exception e) {
                    MovieDto movie = new MovieDto();

                    movie.setId(favorite.getMovieId());
                    movie.setTitle("영화 정보를 불러올 수 없습니다.");
                    movie.setGenreIds(new ArrayList<>());
                    movie.setOttProviders(new ArrayList<>());

                    return movie;
                }
            });
        }

        List<MovieDto> favoriteMovies = new ArrayList<>(movieService.parallel(tasks));

        try {
            catalogModel.attachMemberRatings(favoriteMovies);
        } catch (Exception e) {
            // 회원 별점 조회가 실패해도 찜 목록은 표시합니다.
        }

        model.addAttribute("favoriteMovies", favoriteMovies);

        return "favorite-movies";
    }

    // 내가 작성한 리뷰
    @GetMapping("/my-reviews")
    public String myReviewsPage(HttpSession session) {
        String userId = (String) session.getAttribute("loginUserId");

        if (userId == null) {
            return "redirect:/login?required=true&returnUrl=/my-reviews";
        }

        return "my-reviews";
    }

    // 프로필 수정
    @GetMapping("/profile")
    public String profilePage(HttpSession session) {
        String userId = (String) session.getAttribute("loginUserId");

        if (userId == null) {
            return "redirect:/login?required=true&returnUrl=/profile";
        }

        return "profile";
    }

    @GetMapping("/api/home-favorite") @ResponseBody
    public ResponseEntity<?> homeFavorite(@RequestParam(name="index", defaultValue="0") int index,HttpSession session){
        String id=(String)session.getAttribute("loginUserId");if(id==null)return ResponseEntity.status(401).body(Map.of("message","로그인이 필요합니다."));
        List<Favorite> items=favoriteService.getMyFavorites(id);
        if(items.isEmpty())return ResponseEntity.ok(Map.of("count",0));
        int at=Math.floorMod(index,items.size());
        try {return ResponseEntity.ok(Map.of("count",items.size(),"index",at,"movie",movieService.getMovieSummary(items.get(at).getMovieId())));}
        catch(RestClientException e){return ResponseEntity.status(503).body(Map.of("message","찜한 영화 정보를 불러오지 못했습니다."));}
    }


    @GetMapping("/api/ott-page") @ResponseBody
    public ResponseEntity<?> ottPageData(
            @RequestParam(name="provider", required=false) Integer provider,
            @RequestParam(name="page", defaultValue="1") int page) {

        // 화면의 한 페이지는 20편(5열 x 4행)을 목표로 합니다.
        // TMDB 한 페이지에서 국내 관람등급/성인 필터 후 편수가 줄 수 있으므로
        // 화면 1페이지당 TMDB 최대 4페이지를 묶어 충분한 후보를 확보합니다.
        page = Math.max(1, Math.min(125, page));
        int firstApiPage = (page - 1) * 4 + 1;

        try {
            List<Object> combined = new ArrayList<>();
            int totalApiPages = firstApiPage;
            int lastFetchedPage = firstApiPage - 1;

            for (int apiPage = firstApiPage; apiPage <= Math.min(firstApiPage + 3, totalApiPages); apiPage++) {
                Map<String,Object> raw = movieService.getOttMovies(provider, apiPage);

                if (apiPage == firstApiPage && raw.get("total_pages") instanceof Number n) {
                    totalApiPages = Math.max(1, Math.min(500, n.intValue()));
                }

                if (raw.get("results") instanceof List<?> rows) {
                    combined.addAll(rows);
                }

                lastFetchedPage = apiPage;
            }

            List<MovieDto> converted = movieService.convertToMovieList(
                    Map.of("results", combined),
                    ""
            );
            LinkedHashMap<Long, MovieDto> unique = new LinkedHashMap<>();
            for (MovieDto movie : converted) {
                unique.putIfAbsent(movie.getId(), movie);
            }

            List<MovieDto> visible = new ArrayList<>(unique.values());
            int visibleCount = Math.min(20, (visible.size() / 5) * 5);

            // 아주 마지막 구간에서 5편 미만만 남은 경우에는 결과 자체를
            // 없애지 않기 위해 그대로 보여 줍니다. 일반 페이지는 5개 단위입니다.
            if (visibleCount == 0 && !visible.isEmpty()) {
                visibleCount = visible.size();
            }

            if (visible.size() > visibleCount) {
                visible = new ArrayList<>(visible.subList(0, visibleCount));
            }

            boolean hasMore = lastFetchedPage < totalApiPages;

            return ResponseEntity.ok(
                    Map.of(
                            "movies", visible,
                            "hasMore", hasMore
                    )
            );

        } catch(RestClientException e) {
            return ResponseEntity.status(503)
                    .body(Map.of(
                            "message",
                            "OTT 영화를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."
                    ));
        }
    }
}