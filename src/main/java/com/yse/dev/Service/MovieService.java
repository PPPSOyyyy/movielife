package com.yse.dev.Service;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.yse.dev.DTO.MovieDetailDto;
import com.yse.dev.DTO.MovieDetailDto.CastMember;
import com.yse.dev.DTO.MovieDto;


@Service
public class MovieService {


    @Value("${tmdb.api.key}")
    private String apiKey;


    @Value("${tmdb.api.base-url}")
    private String baseUrl;


    private final RestTemplate restTemplate;


    private record CachedResponse(
            long expiresAt,
            Map<String, Object> value
    ) {
    }


    private final Map<URI, CachedResponse> cache =
            new LinkedHashMap<>(
                    256,
                    .75f,
                    true
            );


    // =========================================================
    // 생성자
    // =========================================================

    public MovieService() {


        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();


        factory.setConnectTimeout(
                4000
        );


        factory.setReadTimeout(
                7000
        );


        restTemplate =
                new RestTemplate(
                        factory
                );
    }


    // =========================================================
    // TMDB 기본 주소
    // =========================================================

    private UriComponentsBuilder endpoint(
            String path) {


        return UriComponentsBuilder
                .fromUriString(
                        baseUrl + path
                )
                .queryParam(
                        "api_key",
                        apiKey
                )
                .queryParam(
                        "language",
                        "ko-KR"
                );
    }


    // =========================================================
    // 페이지 번호 보정
    // =========================================================

    private int page(
            int value) {


        return Math.max(
                1,
                Math.min(
                        500,
                        value
                )
        );
    }


    // =========================================================
    // TMDB 요청
    // =========================================================

    @SuppressWarnings("unchecked")
    private Map<String, Object> request(
            UriComponentsBuilder builder) {


        URI uri =
                builder
                        .build()
                        .encode()
                        .toUri();


        synchronized (cache) {


            CachedResponse cached =
                    cache.get(uri);


            if (
                cached != null &&
                cached.expiresAt()
                        > System.currentTimeMillis()
            ) {

                return cached.value();
            }
        }


        Map<String, Object> value =
                restTemplate.getForObject(
                        uri,
                        Map.class
                );


        if (
            value == null
        ) {

            throw new ResourceAccessException(
                    "TMDB 응답이 비어 있습니다."
            );
        }


        synchronized (cache) {


            cache.put(
                    uri,
                    new CachedResponse(
                            System.currentTimeMillis()
                                    + 300_000,
                            value
                    )
            );


            while (
                cache.size() > 200
            ) {

                cache.remove(
                        cache
                                .keySet()
                                .iterator()
                                .next()
                );
            }
        }


        return value;
    }


    // =========================================================
    // 인기 영화
    // =========================================================

    public Map<String, Object> getPopularMovies(
            int page) {


        return request(
                endpoint(
                        "/movie/popular"
                )
                .queryParam(
                        "page",
                        page(page)
                )
        );
    }


    // =========================================================
    // 영화 검색
    // =========================================================

    public Map<String, Object> searchMovies(
            String query,
            int page) {


        return request(
                endpoint(
                        "/search/movie"
                )
                .queryParam(
                        "query",
                        query
                )
                .queryParam(
                        "include_adult",
                        false
                )
                .queryParam(
                        "page",
                        page(page)
                )
        );
    }


    // =========================================================
    // 장르별 영화
    // =========================================================

    public Map<String, Object> getMoviesByGenre(
            int genreId,
            int page) {


        return getFilteredMovies(
                genreId,
                null,
                null,
                null,
                page
        );
    }


    // =========================================================
    // OTT 영화
    // =========================================================

    public Map<String, Object> getOttMovies(
            Integer provider,
            int page) {


        UriComponentsBuilder builder =
                endpoint(
                        "/discover/movie"
                )
                .queryParam(
                        "watch_region",
                        "KR"
                )
                .queryParam(
                        "with_watch_monetization_types",
                        "flatrate"
                )
                .queryParam(
                        "include_adult",
                        false
                )
                .queryParam(
                        "sort_by",
                        "popularity.desc"
                )
                .queryParam(
                        "page",
                        page(page)
                );


        if (
            provider != null
        ) {

            builder.queryParam(
                    "with_watch_providers",
                    provider
            );
        }


        return request(
                builder
        );
    }


