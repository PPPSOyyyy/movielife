package com.yse.dev.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.yse.dev.DTO.MovieDetailDto;
import com.yse.dev.DTO.MovieDto;


@Service
public class MovieService {


    @Value("${tmdb.api.key}")
    private String apiKey;


    @Value("${tmdb.api.base-url}")
    private String baseUrl;


    private final RestTemplate restTemplate;


    public MovieService() {
        this.restTemplate = new RestTemplate();
    }



    // ==========================================
    // 인기 영화 조회
    // ==========================================
    public Map<String, Object> getPopularMovies(
            int page) {


        URI uri =
                UriComponentsBuilder
                        .fromUriString(
                                baseUrl
                                + "/movie/popular"
                        )
                        .queryParam(
                                "api_key",
                                apiKey
                        )
                        .queryParam(
                                "language",
                                "ko-KR"
                        )
                        .queryParam(
                                "page",
                                page
                        )
                        .build()
                        .encode()
                        .toUri();


        return restTemplate.getForObject(
                uri,
                Map.class
        );
    }



    // ==========================================
    // 영화 제목 검색
    // ==========================================
    public Map<String, Object> searchMovies(
            String query,
            int page) {


        URI uri =
                UriComponentsBuilder
                        .fromUriString(
                                baseUrl
                                + "/search/movie"
                        )
                        .queryParam(
                                "api_key",
                                apiKey
                        )
                        .queryParam(
                                "language",
                                "ko-KR"
                        )
                        .queryParam(
                                "query",
                                query
                        )
                        .queryParam(
                                "page",
                                page
                        )
                        .queryParam(
                                "include_adult",
                                false
                        )
                        .build()
                        .encode()
                        .toUri();


        return restTemplate.getForObject(
                uri,
                Map.class
        );
    }



    // ==========================================
    // 장르별 영화 조회
    // ==========================================
    public Map<String, Object> getMoviesByGenre(
            int genreId,
            int page) {


        URI uri =
                UriComponentsBuilder
                        .fromUriString(
                                baseUrl
                                + "/discover/movie"
                        )
                        .queryParam(
                                "api_key",
                                apiKey
                        )
                        .queryParam(
                                "language",
                                "ko-KR"
                        )
                        .queryParam(
                                "with_genres",
                                genreId
                        )
                        .queryParam(
                                "sort_by",
                                "popularity.desc"
                        )
                        .queryParam(
                                "include_adult",
                                false
                        )
                        .queryParam(
                                "page",
                                page
                        )
                        .build()
                        .encode()
                        .toUri();


        return restTemplate.getForObject(
                uri,
                Map.class
        );
    }



    // ==========================================
    // TMDB 응답 → MovieDto 목록
    // ==========================================
    public List<MovieDto> convertToMovieList(
            Map<String, Object> response) {


        List<MovieDto> movieList =
                new ArrayList<>();


        if (response == null) {
            return movieList;
        }


        Object resultObject =
                response.get("results");


        if (!(resultObject instanceof List<?>)) {
            return movieList;
        }


        List<?> results =
                (List<?>) resultObject;


        for (Object object : results) {


            if (!(object instanceof Map<?, ?>)) {
                continue;
            }


            Map<?, ?> movie =
                    (Map<?, ?>) object;


            MovieDto movieDto =
                    new MovieDto();


            // ID
            Object id =
                    movie.get("id");

            if (id instanceof Number) {

                movieDto.setId(
                        ((Number) id)
                                .longValue()
                );
            }


            // 제목
            Object title =
                    movie.get("title");

            if (title != null) {

                movieDto.setTitle(
                        title.toString()
                );
            }


            // 줄거리
            Object overview =
                    movie.get("overview");

            if (overview != null) {

                movieDto.setOverview(
                        overview.toString()
                );
            }


            // 포스터
            Object posterPath =
                    movie.get("poster_path");

            if (posterPath != null) {

                movieDto.setPosterPath(
                        posterPath.toString()
                );
            }


            // 개봉일
            Object releaseDate =
                    movie.get("release_date");

            if (releaseDate != null) {

                movieDto.setReleaseDate(
                        releaseDate.toString()
                );
            }


            // 평점
            Object voteAverage =
                    movie.get("vote_average");

            if (voteAverage instanceof Number) {

                movieDto.setVoteAverage(
                        ((Number) voteAverage)
                                .doubleValue()
                );
            }


            // 장르 ID 목록
            List<Integer> genreIds =
                    new ArrayList<>();


            Object genreObject =
                    movie.get("genre_ids");


            if (genreObject instanceof List<?>) {


                List<?> genres =
                        (List<?>) genreObject;


                for (Object genre : genres) {


                    if (genre instanceof Number) {

                        genreIds.add(
                                ((Number) genre)
                                        .intValue()
                        );
                    }
                }
            }


            movieDto.setGenreIds(
                    genreIds
            );


            movieList.add(
                    movieDto
            );
        }


        return movieList;
    }



