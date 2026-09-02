package com.yse.dev.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDto {

    // TMDB 영화 고유 번호
    private Long id;

    // 영화 제목
    private String title;

    // 줄거리
    private String overview;

    // 포스터 경로
    private String posterPath;

    // 개봉일
    private String releaseDate;

    // 평점
    private Double voteAverage;

    // 장르 번호 목록
    private List<Integer> genreIds;


    // ==========================================
    // 실제 포스터 이미지 주소
    // ==========================================
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

}