    // =========================================================
    // 복합 필터
    // =========================================================

    public Map<String, Object> getFilteredMovies(
            Integer genre,
            Double minRating,
            Integer year,
            Integer provider,
            int page) {


        UriComponentsBuilder builder =
                endpoint(
                        "/discover/movie"
                )
                .queryParam(
                        "include_adult",
                        false
                )
                .queryParam(
                        "sort_by",
                        "popularity.desc"
                )
                .queryParam(
                        "page",
                        page(page)
                );


        if (
            genre != null
        ) {

            builder.queryParam(
                    "with_genres",
                    genre
            );
        }


        if (
            minRating != null &&
            minRating > 0
        ) {

            builder
                    .queryParam(
                            "vote_average.gte",
                            minRating
                    )
                    .queryParam(
                            "vote_count.gte",
                            50
                    );
        }


        if (
            year != null
        ) {

            builder.queryParam(
                    "primary_release_year",
                    year
            );
        }


        if (
            provider != null
        ) {

            builder
                    .queryParam(
                            "watch_region",
                            "KR"
                    )
                    .queryParam(
                            "with_watch_providers",
                            provider
                    )
                    .queryParam(
                            "with_watch_monetization_types",
                            "flatrate"
                    );
        }


        return request(
                builder
        );
    }


    // =========================================================
    // 평점 높은 영화
    // =========================================================

    public Map<String, Object> getTopRatedMovies(
            int page) {


        return request(
                endpoint(
                        "/movie/top_rated"
                )
                .queryParam(
                        "region",
                        "KR"
                )
                .queryParam(
                        "page",
                        page(page)
                )
        );
    }


    // =========================================================
    // 개봉 예정 영화
    // =========================================================

    public Map<String, Object> getUpcomingMovies(
            int page) {


        LocalDate today =
                LocalDate.now(
                        ZoneId.of(
                                "Asia/Seoul"
                        )
                );


        return request(
                endpoint(
                        "/discover/movie"
                )
                .queryParam(
                        "region",
                        "KR"
                )
                .queryParam(
                        "with_release_type",
                        "3|2"
                )
                .queryParam(
                        "include_adult",
                        false
                )
                .queryParam(
                        "release_date.gte",
                        today.plusDays(1)
                )
                .queryParam(
                        "release_date.lte",
                        today.plusMonths(6)
                )
                .queryParam(
                        "sort_by",
                        "popularity.desc"
                )
                .queryParam(
                        "page",
                        page(page)
                )
        );
    }


    // =========================================================
    // 국내 / 해외 영화
    // =========================================================

    public Map<String, Object> getCountryMovies(
            String country,
            Integer genre,
            Double minRating,
            Integer year,
            Integer provider,
            int page) {


        if (
            country == null ||
            !country.matches(
                    "[A-Z]{2}"
            )
        ) {

            throw new IllegalArgumentException(
                    "국가 정보가 올바르지 않습니다."
            );
        }


        UriComponentsBuilder builder =
                endpoint(
                        "/discover/movie"
                )
                .queryParam(
                        "with_origin_country",
                        country
                )
                .queryParam(
                        "include_adult",
                        false
                )
                .queryParam(
                        "include_video",
                        false
                )
                .queryParam(
                        "sort_by",
                        "popularity.desc"
                )
                .queryParam(
                        "page",
                        page(page)
                );


        if (
            genre != null
        ) {

            builder.queryParam(
                    "with_genres",
                    genre
            );
        }


        if (
            minRating != null &&
            minRating > 0
        ) {

            builder
                    .queryParam(
                            "vote_average.gte",
                            minRating
                    )
                    .queryParam(
                            "vote_count.gte",
                            50
                    );
        }


        if (
            year != null
        ) {

            builder.queryParam(
                    "primary_release_year",
                    year
            );
        }


        if (
            provider != null
        ) {

            builder
                    .queryParam(
                            "watch_region",
                            "KR"
                    )
                    .queryParam(
                            "with_watch_providers",
                            provider
                    )
                    .queryParam(
                            "with_watch_monetization_types",
                            "flatrate"
                    );
        }


        return request(
                builder
        );
    }


