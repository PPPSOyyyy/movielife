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
    // 인기 / 검색 / 장르 / 페이지네이션
    // ==========================================
    @GetMapping({"/movies", "/movie-list"})
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
                    value = "page",
                    defaultValue = "1"
            )
            int page,

            Model model) {


        if (page < 1) {
            page = 1;
        }


        Map<String, Object> response;


        // ======================================
        // 1. 검색어가 있으면 검색 우선
        // ======================================

        if (
            query != null &&
            !query.trim().isEmpty()
        ) {


            response =
                    movieService
                            .searchMovies(
                                    query.trim(),
                                    page
                            );


            model.addAttribute(
                    "pageTitle",
                    "'" + query
                    + "' 검색 결과"
            );


        // ======================================
        // 2. 검색어 없고 장르가 있으면 장르
        // ======================================

        } else if (genre != null) {


            response =
                    movieService
                            .getMoviesByGenre(
                                    genre,
                                    page
                            );


            model.addAttribute(
                    "pageTitle",
                    getGenreName(genre)
                    + " 영화"
            );


        // ======================================
        // 3. 아무것도 없으면 인기영화
        // ======================================

        } else {


            response =
                    movieService
                            .getPopularMovies(
                                    page
                            );


            model.addAttribute(
                    "pageTitle",
                    "지금 인기있는 영화"
            );
        }



        List<MovieDto> movies =
                movieService
                        .convertToMovieList(
                                response
                        );



        // ======================================
        // 현재 페이지
        // ======================================

        int currentPage = 1;


        Object responsePage =
                response.get("page");


        if (responsePage instanceof Number) {

            currentPage =
                    ((Number) responsePage)
                            .intValue();
        }



        // ======================================
        // 전체 페이지
        // ======================================

        int totalPages = 1;


        Object totalPagesObject =
                response.get("total_pages");


        if (totalPagesObject instanceof Number) {

            totalPages =
                    ((Number) totalPagesObject)
                            .intValue();
        }


        /*
         * TMDB API는 페이지를 최대 500까지
         * 접근 가능하도록 제한되는 경우가 있어서
         * 화면에서도 최대 500으로 제한합니다.
         */
        totalPages =
                Math.min(
                        totalPages,
                        500
                );



        // ======================================
        // 화면에 페이지 번호 5개 표시
        // ======================================

        int startPage =
                Math.max(
                        1,
                        currentPage - 2
                );


        int endPage =
                Math.min(
                        totalPages,
                        startPage + 4
                );


        startPage =
                Math.max(
                        1,
                        endPage - 4
                );



        model.addAttribute(
                "movies",
                movies
        );


        model.addAttribute(
                "query",
                query == null
                        ? ""
                        : query
        );


        model.addAttribute(
                "selectedGenre",
                genre
        );


        model.addAttribute(
                "currentPage",
                currentPage
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


        return "movie-list";
    }



    // ==========================================
    // 영화 상세페이지
    // ==========================================
    @GetMapping("/movies/{id}")
    public String movieDetail(

            @PathVariable("id")
            Long id,

            Model model) {


        MovieDetailDto movie =
                movieService
                        .getMovieDetail(id);


        model.addAttribute(
                "movie",
                movie
        );


        return "movie-detail";
    }



    // ==========================================
    // 장르 ID → 한글 이름
    // ==========================================
    private String getGenreName(
            int genreId) {


        return switch (genreId) {

            case 28 ->
                "액션";

            case 12 ->
                "모험";

            case 16 ->
                "애니메이션";

            case 35 ->
                "코미디";

            case 80 ->
                "범죄";

            case 99 ->
                "다큐멘터리";

            case 18 ->
                "드라마";

            case 10751 ->
                "가족";

            case 14 ->
                "판타지";

            case 36 ->
                "역사";

            case 27 ->
                "공포";

            case 10402 ->
                "음악";

            case 9648 ->
                "미스터리";

            case 10749 ->
                "로맨스";

            case 878 ->
                "SF";

            case 53 ->
                "스릴러";

            case 10752 ->
                "전쟁";

            case 37 ->
                "서부";

            default ->
                "장르별";
        };
    }

}