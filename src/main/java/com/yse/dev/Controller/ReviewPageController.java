package com.yse.dev.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.yse.dev.DTO.MovieDetailDto;
import com.yse.dev.Entity.Review;
import com.yse.dev.Service.MovieService;
import com.yse.dev.Service.ReviewService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ReviewPageController {

    private final MovieService movieService;
    private final ReviewService reviewService;

    @GetMapping("/review")
    public String review(
            @RequestParam(
                    value = "movieId",
                    required = false
            ) Long movieId,
            @RequestParam(
                    value = "reviewId",
                    required = false
            ) Long reviewId,
            HttpSession session,
            Model model) {

        String userId =
                (String) session.getAttribute(
                        "loginUserId"
                );

        // 로그인하지 않은 경우
        if (userId == null) {

            String returnUrl = "/review";

            if (reviewId != null) {
                returnUrl += "?reviewId=" + reviewId;
            } else if (movieId != null) {
                returnUrl += "?movieId=" + movieId;
            }

            return "redirect:/login?returnUrl="
                    + java.net.URLEncoder.encode(
                            returnUrl,
                            java.nio.charset.StandardCharsets.UTF_8
                    );
        }

        // ==========================================
        // 리뷰 수정 모드
        // ==========================================
        if (reviewId != null) {

            try {

                Review review =
                        reviewService.getReviewForEdit(
                                reviewId,
                                userId
                        );

                MovieDetailDto movie =
                        movieService.getMovieDetail(
                                review.getMovieId()
                        );

                model.addAttribute(
                        "movieId",
                        review.getMovieId()
                );

                model.addAttribute(
                        "movie",
                        movie
                );

                model.addAttribute(
                        "review",
                        review
                );

                model.addAttribute(
                        "editMode",
                        true
                );

                return "review";

            } catch (IllegalArgumentException e) {

                return "redirect:/my-reviews";
            }
        }

        // ==========================================
        // 새 리뷰 작성 모드
        // ==========================================
        if (movieId == null) {
            return "redirect:/movies?reviewRequired=true";
        }

        MovieDetailDto movie =
                movieService.getMovieDetail(
                        movieId
                );

        model.addAttribute(
                "movieId",
                movieId
        );

        model.addAttribute(
                "movie",
                movie
        );

        model.addAttribute(
                "editMode",
                false
        );

        return "review";
    }
}
