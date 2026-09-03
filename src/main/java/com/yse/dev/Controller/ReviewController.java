package com.yse.dev.Controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import com.yse.dev.DTO.MovieDetailDto;
import com.yse.dev.Entity.Review;
import com.yse.dev.Service.MovieService;
import com.yse.dev.Service.ReviewService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final MovieService movieService;

    // 리뷰 등록
    @PostMapping
    public ResponseEntity<?> createReview(
            @RequestParam("movieId") Long movieId,
            @RequestParam("rating") Integer rating,
            @RequestParam("content") String content,
            HttpSession session) {

        // 로그인한 사용자 확인
        String userId = (String) session.getAttribute("loginUserId");

        if (userId == null) {
            return ResponseEntity
                    .status(401)
                    .body("로그인이 필요합니다.");
        }

        try {
            Review review = reviewService.createReview(
                    userId,
                    movieId,
                    rating,
                    content
            );

            return ResponseEntity.ok(review);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
    
 // 리뷰 수정
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable("reviewId") Long reviewId,
            @RequestParam("rating") Integer rating,
            @RequestParam("content") String content,
            HttpSession session) {
    	
        String userId =
                (String) session.getAttribute("loginUserId");

        // 로그인 확인
        if (userId == null) {
            return ResponseEntity
                    .status(401)
                    .body("로그인이 필요합니다.");
        }

        try {

            Review review = reviewService.updateReview(
                    reviewId,
                    userId,
                    rating,
                    content
            );

            return ResponseEntity.ok(review);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
    
 // 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable("reviewId") Long reviewId,
            HttpSession session) {

        String userId =
                (String) session.getAttribute("loginUserId");

        // 로그인 확인
        if (userId == null) {
            return ResponseEntity
                    .status(401)
                    .body("로그인이 필요합니다.");
        }

        try {

            reviewService.deleteReview(
                    reviewId,
                    userId
            );

            return ResponseEntity.ok("리뷰가 삭제되었습니다.");

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // 영화별 리뷰 조회
    @GetMapping
    public ResponseEntity<?> getReviews(
            @RequestParam("movieId") Long movieId) {

        List<Review> reviews =
                reviewService.getReviewsByMovieId(movieId);

        return ResponseEntity.ok(reviews);
    }

    // 내가 작성한 리뷰 조회
    @GetMapping("/my")
    public ResponseEntity<?> getMyReviews(HttpSession session) {

        // 로그인한 사용자 확인
        String userId = (String) session.getAttribute("loginUserId");

        if (userId == null) {
            return ResponseEntity
                    .status(401)
                    .body("로그인이 필요합니다.");
        }

        List<Review> reviews =
                reviewService.getMyReviews(userId);

        List<Map<String, Object>> reviewResults =
                new ArrayList<>();

        for (Review review : reviews) {
            Map<String, Object> reviewResult =
                    new LinkedHashMap<>();

            reviewResult.put("id", review.getId());
            reviewResult.put("userId", review.getUserId());
            reviewResult.put("movieId", review.getMovieId());
            reviewResult.put("rating", review.getRating());
            reviewResult.put("content", review.getContent());
            reviewResult.put("createdAt", review.getCreatedAt());
            reviewResult.put("updatedAt", review.getUpdatedAt());

            try {
                MovieDetailDto movie =
                        movieService.getMovieDetail(
                                review.getMovieId()
                        );

                reviewResult.put("movieTitle", movie.getTitle());
                reviewResult.put("posterUrl", movie.getPosterUrl());

            } catch (Exception e) {
                reviewResult.put(
                        "movieTitle",
                        "영화 정보를 불러올 수 없습니다."
                );
                reviewResult.put("posterUrl", null);
            }

            reviewResults.add(reviewResult);
        }

        return ResponseEntity.ok(reviewResults);
    }
}
