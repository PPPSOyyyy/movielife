package com.yse.dev.Controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.yse.dev.Service.RecommendService;

import jakarta.servlet.http.HttpSession;

@RestController
public class RecommendationController {

    private final RecommendService recommendService;

    public RecommendationController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping("/api/recommendations")
    public ResponseEntity<?> recommend(HttpSession session) {
        try {
            String userId = (String) session.getAttribute("loginUserId");
            return ResponseEntity.ok(recommendService.autoRecommend(userId));
        } catch (RestClientException e) {
            return ResponseEntity.status(503).body(Map.of(
                    "message",
                    "추천 영화를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "message",
                    "추천 처리 중 오류가 발생했습니다."
            ));
        }
    }
}
