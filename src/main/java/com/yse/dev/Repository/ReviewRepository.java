package com.yse.dev.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yse.dev.Entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByMovieId(Long movieId);
    List<Review> findByUserId(String userId);

}