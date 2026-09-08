package com.yse.dev.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.yse.dev.DTO.MovieDetailDto;
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

    @GetMapping("/")
    public String indexPage(Model model) {

        List<com.yse.dev.DTO.MovieDto> popularMovies = new ArrayList<>();

        try {
            Map<String, Object> response = movieService.getPopularMovies(1);
            List<com.yse.dev.DTO.MovieDto> movies =
                    movieService.convertToMovieList(response);

            int endIndex = Math.min(5, movies.size());
            popularMovies.addAll(movies.subList(0, endIndex));

        } catch (Exception e) {
            System.out.println("홈 인기영화 조회 실패: " + e.getMessage());
        }

        model.addAttribute("popularMovies", popularMovies);

        return "index";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/mypage")
    public String mypagePage(HttpSession session) {
        String userId = (String) session.getAttribute("loginUserId");

        if (userId == null) {
            return "redirect:/login?required=true";
        }

        return "mypage";
    }

    @GetMapping({"/movie-recommend", "/recommend"})
    public String movieRecommendPage() {
        return "movie-recommend";
    }

    @GetMapping("/popular")
    public String popularPage() {
        return "popular";
    }

    @GetMapping("/ott")
    public String ottPage() {
        return "ott";
    }

    @GetMapping("/favorite-movies")
    public String favoriteMoviesPage(
            HttpSession session,
            Model model) {

        String userId = (String) session.getAttribute("loginUserId");

        if (userId == null) {
            return "redirect:/login?required=true";
        }

        List<Favorite> favorites =
                favoriteService.getMyFavorites(userId);

        List<MovieDetailDto> favoriteMovies =
                new ArrayList<>();

        for (Favorite favorite : favorites) {
            try {
                MovieDetailDto movie =
                        movieService.getMovieDetail(
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

    @GetMapping("/my-reviews")
    public String myReviewsPage(HttpSession session) {
        String userId = (String) session.getAttribute("loginUserId");

        if (userId == null) {
            return "redirect:/login?required=true";
        }

        return "my-reviews";
    }

    @GetMapping("/profile")
    public String profilePage(HttpSession session) {
        String userId = (String) session.getAttribute("loginUserId");

        if (userId == null) {
            return "redirect:/login?required=true";
        }

        return "profile";
    }
}
