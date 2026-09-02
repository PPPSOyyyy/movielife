package com.yse.dev.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDetailDto {

    // 영화 기본 정보
    private Long id;

    private String title;

    private String originalTitle;

    private String overview;

    private String posterPath;

    private String backdropPath;

    private String releaseDate;

    private Double voteAverage;

    private Integer runtime;


    // 장르 이름
    private List<String> genres;


    // 출연진
    private List<String> cast;


    // OTT 제공처
    private List<String> ottProviders;


    // 포스터 전체 주소
    public String getPosterUrl() {

        if (
            posterPath == null ||
            posterPath.isBlank()
        ) {

            return "";
        }


        return "https://image.tmdb.org/t/p/w500"
                + posterPath;
    }


    // 배경 이미지 전체 주소
    public String getBackdropUrl() {

        if (
            backdropPath == null ||
            backdropPath.isBlank()
        ) {

            return "";
        }


        return "https://image.tmdb.org/t/p/original"
                + backdropPath;
    }

}