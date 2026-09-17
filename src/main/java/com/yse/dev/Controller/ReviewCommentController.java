package com.yse.dev.Controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yse.dev.Entity.Member;
import com.yse.dev.Entity.ReviewComment;
import com.yse.dev.Service.MemberService;
import com.yse.dev.Service.ReviewCommentService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/review-comments")
@RequiredArgsConstructor
public class ReviewCommentController {

    private final ReviewCommentService reviewCommentService;
    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<?> getComments(@RequestParam("reviewId") Long reviewId) {
        try {
            List<ReviewComment> comments = reviewCommentService.getComments(reviewId);
            List<Map<String, Object>> result = new ArrayList<>();

            for (ReviewComment comment : comments) {
                result.add(toResult(comment));
            }

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createComment(
            @RequestParam("reviewId") Long reviewId,
            @RequestParam("content") String content,
            HttpSession session) {

        String userId = (String) session.getAttribute("loginUserId");
        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        try {
            ReviewComment comment = reviewCommentService.createComment(reviewId, userId, content);
            return ResponseEntity.ok(toResult(comment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable("commentId") Long commentId,
            @RequestParam("content") String content,
            HttpSession session) {

        String userId = (String) session.getAttribute("loginUserId");
        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        try {
            ReviewComment comment = reviewCommentService.updateComment(commentId, userId, content);
            return ResponseEntity.ok(toResult(comment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable("commentId") Long commentId,
            HttpSession session) {

        String userId = (String) session.getAttribute("loginUserId");
        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        try {
            reviewCommentService.deleteComment(commentId, userId);
            return ResponseEntity.ok("댓글이 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private Map<String, Object> toResult(ReviewComment comment) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", comment.getId());
        result.put("reviewId", comment.getReviewId());
        result.put("userId", comment.getUserId());
        result.put("content", comment.getContent());
        result.put("createdAt", comment.getCreatedAt());
        result.put("updatedAt", comment.getUpdatedAt());

        try {
            Member member = memberService.getMemberByUserId(comment.getUserId());
            result.put("nickname", member.getNickname());
        } catch (Exception e) {
            result.put("nickname", comment.getUserId());
        }

        return result;
    }
}
