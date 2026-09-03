package com.yse.dev.DTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    // TMDB 장르 ID → 한글 장르명
    // ==========================================

    private static final Map<Integer, String>
            GENRE_MAP = Map.ofEntries(

            Map.entry(28, "액션"),
            Map.entry(12, "모험"),
            Map.entry(16, "애니메이션"),
            Map.entry(35, "코미디"),
            Map.entry(80, "범죄"),
            Map.entry(99, "다큐멘터리"),
            Map.entry(18, "드라마"),
            Map.entry(10751, "가족"),
            Map.entry(14, "판타지"),
            Map.entry(36, "역사"),
            Map.entry(27, "공포"),
            Map.entry(10402, "음악"),
            Map.entry(9648, "미스터리"),
            Map.entry(10749, "로맨스"),
            Map.entry(878, "SF"),
            Map.entry(10770, "TV 영화"),
            Map.entry(53, "스릴러"),
            Map.entry(10752, "전쟁"),
            Map.entry(37, "서부")

    );


    // ==========================================
    // 장르 ID를 한글 장르명 목록으로 변환
    // ==========================================

    public List<String> getGenreNames() {


        List<String> genreNames =
                new ArrayList<>();


        if (
            genreIds == null ||
            genreIds.isEmpty()
        ) {

            return genreNames;

        }


        for (Integer genreId : genreIds) {


            String genreName =
                    GENRE_MAP.get(genreId);


            if (genreName != null) {

                genreNames.add(
                        genreName
                );

            }

        }


        return genreNames;

    }


    // ==========================================
    // 실제 포스터 이미지 주소
    // ==========================================

    public String getPosterUrl() {

        if (
            posterPath == null ||
            posterPath.isBlank()
        ) {

            return "/poster/no-poster.png";
        }

        return "https://image.tmdb.org/t/p/w500"
                + posterPath;
    }

}