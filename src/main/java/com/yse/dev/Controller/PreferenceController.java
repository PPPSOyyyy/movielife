package com.yse.dev.Controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yse.dev.DTO.PreferenceDto;
import com.yse.dev.Service.PreferenceService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ResponseEntity<?> get(HttpSession session) {
        String userId = (String) session.getAttribute("loginUserId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        return ResponseEntity.ok(Map.of(
                "completed", preferenceService.isCompleted(userId),
                "genreIds", preferenceService.getGenreIds(userId)
        ));
    }

    @PutMapping
    public ResponseEntity<?> save(
            @RequestBody PreferenceDto dto,
            HttpSession session) {

        String userId = (String) session.getAttribute("loginUserId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        try {
            return ResponseEntity.ok(Map.of(
                    "message", dto.isSkip() ? "취향 설정을 건너뛰었습니다." : "취향 설정을 저장했습니다.",
                    "genreIds", preferenceService.save(userId, dto)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
