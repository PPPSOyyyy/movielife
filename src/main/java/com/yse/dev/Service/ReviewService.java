package com.yse.dev.Service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.yse.dev.Entity.Review;
import com.yse.dev.Repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public Review createReview(
            String userId,
            Long movieId,
            Integer rating,
            String content) {

        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
                    "별점은 1점부터 5점까지 입력해주세요.");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "리뷰 내용을 입력해주세요.");
        }

        Review review = new Review();

        review.setUserId(userId);
        review.setMovieId(movieId);
        review.setRating(rating);
        review.setContent(content);

        return reviewRepository.save(review);
        
        
    }
 // 리뷰 수정
    public Review updateReview(
            Long reviewId,
            String userId,
            Integer rating,
            String content) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        // 본인이 작성한 리뷰인지 확인
        if (!review.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                    "본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        // 별점 검사
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
                    "별점은 1점부터 5점까지 입력해주세요.");
        }

        // 내용 검사
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "리뷰 내용을 입력해주세요.");
        }

        review.setRating(rating);
        review.setContent(content);

        return reviewRepository.save(review);
    }
    
 // 리뷰 삭제
    public void deleteReview(Long reviewId, String userId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        // 본인이 작성한 리뷰인지 확인
        if (!review.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                    "본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        reviewRepository.delete(review);
    }
    
    public List<Review> getReviewsByMovieId(Long movieId) {
        return reviewRepository.findByMovieId(movieId);
    }
 // 로그인한 사용자의 리뷰 조회
    public List<Review> getMyReviews(String userId) {
        return reviewRepository.findByUserId(userId);
    }
}
