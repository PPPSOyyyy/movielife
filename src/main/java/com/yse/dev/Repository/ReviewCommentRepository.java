package com.yse.dev.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yse.dev.Entity.ReviewComment;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {

    List<ReviewComment> findByReviewIdOrderByCreatedAtAsc(Long reviewId);

    long countByReviewId(Long reviewId);

    void deleteByReviewId(Long reviewId);

    void deleteByUserId(String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ReviewComment c where c.reviewId in (select r.id from Review r where r.userId = :userId)")
    int deleteCommentsOnReviewsByUserId(@Param("userId") String userId);
}
