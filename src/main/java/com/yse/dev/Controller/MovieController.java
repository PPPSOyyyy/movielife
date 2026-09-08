package com.yse.dev.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.yse.dev.DTO.MovieDetailDto;
import com.yse.dev.DTO.MovieDto;
import com.yse.dev.Service.MovieService;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class MovieController {


    private final MovieService movieService;



    // ==========================================
    // 영화
    // 전체 영화 + 검색 + 복합필터
    // ==========================================

    @GetMapping("/movies")
    public String movieList(

            @RequestParam(
                    value = "query",
                    required = false
            )
            String query,

            @RequestParam(
                    value = "genre",
                    required = false
            )
            Integer genre,

            @RequestParam(
                    value = "minRating",
                    required = false
            )
            Double minRating,

            @RequestParam(
                    value = "year",
                    required = false
            )
            Integer year,

            @RequestParam(
                    value = "provider",
                    required = false
            )
            Integer provider,

            @RequestParam(
                    value = "page",
                    defaultValue = "1"
            )
            int page,

            Model model) {


        if (page < 1) {
            page = 1;
        }


        Map<String, Object> response;

        String pageTitle =
                "전체 영화";


        // ==========================================
        // 제목 검색
        // ==========================================

        if (
            query != null &&
            !query.trim().isEmpty()
        ) {


            response =
                    movieService.searchMovies(
                            query.trim(),
                            page
                    );


            pageTitle =
                    "'" + query.trim()
                    + "' 검색 결과";

        }


        // ==========================================
        // 복합 필터
        // ==========================================

        else if (
            genre != null ||
            minRating != null ||
            year != null ||
            provider != null
        ) {


            response =
                    movieService.getFilteredMovies(

                            genre,

                            minRating,

                            year,

                            provider,

                            page

                    );


            pageTitle =
                    "필터 검색 결과";

        }


        // ==========================================
        // 기본 전체 영화
        // 인기영화 전용 API를 사용하지 않음
        // ==========================================

        else {


            response =
                    movieService.getFilteredMovies(

                            null,
                            null,
                            null,
                            null,
                            page

                    );

        }


        addMovieListModel(
                model,
                response,
                page,
                pageTitle,
                "all",
                query,
                genre,
                minRating,
                year,
                provider
        );


        return "movie-list";
    }



    // ==========================================
    // 인기 영화
    // TMDB Popular
    // ==========================================

    @GetMapping("/movies/popular")
    public String popularMovies(

            @RequestParam(
                    value = "page",
                    defaultValue = "1"
            )
            int page,

            Model model) {


        if (page < 1) {
            page = 1;
        }


        Map<String, Object> response =
                movieService.getPopularMovies(
                        page
                );


        addMovieListModel(
                model,
                response,
                page,
                "현재 인기 영화",
                "popular",
                null,
                null,
                null,
                null,
                null
        );


        return "movie-list";
    }



    // ==========================================
    // 평점 높은 영화
    // TMDB Top Rated
    // ==========================================

    @GetMapping("/movies/top-rated")
    public String topRatedMovies(

            @RequestParam(
                    value = "page",
                    defaultValue = "1"
            )
            int page,

            Model model) {


        if (page < 1) {
            page = 1;
        }


        Map<String, Object> response =
                movieService.getTopRatedMovies(
                        page
                );


        addMovieListModel(
                model,
                response,
                page,
                "평점 높은 영화",
                "topRated",
                null,
                null,
                null,
                null,
                null
        );


        return "movie-list";
    }



    // ==========================================
    // 개봉 예정 영화
    // TMDB Upcoming
    // ==========================================

    @GetMapping("/movies/upcoming")
    public String upcomingMovies(

            @RequestParam(
                    value = "page",
                    defaultValue = "1"
            )
            int page,

            Model model) {


        if (page < 1) {
            page = 1;
        }


        Map<String, Object> response =
                movieService.getUpcomingMovies(
                        page
                );


        addMovieListModel(
                model,
                response,
                page,
                "개봉 예정 영화",
                "upcoming",
                null,
                null,
                null,
                null,
                null
        );


        return "movie-list";
    }



    // ==========================================
    // 영화 상세페이지
    // ==========================================

    @GetMapping("/movies/{movieId}")
    public String movieDetail(

            @PathVariable("movieId")
            Long movieId,

            Model model) {


        MovieDetailDto movie =
                movieService.getMovieDetail(
                        movieId
                );


        model.addAttribute(
                "movie",
                movie
        );


        return "movie-detail";
    }



    // ==========================================
    // 영화 목록 공통 Model 처리
    // ==========================================

    private void addMovieListModel(

            Model model,

            Map<String, Object> response,

            int page,

            String pageTitle,

            String listType,

            String query,

            Integer genre,

            Double minRating,

            Integer year,

            Integer provider) {


        // ==========================================
        // 영화 목록 변환
        // ==========================================

        List<MovieDto> movies =
                movieService.convertToMovieList(
                        response
                );


        // ==========================================
        // 전체 페이지
        // ==========================================

        int totalPages =
                getNumber(
                        response,
                        "total_pages",
                        1
                );


        if (totalPages > 500) {
            totalPages = 500;
        }


        // ==========================================
        // 페이지 번호 범위
        // ==========================================

        int startPage =
                Math.max(
                        1,
                        page - 2
                );


        int endPage =
                Math.min(
                        totalPages,
                        startPage + 4
                );


        if (
            endPage - startPage < 4
        ) {

            startPage =
                    Math.max(
                            1,
                            endPage - 4
                    );
        }


        // ==========================================
        // Model
        // ==========================================

        model.addAttribute(
                "movies",
                movies
        );


        model.addAttribute(
                "query",
                query
        );


        model.addAttribute(
                "selectedGenre",
                genre
        );


        model.addAttribute(
                "selectedMinRating",
                minRating
        );


        model.addAttribute(
                "selectedYear",
                year
        );


        model.addAttribute(
                "selectedProvider",
                provider
        );


        model.addAttribute(
                "currentPage",
                page
        );


        model.addAttribute(
                "totalPages",
                totalPages
        );


        model.addAttribute(
                "startPage",
                startPage
        );


        model.addAttribute(
                "endPage",
                endPage
        );


        model.addAttribute(
                "pageTitle",
                pageTitle
        );


        model.addAttribute(
                "listType",
                listType
        );
    }



    // ==========================================
    // Map 숫자 읽기
    // ==========================================

    private int getNumber(

            Map<String, Object> response,

            String key,

            int defaultValue) {


        if (response == null) {

            return defaultValue;
        }


        Object value =
                response.get(
                        key
                );


        if (value instanceof Number) {

            return ((Number) value)
                    .intValue();
        }


        return defaultValue;
    }

}
