package com.yse.dev.DTO;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonDetailDto {


    // =========================================================
    // 배우 기본 정보
    // =========================================================

    private Long id;

    private String name;

    private String originalName;

    private String profilePath;

    private String biography;

    private String birthday;

    private String deathday;

    private String placeOfBirth;

    private String knownForDepartment;

    private Double popularity;


    // =========================================================
    // 대표 출연작
    // =========================================================

    private List<PersonMovieDto> movies =
            new ArrayList<>();


    // =========================================================
    // 배우 프로필 이미지
    // =========================================================

    public String getProfileUrl() {

        if (
            profilePath == null ||
            profilePath.isBlank()
        ) {

            return "/poster/no-poster.svg";
        }


        return "https://image.tmdb.org/t/p/w500"
                + profilePath;
    }


    // =========================================================
    // 분야 한글
    // =========================================================

    public String getDepartmentName() {

        if (
            knownForDepartment == null ||
            knownForDepartment.isBlank()
        ) {

            return "정보 없음";
        }


        return switch (
                knownForDepartment
        ) {

            case "Acting" ->
                    "배우";

            case "Directing" ->
                    "감독";

            case "Writing" ->
                    "각본";

            case "Production" ->
                    "제작";

            case "Camera" ->
                    "촬영";

            case "Editing" ->
                    "편집";

            case "Sound" ->
                    "음향";

            default ->
                    knownForDepartment;
        };
    }


    // =========================================================
    // 출연작 DTO
    // =========================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonMovieDto {


        private Long id;

        private String title;

        private String posterPath;

        private String releaseDate;

        private Double voteAverage;

        private Integer voteCount;

        private Double popularity;

        private String character;


        public String getPosterUrl() {

            if (
                posterPath == null ||
                posterPath.isBlank()
            ) {

                return "/poster/no-poster.svg";
            }


            return "https://image.tmdb.org/t/p/w500"
                    + posterPath;
        }


        public String getReleaseYear() {

            if (
                releaseDate == null ||
                releaseDate.length() < 4
            ) {

                return "연도 미정";
            }


            return releaseDate.substring(
                    0,
                    4
            );
        }


        public String getCharacterName() {

            if (
                character == null ||
                character.isBlank()
            ) {

                return "배역 정보 없음";
            }


            return character;
        }
    }
}