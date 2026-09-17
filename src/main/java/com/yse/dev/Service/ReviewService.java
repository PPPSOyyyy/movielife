package com.yse.dev.Service;

import java.util.List;
import com.yse.dev.Repository.MemberRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yse.dev.Entity.Review;
import com.yse.dev.Repository.ReviewRepository;
import com.yse.dev.Repository.ReviewCommentRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class ReviewService {


    private static final int MAX_CONTENT_LENGTH =
            1000;


    private final ReviewRepository
            reviewRepository;

    private final ReviewCommentRepository
            reviewCommentRepository;

    private final MemberRepository memberRepository;



    // ==========================================
    // 리뷰 작성
    // ==========================================

    @Transactional
    public Review createReview(
            String userId,
            Long movieId,
            Integer rating,
            String content) {
        if (movieId == null || movieId <= 0) throw new IllegalArgumentException("영화 정보가 올바르지 않습니다.");
        memberRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다."));
        if (reviewRepository.countByUserIdAndMovieId(userId, movieId) > 0) {
            throw new IllegalArgumentException("이미 작성한 리뷰가 있습니다. 기존 리뷰를 수정해 주세요.");
        }



        validateReview(
                rating,
                content
        );


        Review review =
                new Review();


        review.setUserId(
                userId
        );


        review.setMovieId(
                movieId
        );


        review.setRating(
                rating
        );


        review.setContent(
                content.trim()
        );


        return reviewRepository.save(
                review
        );

    }



    // ==========================================
    // 리뷰 수정
    // ==========================================

    @Transactional
    public Review updateReview(
            Long reviewId,
            String userId,
            Integer rating,
            String content) {


        Review review =
                reviewRepository
                        .findById(
                                reviewId
                        )
                        .orElseThrow(() ->

                                new IllegalArgumentException(
                                        "리뷰를 찾을 수 없습니다."
                                )

                        );


        // 본인 리뷰인지 확인
        if (
            !review.getUserId()
                    .equals(userId)
        ) {

            throw new IllegalArgumentException(
                    "본인이 작성한 리뷰만 수정할 수 있습니다."
            );

        }


        validateReview(
                rating,
                content
        );


        review.setRating(
                rating
        );


        review.setContent(
                content.trim()
        );


        return reviewRepository.save(
                review
        );

    }



    // ==========================================
    // 리뷰 삭제
    // ==========================================

    @Transactional
    public void deleteReview(
            Long reviewId,
            String userId) {


        Review review =
                reviewRepository
                        .findById(
                                reviewId
                        )
                        .orElseThrow(() ->

                                new IllegalArgumentException(
                                        "리뷰를 찾을 수 없습니다."
                                )

                        );


        // 본인 리뷰인지 확인
        if (
            !review.getUserId()
                    .equals(userId)
        ) {

            throw new IllegalArgumentException(
                    "본인이 작성한 리뷰만 삭제할 수 있습니다."
            );

        }


        reviewCommentRepository.deleteByReviewId(
                reviewId
        );

        reviewRepository.delete(
                review
        );

    }



    // ==========================================
    // 영화별 리뷰 조회
    // 최신순
    // ==========================================

    @Transactional(readOnly = true)
    public List<Review> getReviewsByMovieId(
            Long movieId) {


        return reviewRepository
                .findByMovieIdOrderByCreatedAtDesc(
                        movieId
                );

    }



    // ==========================================
    // 내가 작성한 리뷰 조회
    // ==========================================

    @Transactional(readOnly = true)
    public List<Review> getMyReviews(
            String userId) {


        return reviewRepository
                .findByUserIdOrderByCreatedAtDesc(
                        userId
                );

    }



    // ==========================================
    // 리뷰 수정 화면용 단건 조회
    // ==========================================

    @Transactional(readOnly = true)
    public Review getReviewForEdit(
            Long reviewId,
            String userId) {


        Review review =
                reviewRepository
                        .findById(
                                reviewId
                        )
                        .orElseThrow(() ->

                                new IllegalArgumentException(
                                        "리뷰를 찾을 수 없습니다."
                                )

                        );


        if (
            !review.getUserId()
                    .equals(userId)
        ) {

            throw new IllegalArgumentException(
                    "본인이 작성한 리뷰만 수정할 수 있습니다."
            );

        }


        return review;

    }



    // ==========================================
    // MovieLife 평균 별점
    // ==========================================

    @Transactional(readOnly = true)
    public double getAverageRating(
            Long movieId) {


        List<Review> reviews =
                getReviewsByMovieId(
                        movieId
                );


        if (
            reviews.isEmpty()
        ) {

            return 0.0;

        }


        double total = 0;


        for (
            Review review
            : reviews
        ) {

            total +=
                    review.getRating();

        }


        return total
                / reviews.size();

    }



    // ==========================================
    // 평가 참여 인원
    // ==========================================

    @Transactional(readOnly = true)
    public int getReviewCount(
            Long movieId) {


        return getReviewsByMovieId(
                movieId
        ).size();

    }



    // ==========================================
    // 리뷰 공통 검증
    // ==========================================

    private void validateReview(
            Integer rating,
            String content) {


        // 별점 1~10
        if (
            rating == null ||
            rating < 1 ||
            rating > 10
        ) {

            throw new IllegalArgumentException(
                    "별점은 1점부터 10점까지 입력해주세요."
            );

        }


        // 빈 리뷰 또는 공백 리뷰
        if (
            content == null ||
            content.trim().isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "리뷰 내용을 입력해주세요."
            );

        }


        // 1000자 제한
        if (
            content.trim().length()
            > MAX_CONTENT_LENGTH
        ) {

            throw new IllegalArgumentException(
                    "리뷰는 1000자 이하로 작성해주세요."
            );

        }

    }

}