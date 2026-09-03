package com.yse.dev.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yse.dev.Entity.Favorite;
import com.yse.dev.Service.FavoriteService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    // 찜 추가
    @PostMapping
    public ResponseEntity<?> addFavorite(
            @RequestParam("movieId") Long movieId,
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

            Favorite favorite =
                    favoriteService.addFavorite(
                            userId,
                            movieId
                    );

            return ResponseEntity.ok(favorite);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // 찜 취소
    @DeleteMapping("/{movieId}")
    public ResponseEntity<?> deleteFavorite(
            @PathVariable("movieId") Long movieId,
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

            favoriteService.deleteFavorite(
                    userId,
                    movieId
            );

            return ResponseEntity.ok(
                    "찜이 취소되었습니다."
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // 내가 찜한 영화 목록
    @GetMapping("/my")
    public ResponseEntity<?> getMyFavorites(
            HttpSession session) {

        String userId =
                (String) session.getAttribute("loginUserId");

        // 로그인 확인
        if (userId == null) {
            return ResponseEntity
                    .status(401)
                    .body("로그인이 필요합니다.");
        }

        List<Favorite> favorites =
                favoriteService.getMyFavorites(userId);

        return ResponseEntity.ok(favorites);
    }


    // 특정 영화 찜 여부 확인
    @GetMapping("/check")
    public ResponseEntity<?> checkFavorite(
            @RequestParam("movieId") Long movieId,
            HttpSession session) {

        String userId =
                (String) session.getAttribute("loginUserId");

        // 로그인하지 않은 경우
        if (userId == null) {
            return ResponseEntity.ok(false);
        }

        boolean favorite =
                favoriteService.isFavorite(
                        userId,
                        movieId
                );

        return ResponseEntity.ok(favorite);
    }
}