package com.yse.dev.DTO;

/** Existing movie-card fields plus the site's independent 10-point average. */
public class RecommendDto extends MovieDto {
    private final Double siteRating;
    public RecommendDto(MovieDto movie, Double siteRating) {
        setId(movie.getId()); setTitle(movie.getTitle()); setOverview(movie.getOverview());
        setPosterPath(movie.getPosterPath()); setBackdropPath(movie.getBackdropPath());
        setReleaseDate(movie.getReleaseDate()); setVoteAverage(movie.getVoteAverage());
        setVoteCount(movie.getVoteCount()); setPopularity(movie.getPopularity());
        setMemberRatingAverage(movie.getMemberRatingAverage()); setMemberRatingCount(movie.getMemberRatingCount());
        setGenreIds(movie.getGenreIds()); setOttProviders(movie.getOttProviders());
        setAdult(movie.isAdult()); setCertificationCode(movie.getCertificationCode());
        setCertificationChecked(movie.isCertificationChecked());
        this.siteRating = siteRating;
    }
    public Double getSiteRating() { return siteRating; }
}
