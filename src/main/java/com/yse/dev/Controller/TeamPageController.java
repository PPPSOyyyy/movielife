package com.yse.dev.Controller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;

import com.yse.dev.DTO.MovieDto;
import com.yse.dev.Service.MovieService;

@Controller
public class TeamPageController {

    private static final Map<String, String> COUNTRIES = countries();

    private final MovieService movieService;


    public TeamPageController(MovieService movieService) {

        this.movieService = movieService;
    }


    // =========================================================
    // 국내영화
    // =========================================================

    @GetMapping("/movies/domestic")
    public String domesticMovies(

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


        return countryMovies(

                false,

                "KR",

                genre,

                minRating,

                year,

                provider,

                page,

                model
        );
    }


    // =========================================================
    // 해외영화
    // =========================================================

    @GetMapping("/movies/foreign")
    public String foreignMovies(

            @RequestParam(
                    value = "country",
                    defaultValue = "US"
            )
            String country,

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


        if (country == null) {

            country = "US";
        }


        country = country
                .trim()
                .toUpperCase(Locale.ROOT);


        if (!COUNTRIES.containsKey(country)) {

            country = "US";
        }


        return countryMovies(

                true,

                country,

                genre,

                minRating,

                year,

                provider,

                page,

                model
        );
    }


    // =========================================================
    // 국내 / 해외 공통 처리
    // =========================================================

    private String countryMovies(

            boolean foreignPage,

            String country,

            Integer genre,

            Double minRating,

            Integer year,

            Integer provider,

            int requestedPage,

            Model model) {


        int page = Math.max(

                1,

                Math.min(
                        requestedPage,
                        500
                )
        );


        String countryName;

        if (foreignPage) {

            countryName = COUNTRIES.get(country);

        } else {

            countryName = "한국";
        }


        String catalogError = null;

        Map<String, Object> response = Map.of();


        try {

            response = movieService.getCountryMovies(

                    country,

                    genre,

                    minRating,

                    year,

                    provider,

                    page
            );


            int lastPage = Math.max(

                    1,

                    Math.min(
                            500,
                            count(
                                    response,
                                    "total_pages"
                            )
                    )
            );


            if (page > lastPage) {

                page = lastPage;


                response = movieService.getCountryMovies(

                        country,

                        genre,

                        minRating,

                        year,

                        provider,

                        page
                );
            }

        } catch (RestClientException e) {

            catalogError =
                    "영화 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";

            response = Map.of();

            page = 1;
        }


        List<MovieDto> movies =
                movieService.convertToMovieList(
                        response
                );


        int totalPages = Math.max(

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


        if (endPage - startPage < 4) {

            startPage =
                    Math.max(
                            1,
                            endPage - 4
                    );
        }


        // =====================================================
        // 화면 기본 정보
        // =====================================================

        model.addAttribute(
                "foreignPage",
                foreignPage
        );


        model.addAttribute(
                "pageTitle",
                foreignPage
                        ? "해외영화"
                        : "국내영화"
        );


        model.addAttribute(
                "pageDescription",
                foreignPage
                        ? "세계 여러 나라의 다양한 영화를 만나보세요."
                        : "한국 영화의 다양한 이야기를 만나보세요."
        );


        model.addAttribute(
                "eyebrow",
                foreignPage
                        ? "WORLD CINEMA"
                        : "KOREAN CINEMA"
        );


        model.addAttribute(
                "basePath",
                foreignPage
                        ? "/movies/foreign"
                        : "/movies/domestic"
        );


        model.addAttribute(
                "countryName",
                countryName
        );


        model.addAttribute(
                "selectedCountry",
                country
        );


        model.addAttribute(
                "countries",
                COUNTRIES
        );


        // =====================================================
        // 필터
        // =====================================================

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
                "currentYear",
                LocalDate.now().getYear()
        );


        // =====================================================
        // 영화 목록
        // =====================================================

        model.addAttribute(
                "movies",
                movies
        );


        model.addAttribute(
                "catalogError",
                catalogError
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


        return "country-movies";
    }


    // =========================================================
    // TMDB 결과 숫자 안전하게 가져오기
    // =========================================================

    private int count(

            Map<String, Object> response,

            String key) {


        if (response == null) {

            return 0;
        }


        Object value =
                response.get(key);


        if (value instanceof Number number) {

            return Math.max(
                    0,
                    number.intValue()
            );
        }


        return 0;
    }


    // =========================================================
    // 해외영화 국가 목록
    // =========================================================

    private static Map<String, String> countries() {

        Map<String, String> countries =
                new LinkedHashMap<>();


        countries.put(
                "US",
                "미국"
        );


        countries.put(
                "JP",
                "일본"
        );


        countries.put(
                "GB",
                "영국"
        );


        countries.put(
                "FR",
                "프랑스"
        );


        countries.put(
                "DE",
                "독일"
        );


        countries.put(
                "CA",
                "캐나다"
        );


        countries.put(
                "IN",
                "인도"
        );


        countries.put(
                "ES",
                "스페인"
        );


        countries.put(
                "IT",
                "이탈리아"
        );


        countries.put(
                "AU",
                "호주"
        );


        countries.put(
                "CN",
                "중국"
        );


        countries.put(
                "HK",
                "홍콩"
        );


        countries.put(
                "TW",
                "대만"
        );


        return Collections.unmodifiableMap(
                countries
        );
    }
}