package com.yse.dev.Controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
import com.yse.dev.Service.FavoriteService;
import com.yse.dev.Service.MovieService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final FavoriteService favoriteService;
    private final MovieService movieService;

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
    public String mypagePage(HttpSession session) {
        String userId = (String) session.getAttribute("loginUserId");

        if (userId == null) {
            return "redirect:/login?required=true&returnUrl=/mypage";
        }

        return "mypage";
    }

    // 영화 추천
    @GetMapping({"/movie-recommend", "/recommend"})
    public String movieRecommendPage() {
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
        List<MovieDetailDto> favoriteMovies = new ArrayList<>();

        for (Favorite favorite : favorites) {
            try {
                MovieDetailDto movie = movieService.getMovieDetail(
                        favorite.getMovieId()
                );

                favoriteMovies.add(movie);

            } catch (Exception e) {
                MovieDetailDto movie = new MovieDetailDto();

                movie.setId(favorite.getMovieId());
                movie.setTitle("영화 정보를 불러올 수 없습니다.");

                favoriteMovies.add(movie);
            }
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
}