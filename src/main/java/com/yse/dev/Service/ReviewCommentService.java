package com.yse.dev.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yse.dev.Entity.ReviewComment;
import com.yse.dev.Repository.ReviewCommentRepository;
import com.yse.dev.Repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewCommentService {

    private static final int MAX_CONTENT_LENGTH = 500;

    private final ReviewCommentRepository reviewCommentRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public List<ReviewComment> getComments(Long reviewId) {
        validateReviewExists(reviewId);
        return reviewCommentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId);
    }

    @Transactional
    public ReviewComment createComment(Long reviewId, String userId, String content) {
        validateReviewExists(reviewId);
        validateContent(content);

        ReviewComment comment = new ReviewComment();
        comment.setReviewId(reviewId);
        comment.setUserId(userId);
        comment.setContent(content.trim());

        return reviewCommentRepository.save(comment);
    }

    @Transactional
    public ReviewComment updateComment(Long commentId, String userId, String content) {
        validateContent(content);

        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인이 작성한 댓글만 수정할 수 있습니다.");
        }

        comment.setContent(content.trim());
        return reviewCommentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, String userId) {
        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인이 작성한 댓글만 삭제할 수 있습니다.");
        }

        reviewCommentRepository.delete(comment);
    }

    private void validateReviewExists(Long reviewId) {
        if (reviewId == null || !reviewRepository.existsById(reviewId)) {
            throw new IllegalArgumentException("리뷰를 찾을 수 없습니다.");
        }
    }

    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("댓글 내용을 입력해 주세요.");
        }
        if (content.trim().length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("댓글은 500자 이하로 입력해 주세요.");
        }
    }
}