    // ==========================================
    // 영화 상세정보 조회
    // ==========================================
    public MovieDetailDto getMovieDetail(
            Long movieId) {


        MovieDetailDto detail =
                new MovieDetailDto();


        // ------------------------------------------
        // 영화 기본정보
        // ------------------------------------------

        URI detailUri =
                UriComponentsBuilder
                        .fromUriString(
                                baseUrl
                                + "/movie/"
                                + movieId
                        )
                        .queryParam(
                                "api_key",
                                apiKey
                        )
                        .queryParam(
                                "language",
                                "ko-KR"
                        )
                        .build()
                        .encode()
                        .toUri();


        Map<String, Object> movie =
                restTemplate.getForObject(
                        detailUri,
                        Map.class
                );


        if (movie == null) {
            return detail;
        }


        detail.setId(movieId);


        Object title =
                movie.get("title");

        if (title != null) {
            detail.setTitle(title.toString());
        }


        Object originalTitle =
                movie.get("original_title");

        if (originalTitle != null) {
            detail.setOriginalTitle(
                    originalTitle.toString()
            );
        }


        Object overview =
                movie.get("overview");

        if (overview != null) {
            detail.setOverview(
                    overview.toString()
            );
        }


        Object posterPath =
                movie.get("poster_path");

        if (posterPath != null) {
            detail.setPosterPath(
                    posterPath.toString()
            );
        }


        Object backdropPath =
                movie.get("backdrop_path");

        if (backdropPath != null) {
            detail.setBackdropPath(
                    backdropPath.toString()
            );
        }


        Object releaseDate =
                movie.get("release_date");

        if (releaseDate != null) {
            detail.setReleaseDate(
                    releaseDate.toString()
            );
        }


        Object voteAverage =
                movie.get("vote_average");

        if (voteAverage instanceof Number) {

            detail.setVoteAverage(
                    ((Number) voteAverage)
                            .doubleValue()
            );
        }


        Object runtime =
                movie.get("runtime");

        if (runtime instanceof Number) {

            detail.setRuntime(
                    ((Number) runtime)
                            .intValue()
            );
        }



        // ------------------------------------------
        // 장르
        // ------------------------------------------

        List<String> genreNames =
                new ArrayList<>();


        Object genresObject =
                movie.get("genres");


        if (genresObject instanceof List<?>) {


            List<?> genres =
                    (List<?>) genresObject;


            for (Object genreObject : genres) {


                if (genreObject instanceof Map<?, ?>) {


                    Map<?, ?> genre =
                            (Map<?, ?>) genreObject;


                    Object genreName =
                            genre.get("name");


                    if (genreName != null) {

                        genreNames.add(
                                genreName.toString()
                        );
                    }
                }
            }
        }


        detail.setGenres(
                genreNames
        );


        // ------------------------------------------
        // 제작 국가
        // ------------------------------------------

        List<String> productionCountries =
                new ArrayList<>();


        Object countryObject =
                movie.get("production_countries");


        if (countryObject instanceof List<?>) {


            List<?> countries =
                    (List<?>) countryObject;


            for (Object countryItem : countries) {


                if (countryItem instanceof Map<?, ?>) {


                    Map<?, ?> country =
                            (Map<?, ?>) countryItem;


                    Object countryName =
                            country.get("name");


                    if (countryName != null) {

                        productionCountries.add(
                                countryName.toString()
                        );
                    }
                }
            }
        }


        detail.setProductionCountries(
                productionCountries
        );



        // ------------------------------------------
        // 출연진 / 감독
        // ------------------------------------------

        URI creditUri =
                UriComponentsBuilder
                        .fromUriString(
                                baseUrl
                                + "/movie/"
                                + movieId
                                + "/credits"
                        )
                        .queryParam(
                                "api_key",
                                apiKey
                        )
                        .queryParam(
                                "language",
                                "ko-KR"
                        )
                        .build()
                        .encode()
                        .toUri();


        Map<String, Object> creditResponse =
                restTemplate.getForObject(
                        creditUri,
                        Map.class
                );


        List<String> castList =
                new ArrayList<>();


        if (creditResponse != null) {


            Object castObject =
                    creditResponse.get("cast");


            if (castObject instanceof List<?>) {


                List<?> casts =
                        (List<?>) castObject;


                int count = 0;


                for (Object castItem : casts) {


                    if (count >= 8) {
                        break;
                    }


                    if (castItem instanceof Map<?, ?>) {


                        Map<?, ?> cast =
                                (Map<?, ?>) castItem;


                        Object castName =
                                cast.get("name");


                        if (castName != null) {

                            castList.add(
                                    castName.toString()
                            );

                            count++;
                        }
                    }
                }
            }
        }


        detail.setCast(
                castList
        );


        // ------------------------------------------
        // 감독
        // ------------------------------------------

        String director = null;


        if (creditResponse != null) {


            Object crewObject =
                    creditResponse.get("crew");


            if (crewObject instanceof List<?>) {


                List<?> crewList =
                        (List<?>) crewObject;


                for (Object crewItem : crewList) {


                    if (crewItem instanceof Map<?, ?>) {


                        Map<?, ?> crew =
                                (Map<?, ?>) crewItem;


                        Object job =
                                crew.get("job");


                        Object name =
                                crew.get("name");


                        if (
                            job != null &&
                            "Director".equals(job.toString()) &&
                            name != null
                        ) {

                            director =
                                    name.toString();

                            break;
                        }
                    }
                }
            }
        }


        detail.setDirector(
                director
        );

        // ------------------------------------------
        // 대한민국 관람등급
        // ------------------------------------------

        URI releaseUri =
                UriComponentsBuilder
                        .fromUriString(
                                baseUrl
                                + "/movie/"
                                + movieId
                                + "/release_dates"
                        )
                        .queryParam(
                                "api_key",
                                apiKey
                        )
                        .build()
                        .encode()
                        .toUri();


        Map<String, Object> releaseResponse =
                restTemplate.getForObject(
                        releaseUri,
                        Map.class
                );


        String certification = null;


        if (releaseResponse != null) {


            Object releaseResultsObject =
                    releaseResponse.get(
                            "results"
                    );


            if (
                releaseResultsObject
                        instanceof List<?>
            ) {


                List<?> releaseResults =
                        (List<?>)
                                releaseResultsObject;


                for (
                    Object releaseResultItem
                    : releaseResults
                ) {


                    if (
                        releaseResultItem
                                instanceof Map<?, ?>
                    ) {


                        Map<?, ?> releaseResult =
                                (Map<?, ?>)
                                        releaseResultItem;


                        Object isoCountry =
                                releaseResult.get(
                                        "iso_3166_1"
                                );


                        // 대한민국(KR) 정보 찾기
                        if (
                            isoCountry != null &&
                            "KR".equals(
                                    isoCountry.toString()
                            )
                        ) {


                            Object releaseDatesObject =
                                    releaseResult.get(
                                            "release_dates"
                                    );


                            if (
                                releaseDatesObject
                                        instanceof List<?>
                            ) {


                                List<?> releaseDates =
                                        (List<?>)
                                                releaseDatesObject;


                                for (
                                    Object releaseDateItem
                                    : releaseDates
                                ) {


                                    if (
                                        releaseDateItem
                                                instanceof Map<?, ?>
                                    ) {


                                        Map<?, ?> releaseDateMap =
                                                (Map<?, ?>)
                                                        releaseDateItem;


                                        Object certificationObject =
                                                releaseDateMap.get(
                                                        "certification"
                                                );


                                        if (
                                            certificationObject != null &&
                                            !certificationObject
                                                    .toString()
                                                    .isBlank()
                                        ) {


                                            certification =
                                                    certificationObject
                                                            .toString();


                                            break;

                                        }

                                    }

                                }

                            }


                            // KR 정보는 찾았으므로 종료
                            break;

                        }

                    }

                }

            }

        }


        // ------------------------------------------
        // 관람등급 한글 표시
        // ------------------------------------------

        if (certification == null) {

            detail.setCertification(
                    "정보 없음"
            );

        } else {


            switch (certification) {

                case "ALL":
                    detail.setCertification(
                            "전체 관람가"
                    );
                    break;


                case "12":
                    detail.setCertification(
                            "12세 이상 관람가"
                    );
                    break;


                case "15":
                    detail.setCertification(
                            "15세 이상 관람가"
                    );
                    break;


                case "18":
                case "19":
                    detail.setCertification(
                            "청소년 관람불가"
                    );
                    break;


                default:
                    detail.setCertification(
                            certification
                    );
                    break;

            }

        }


        // ------------------------------------------
        // 한국 OTT
        // ------------------------------------------

        URI providerUri =
                UriComponentsBuilder
                        .fromUriString(
                                baseUrl
                                + "/movie/"
                                + movieId
                                + "/watch/providers"
                        )
                        .queryParam(
                                "api_key",
                                apiKey
                        )
                        .build()
                        .encode()
                        .toUri();


        Map<String, Object> providerResponse =
                restTemplate.getForObject(
                        providerUri,
                        Map.class
                );


        List<String> providers =
                new ArrayList<>();


        if (providerResponse != null) {


            Object resultsObject =
                    providerResponse.get("results");


            if (resultsObject instanceof Map<?, ?>) {


                Map<?, ?> results =
                        (Map<?, ?>) resultsObject;


                Object krObject =
                        results.get("KR");


                if (krObject instanceof Map<?, ?>) {


                    Map<?, ?> kr =
                            (Map<?, ?>) krObject;


                    addProviders(
                            kr.get("flatrate"),
                            providers
                    );


                    addProviders(
                            kr.get("rent"),
                            providers
                    );


                    addProviders(
                            kr.get("buy"),
                            providers
                    );
                }
            }
        }


        detail.setOttProviders(
                providers
        );


        return detail;
    }



    // ==========================================
    // OTT 제공처 이름 중복 없이 추가
    // ==========================================
    private void addProviders(
            Object providerObject,
            List<String> providers) {


        if (!(providerObject instanceof List<?>)) {
            return;
        }


        List<?> providerList =
                (List<?>) providerObject;


        for (Object item : providerList) {


            if (item instanceof Map<?, ?>) {


                Map<?, ?> provider =
                        (Map<?, ?>) item;


                Object providerName =
                        provider.get(
                                "provider_name"
                        );


                if (providerName != null) {


                    String name =
                            providerName.toString();


                    if (!providers.contains(name)) {

                        providers.add(name);
                    }
                }
            }
        }
    }

}