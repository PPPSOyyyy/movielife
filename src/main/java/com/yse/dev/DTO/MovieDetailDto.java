package com.yse.dev.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDetailDto {
    private java.util.List<CastMember> directors = new java.util.ArrayList<>();
    private String certificationCode = "UNKNOWN";
    private boolean certificationChecked = true;
    public boolean isAdultsOnly(){return "19".equals(certificationCode);}

    // TMDB adult 플래그
    private boolean adult;

    // 카드 프래그먼트(fragments/layout :: card)와 호환되도록 회원 별점 필드를 둡니다.
    private Double memberRatingAverage;
    private Long memberRatingCount = 0L;

    // OTT 제공처 + 이동 링크 (이름 목록인 ottProviders 와 같은 순서)
    private java.util.List<OttProvider> ottLinks = new java.util.ArrayList<>();


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OttProvider {

        // TMDB provider_name (예: Netflix, Watcha)
        private String name;

        // TMDB logo_path
        private String logoPath;

        // 클릭 시 이동할 주소 (서비스 내 작품 검색 또는 TMDB 시청 안내 페이지)
        private String url;

        // 구독 / 무료 / 광고 / 대여 / 구매
        private String type;


        public String getLogoUrl() {

            if (logoPath == null || logoPath.isBlank()) {
                return "";
            }

            return "https://image.tmdb.org/t/p/w92" + logoPath;
        }
    }



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