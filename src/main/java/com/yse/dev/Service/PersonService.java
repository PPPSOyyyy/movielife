package com.yse.dev.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.yse.dev.DTO.PersonDetailDto;
import com.yse.dev.DTO.PersonDetailDto.PersonMovieDto;


@Service
public class PersonService {


    @Value("${tmdb.api.key}")
    private String apiKey;


    @Value("${tmdb.api.base-url}")
    private String baseUrl;


    private final RestTemplate restTemplate =
            new RestTemplate();


    // =========================================================
    // 배우 상세
    // =========================================================

    public PersonDetailDto getPersonDetail(
            Long personId) {


        if (
            personId == null ||
            personId <= 0
        ) {

            throw new IllegalArgumentException(
                    "배우 정보가 올바르지 않습니다."
            );
        }


        // =====================================================
        // 배우 기본 정보
        // =====================================================

        Map<String, Object> person =
                request(
                        "/person/"
                        + personId
                );


        // =====================================================
        // 배우 영화 출연작
        // =====================================================

        Map<String, Object> credits =
                request(
                        "/person/"
                        + personId
                        + "/movie_credits"
                );


        PersonDetailDto dto =
                new PersonDetailDto();


        dto.setId(
                personId
        );


        dto.setName(
                text(
                        person,
                        "name",
                        "이름 정보 없음"
                )
        );


        dto.setOriginalName(
                text(
                        person,
                        "original_name",
                        ""
                )
        );


        dto.setProfilePath(
                text(
                        person,
                        "profile_path",
                        ""
                )
        );


        dto.setBiography(
                text(
                        person,
                        "biography",
                        ""
                )
        );


        dto.setBirthday(
                text(
                        person,
                        "birthday",
                        ""
                )
        );


        dto.setDeathday(
                text(
                        person,
                        "deathday",
                        ""
                )
        );


        dto.setPlaceOfBirth(
                text(
                        person,
                        "place_of_birth",
                        ""
                )
        );


        dto.setKnownForDepartment(
                text(
                        person,
                        "known_for_department",
                        ""
                )
        );


        dto.setPopularity(
                number(
                        person,
                        "popularity"
                )
                .doubleValue()
        );


        // =====================================================
        // 대표 출연작
        // =====================================================

        List<PersonMovieDto> movies =
                new ArrayList<>();


        Object castValue =
                credits.get(
                        "cast"
                );


        if (
            castValue instanceof List<?> castList
        ) {


            for (
                Object item :
                castList
            ) {


                if (
                    !(item instanceof Map<?, ?> movie)
                ) {

                    continue;
                }


                if (
                    !(movie.get("id")
                            instanceof Number movieId)
                ) {

                    continue;
                }


                String posterPath =
                        text(
                                movie,
                                "poster_path",
                                ""
                        );


                // 포스터 없는 작품 제외
                if (
                    posterPath.isBlank()
                ) {

                    continue;
                }


                PersonMovieDto work =
                        new PersonMovieDto();


                work.setId(
                        movieId.longValue()
                );


                work.setTitle(
                        text(
                                movie,
                                "title",
                                "제목 정보 없음"
                        )
                );


                work.setPosterPath(
                        posterPath
                );


                work.setReleaseDate(
                        text(
                                movie,
                                "release_date",
                                ""
                        )
                );


                work.setVoteAverage(
                        number(
                                movie,
                                "vote_average"
                        )
                        .doubleValue()
                );


                work.setVoteCount(
                        number(
                                movie,
                                "vote_count"
                        )
                        .intValue()
                );


                work.setPopularity(
                        number(
                                movie,
                                "popularity"
                        )
                        .doubleValue()
                );


                work.setCharacter(
                        text(
                                movie,
                                "character",
                                ""
                        )
                );


                movies.add(
                        work
                );
            }
        }


        // =====================================================
        // 대표작 정렬
        //
        // 평가 수가 많은 작품을 우선으로 해서
        // 단편/행사영상/인지도 낮은 작품이 앞에 뜨는 것을 줄임
        // =====================================================

        movies.sort(

                Comparator
                        .comparingInt(
                                (PersonMovieDto movie) ->
                                        movie.getVoteCount() == null
                                                ? 0
                                                : movie.getVoteCount()
                        )
                        .reversed()

                        .thenComparing(
                                Comparator.comparingDouble(
                                        (PersonMovieDto movie) ->
                                                movie.getPopularity() == null
                                                        ? 0.0
                                                        : movie.getPopularity()
                                )
                                .reversed()
                        )

                        .thenComparing(
                                Comparator.comparingDouble(
                                        (PersonMovieDto movie) ->
                                                movie.getVoteAverage() == null
                                                        ? 0.0
                                                        : movie.getVoteAverage()
                                )
                                .reversed()
                        )
        );


        // =====================================================
        // 같은 영화 중복 제거
        // =====================================================

        List<PersonMovieDto> representativeMovies =
                new ArrayList<>();


        Set<Long> usedMovieIds =
                new HashSet<>();


        for (
            PersonMovieDto movie :
            movies
        ) {


            if (
                movie.getId() == null ||
                usedMovieIds.contains(
                        movie.getId()
                )
            ) {

                continue;
            }


            usedMovieIds.add(
                    movie.getId()
            );


            representativeMovies.add(
                    movie
            );


            // 대표작 18개
            if (
                representativeMovies.size()
                        >= 18
            ) {

                break;
            }
        }


        dto.setMovies(
                representativeMovies
        );


        return dto;
    }


    // =========================================================
    // TMDB 요청
    // =========================================================

    @SuppressWarnings("unchecked")
    private Map<String, Object> request(
            String path) {


        String url =
                UriComponentsBuilder
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
                        )
                        .build()
                        .encode()
                        .toUriString();


        Map<String, Object> response =
                restTemplate.getForObject(
                        url,
                        Map.class
                );


        if (
            response == null
        ) {

            throw new IllegalArgumentException(
                    "배우 정보를 불러오지 못했습니다."
            );
        }


        return response;
    }


    // =========================================================
    // 값 안전하게 가져오기
    // =========================================================

    private String text(

            Map<?, ?> map,

            String key,

            String fallback) {


        Object value =
                map.get(
                        key
                );


        if (
            value == null ||
            value.toString().isBlank()
        ) {

            return fallback;
        }


        return value.toString();
    }


    private Number number(

            Map<?, ?> map,

            String key) {


        Object value =
                map.get(
                        key
                );


        if (
            value instanceof Number number
        ) {

            return number;
        }


        return 0;
    }
}