    // =========================================================
    // 목록 Map → MovieDto
    // =========================================================

    public List<MovieDto> convertToMovieList(
            Map<String, Object> response) {


        List<MovieDto> result =
                new ArrayList<>();


        if (
            response == null
        ) {

            return result;
        }


        for (
            Map<?, ?> movie :
            objects(
                    response.get(
                            "results"
                    )
            )
        ) {


            if (
                !(movie.get("id")
                        instanceof Number id)
            ) {

                continue;
            }


            result.add(
                    movieDtoFromMap(
                            movie,
                            id.longValue()
                    )
            );
        }


        return result;
    }


    // =========================================================
    // 공통 MovieDto 변환
    // =========================================================

    private MovieDto movieDtoFromMap(
            Map<?, ?> movie,
            Long id) {


        MovieDto dto =
                new MovieDto();


        dto.setId(
                id
        );


        dto.setTitle(
                text(
                        movie,
                        "title",
                        "제목 정보 없음"
                )
        );


        dto.setOverview(
                text(
                        movie,
                        "overview",
                        ""
                )
        );


        dto.setPosterPath(
                text(
                        movie,
                        "poster_path",
                        ""
                )
        );


        dto.setBackdropPath(
                text(
                        movie,
                        "backdrop_path",
                        ""
                )
        );


        dto.setReleaseDate(
                text(
                        movie,
                        "release_date",
                        ""
                )
        );


        dto.setVoteAverage(
                number(
                        movie,
                        "vote_average"
                )
                .doubleValue()
        );


        List<Integer> genreIds =
                new ArrayList<>();


        if (
            movie.get("genre_ids")
                    instanceof List<?> ids
        ) {


            for (
                Object genreId :
                ids
            ) {


                if (
                    genreId instanceof Number n
                ) {

                    genreIds.add(
                            n.intValue()
                    );
                }
            }
        }


        dto.setGenreIds(
                genreIds
        );


        dto.setOttProviders(
                new ArrayList<>()
        );


        return dto;
    }


    // =========================================================
    // 영화 상세
    // =========================================================

