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
    // 영화 목록
    // 검색 + 장르 + 복합필터
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


        /*
         * 잘못된 페이지값 방지
         */

        if (page < 1) {

            page = 1;

        }


        Map<String, Object> response;


        String pageTitle =
                "지금 인기있는 영화";



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
        // 기본 인기영화
        // ==========================================

        else {


            response =
                    movieService.getPopularMovies(
                            page
                    );

        }



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


        /*
         * TMDB는 최대 페이지가 많을 수 있으므로
         * 화면에서는 최대 500까지만
         */

        if (
            totalPages > 500
        ) {

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
    // Map 숫자 읽기
    // ==========================================

    private int getNumber(
            Map<String, Object> response,
            String key,
            int defaultValue) {


        if (
            response == null
        ) {

            return defaultValue;

        }


        Object value =
                response.get(
                        key
                );


        if (
            value instanceof Number
        ) {

            return ((Number) value)
                    .intValue();

        }


        return defaultValue;

    }

}