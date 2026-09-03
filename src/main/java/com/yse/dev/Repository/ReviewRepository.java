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

}