    public MovieDetailDto getMovieDetail(
            Long movieId) {


        if (
            movieId == null ||
            movieId <= 0
        ) {

            throw new IllegalArgumentException(
                    "영화 정보가 올바르지 않습니다."
            );
        }


        // =====================================================
        // 영화 기본정보
        // =====================================================

        Map<String, Object> movie =
                request(
                        endpoint(
                                "/movie/"
                                + movieId
                        )
                );


        // =====================================================
        // 출연진 / 감독
        // =====================================================

        Map<String, Object> credits =
                request(
                        endpoint(
                                "/movie/"
                                + movieId
                                + "/credits"
                        )
                );


        // =====================================================
        // 개봉 정보 / 관람등급
        // =====================================================

        Map<String, Object> releaseDates =
                request(
                        endpoint(
                                "/movie/"
                                + movieId
                                + "/release_dates"
                        )
                );


        // =====================================================
        // OTT
        // =====================================================

        Map<String, Object> watchProviders =
                request(
                        endpoint(
                                "/movie/"
                                + movieId
                                + "/watch/providers"
                        )
                );


        // =====================================================
        // 예고편
        // =====================================================

        Map<String, Object> videos =
                request(
                        endpoint(
                                "/movie/"
                                + movieId
                                + "/videos"
                        )
                );


        MovieDetailDto dto =
                new MovieDetailDto();


        dto.setId(
                movieId
        );


        dto.setTitle(
                text(
                        movie,
                        "title",
                        "제목 정보 없음"
                )
        );


        dto.setOriginalTitle(
                text(
                        movie,
                        "original_title",
                        ""
                )
        );


        dto.setOverview(
                text(
                        movie,
                        "overview",
                        ""
                )
        );


        dto.setPosterPath(
                text(
                        movie,
                        "poster_path",
                        ""
                )
        );


        dto.setBackdropPath(
                text(
                        movie,
                        "backdrop_path",
                        ""
                )
        );


        dto.setReleaseDate(
                text(
                        movie,
                        "release_date",
                        ""
                )
        );


        dto.setVoteAverage(
                number(
                        movie,
                        "vote_average"
                )
                .doubleValue()
        );


        dto.setRuntime(
                number(
                        movie,
                        "runtime"
                )
                .intValue()
        );


        dto.setGenres(
                names(
                        movie.get(
                                "genres"
                        )
                )
        );


        // =====================================================
        // 제작 국가
        // =====================================================

        List<String> countries =
                new ArrayList<>();


        for (
            Map<?, ?> country :
            objects(
                    movie.get(
                            "production_countries"
                    )
            )
        ) {


            String code =
                    text(
                            country,
                            "iso_3166_1",
                            ""
                    );


            if (
                code.isBlank()
            ) {

                countries.add(
                        text(
                                country,
                                "name",
                                "정보 없음"
                        )
                );

            } else {

                countries.add(
                        new Locale(
                                "",
                                code
                        )
                        .getDisplayCountry(
                                Locale.KOREAN
                        )
                );
            }
        }


        dto.setProductionCountries(
                countries
        );


        // =====================================================
        // 출연진
        // =====================================================

        List<CastMember> castMembers =
                new ArrayList<>();


        for (
            Map<?, ?> cast :
            objects(
                    credits.get(
                            "cast"
                    )
            )
        ) {


            if (
                castMembers.size()
                        >= 8
            ) {

                break;
            }


            Long personId =
                    null;


            if (
                cast.get("id")
                        instanceof Number n
            ) {

                personId =
                        n.longValue();
            }


            castMembers.add(
                    new CastMember(
                            personId,
                            text(
                                    cast,
                                    "name",
                                    "이름 정보 없음"
                            ),
                            text(
                                    cast,
                                    "character",
                                    ""
                            ),
                            text(
                                    cast,
                                    "profile_path",
                                    ""
                            )
                    )
            );
        }


        dto.setCastMembers(
                castMembers
        );


        dto.setCast(
                castMembers
                        .stream()
                        .map(
                                CastMember::getName
                        )
                        .toList()
        );


        // =====================================================
        // 감독
        // =====================================================

        dto.setDirector(
                objects(
                        credits.get(
                                "crew"
                        )
                )
                .stream()
                .filter(
                        crew ->
                                "Director".equals(
                                        crew.get(
                                                "job"
                                        )
                                )
                )
                .map(
                        crew ->
                                text(
                                        crew,
                                        "name",
                                        ""
                                )
                )
                .findFirst()
                .orElse(
                        "정보 없음"
                )
        );


        // =====================================================
        // 관람등급
        // =====================================================

        String certification =
                "정보 없음";


        for (
            Map<?, ?> release :
            objects(
                    releaseDates.get(
                            "results"
                    )
            )
        ) {


            if (
                !"KR".equals(
                        release.get(
                                "iso_3166_1"
                        )
                )
            ) {

                continue;
            }


            for (
                Map<?, ?> date :
                objects(
                        release.get(
                                "release_dates"
                        )
                )
            ) {


                String value =
                        text(
                                date,
                                "certification",
                                ""
                        );


                if (
                    value.isBlank()
                ) {

                    continue;
                }


                certification =
                        switch (
                                value
                        ) {

                            case "ALL",
                                 "0" ->
                                    "전체 관람가";

                            case "12" ->
                                    "12세 이상 관람가";

                            case "15" ->
                                    "15세 이상 관람가";

                            case "18",
                                 "19" ->
                                    "청소년 관람불가";

                            default ->
                                    value;
                        };


                break;
            }
        }


        dto.setCertification(
                certification
        );


        // =====================================================
        // OTT
        // =====================================================

        dto.setOttProviders(
                providerNames(
                        watchProviders
                )
        );


        // =====================================================
        // 예고편
        // =====================================================

        dto.setTrailerKey(
                findTrailerKey(
                        videos
                )
        );


        return dto;
    }


    // =========================================================
    // 예고편 찾기
    // =========================================================

