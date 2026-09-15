package com.yse.dev.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;

import com.yse.dev.DTO.MovieDetailDto;
import com.yse.dev.DTO.MovieDto;
import com.yse.dev.Service.MovieService;


@Controller
public class MovieController {


    private final MovieService movieService;


    public MovieController(
            MovieService movieService) {

        this.movieService =
                movieService;
    }


    @GetMapping({
            "/movies",
            "/movie-list"
    })
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
                    value = "tab",
                    required = false,
                    defaultValue = "all"
            )
            String tab,

            @RequestParam(
                    value = "page",
                    defaultValue = "1"
            )
            int requestedPage,

            Model model) {


        int page =
                Math.max(
                        1,
                        Math.min(
                                requestedPage,
                                500
                        )
                );


        String normalizedQuery =
                query == null
                        ? ""
                        : query.trim();


        String activeTab =
                normalizeTab(
                        tab
                );


        Map<String, Object> response =
                Map.of();


        String catalogError =
                null;


        try {


            if (
                !normalizedQuery.isBlank()
            ) {

                response =
                        movieService.searchMovies(
                                normalizedQuery,
                                page
                        );

                activeTab =
                        "all";

            } else if (
                "topRated".equals(
                        activeTab
                )
            ) {

                response =
                        movieService.getTopRatedMovies(
                                page
                        );

            } else if (
                "upcoming".equals(
                        activeTab
                )
            ) {

                response =
                        movieService.getUpcomingMovies(
                                page
                        );

            } else if (
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

            } else {

                response =
                        movieService.getPopularMovies(
                                page
                        );
            }


        } catch (
            RestClientException e
        ) {

            catalogError =
                    "영화 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";

            response =
                    Map.of();

            page =
                    1;
        }


        List<MovieDto> movies =
                movieService.convertToMovieList(
                        response
                );


        int totalPages =
                Math.max(
                        1,
                        Math.min(
                                500,
                                count(
                                        response,
                                        "total_pages"
                                )
                        )
                );


        int totalResults =
                count(
                        response,
                        "total_results"
                );


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


        String pageTitle;


        if (
            !normalizedQuery.isBlank()
        ) {

            pageTitle =
                    "'"
                    + normalizedQuery
                    + "' 검색 결과";

        } else if (
            "topRated".equals(
                    activeTab
            )
        ) {

            pageTitle =
                    "평점 높은 영화";

        } else if (
            "upcoming".equals(
                    activeTab
            )
        ) {

            pageTitle =
                    "개봉 예정 영화";

        } else {

            pageTitle =
                    "영화 둘러보기";
        }


        model.addAttribute(
                "movies",
                movies
        );


        model.addAttribute(
                "query",
                normalizedQuery
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
                "activeTab",
                activeTab
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
                "totalResults",
                totalResults
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
                "catalogError",
                catalogError
        );


        model.addAttribute(
                "currentYear",
                LocalDate.now().getYear()
        );


        return "movie-list";
    }


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


    private String normalizeTab(
            String tab) {


        if (
            "topRated".equals(
                    tab
            )
        ) {

            return "topRated";
        }


        if (
            "upcoming".equals(
                    tab
            )
        ) {

            return "upcoming";
        }


        return "all";
    }


    private int count(

            Map<String, Object> response,

            String key) {


        if (
            response == null
        ) {

            return 0;
        }


        Object value =
                response.get(
                        key
                );


        if (
            value instanceof Number number
        ) {

            return Math.max(
                    0,
                    number.intValue()
            );
        }


        return 0;
    }
}