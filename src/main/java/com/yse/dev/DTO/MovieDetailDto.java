package com.yse.dev.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDetailDto {


    private Long id;

    private String title;

    private String originalTitle;

    private String overview;

    private String posterPath;

    private String backdropPath;

    private String releaseDate;

    private Double voteAverage;

    private Integer runtime;

    private List<String> genres;

    private List<String> productionCountries;

    private String director;

    private String certification;

    private List<String> cast;

    private List<CastMember> castMembers;

    private List<String> ottProviders;

    private String trailerKey;


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


    public String getBackdropUrl() {

        if (
            backdropPath == null ||
            backdropPath.isBlank()
        ) {

            return "";
        }


        return "https://image.tmdb.org/t/p/w1280"
                + backdropPath;
    }


    public String getTrailerUrl() {

        if (
            trailerKey == null ||
            trailerKey.isBlank()
        ) {

            return "";
        }


        return "https://www.youtube.com/watch?v="
                + trailerKey;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CastMember {


        private Long id;

        private String name;

        private String character;

        private String profilePath;


        public String getProfileUrl() {

            if (
                profilePath == null ||
                profilePath.isBlank()
            ) {

                return "/poster/no-poster.svg";
            }


            return "https://image.tmdb.org/t/p/w185"
                    + profilePath;
        }
    }
}