    private String findTrailerKey(
            Object videosValue) {


        Map<?, ?> videosMap =
                map(
                        videosValue
                );


        List<Map<?, ?>> videos =
                objects(
                        videosMap.get(
                                "results"
                        )
                );


        String fallback =
                "";


        for (
            Map<?, ?> video :
            videos
        ) {


            String site =
                    text(
                            video,
                            "site",
                            ""
                    );


            String type =
                    text(
                            video,
                            "type",
                            ""
                    );


            String key =
                    text(
                            video,
                            "key",
                            ""
                    );


            if (
                !"YouTube".equalsIgnoreCase(
                        site
                ) ||
                !"Trailer".equalsIgnoreCase(
                        type
                ) ||
                key.isBlank()
            ) {

                continue;
            }


            if (
                Boolean.TRUE.equals(
                        video.get(
                                "official"
                        )
                )
            ) {

                return key;
            }


            if (
                fallback.isBlank()
            ) {

                fallback =
                        key;
            }
        }


        return fallback;
    }


    // =========================================================
    // 영화 OTT 조회
    // =========================================================

    public List<String> getOttProviders(
            Long movieId) {


        return providerNames(
                request(
                        endpoint(
                                "/movie/"
                                + movieId
                                + "/watch/providers"
                        )
                )
        );
    }


    // =========================================================
    // OTT 제공처 ID 찾기
    // =========================================================

    public Integer getMovieProviderId(
            String providerName) {


        for (
            Map<?, ?> provider :
            objects(
                    request(
                            endpoint(
                                    "/watch/providers/movie"
                            )
                            .queryParam(
                                    "watch_region",
                                    "KR"
                            )
                    )
                    .get(
                            "results"
                    )
            )
        ) {


            if (
                providerName.equalsIgnoreCase(
                        text(
                                provider,
                                "provider_name",
                                ""
                        )
                )
                &&
                provider.get(
                        "provider_id"
                )
                instanceof Number id
            ) {

                return id.intValue();
            }
        }


        return null;
    }


    // =========================================================
    // OTT 이름 목록
    // =========================================================

    private List<String> providerNames(
            Object value) {


        Map<?, ?> results =
                map(
                        map(
                                value
                        )
                        .get(
                                "results"
                        )
                );


        Map<?, ?> kr =
                map(
                        results.get(
                                "KR"
                        )
                );


        List<String> result =
                new ArrayList<>();


        for (
            String kind :
            List.of(
                    "flatrate",
                    "rent",
                    "buy"
            )
        ) {


            for (
                Map<?, ?> provider :
                objects(
                        kr.get(
                                kind
                        )
                )
            ) {


                String name =
                        text(
                                provider,
                                "provider_name",
                                ""
                        );


                if (
                    !name.isBlank() &&
                    !result.contains(name)
                ) {

                    result.add(
                            name
                    );
                }
            }
        }


        return result;
    }


    // =========================================================
    // Map 안전 변환
    // =========================================================

    private Map<?, ?> map(
            Object value) {


        return value
                instanceof Map<?, ?> m
                ? m
                : Map.of();
    }


    // =========================================================
    // List<Map> 안전 변환
    // =========================================================

    private List<Map<?, ?>> objects(
            Object value) {


        List<Map<?, ?>> result =
                new ArrayList<>();


        if (
            value instanceof List<?> list
        ) {


            for (
                Object item :
                list
            ) {


                if (
                    item instanceof Map<?, ?> m
                ) {

                    result.add(
                            m
                    );
                }
            }
        }


        return result;
    }


    // =========================================================
    // name 목록 추출
    // =========================================================

    private List<String> names(
            Object value) {


        return objects(
                value
        )
        .stream()
        .map(
                item ->
                        text(
                                item,
                                "name",
                                ""
                        )
        )
        .filter(
                name ->
                        !name.isBlank()
        )
        .toList();
    }


    // =========================================================
    // String 값 안전 추출
    // =========================================================

    private String text(
            Map<?, ?> map,
            String key,
            String fallback) {


        Object value =
                map.get(
                        key
                );


        return (
                value == null ||
                value.toString()
                        .isBlank()
        )
                ? fallback
                : value.toString();
    }


    // =========================================================
    // Number 값 안전 추출
    // =========================================================

    private Number number(
            Map<?, ?> map,
            String key) {


        return map.get(
                key
        )
        instanceof Number n
                ? n
                : 0;
    }
}