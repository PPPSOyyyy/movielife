package com.yse.dev.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yse.dev.Entity.Review;


public interface ReviewRepository
        extends JpaRepository<Review, Long> {


    // ==========================================
    // 영화별 리뷰
    // 최신 작성순
    // ==========================================

    List<Review> findByMovieIdOrderByCreatedAtDesc(
            Long movieId
    );


    // ==========================================
    // 내가 작성한 리뷰
    // ==========================================

    List<Review> findByUserId(
            String userId
    );


    // ==========================================
    // 동일 사용자 + 동일 영화 리뷰 개수
    // ==========================================

    long countByUserIdAndMovieId(
            String userId,
            Long movieId
    );

    List<Review> findByUserIdOrderByCreatedAtDesc(String userId);
    void deleteByUserId(String userId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically=true, flushAutomatically=true)
    @org.springframework.data.jpa.repository.Query("update Review r set r.rating = r.rating * 2, r.ratingScale = 10 where r.ratingScale is null and r.rating between 1 and 5")
    int convertLegacyRatingsToTen();

    @org.springframework.data.jpa.repository.Query("select count(r) from Review r where r.ratingScale is null or r.ratingScale <> 10 or r.rating < 1 or r.rating > 10")
    long countInvalidRatingScale();
    // 영화 목록에 표시할 MovieLife 회원 평균 별점 + 참여 수를 한 번에 조회
    @org.springframework.data.jpa.repository.Query(
            "select r.movieId, avg(r.rating), count(r) " +
            "from Review r where r.movieId in :movieIds group by r.movieId")
    List<Object[]> ratingStatsForMovies(
            @org.springframework.data.repository.query.Param("movieIds") List<Long> movieIds);

    // 기존 코드 호환용: 평균 별점만 필요한 곳에서 사용
    @org.springframework.data.jpa.repository.Query(
            "select r.movieId, avg(r.rating) " +
            "from Review r where r.movieId in :movieIds group by r.movieId")
    List<Object[]> averageRatingsForMovies(
            @org.springframework.data.repository.query.Param("movieIds") List<Long> movieIds);
}
