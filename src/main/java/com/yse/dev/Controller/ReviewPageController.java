package com.yse.dev.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.yse.dev.DTO.MovieDetailDto;
import com.yse.dev.Service.MovieService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ReviewPageController {

    private final MovieService movieService;

    @GetMapping("/review")
    public String review(
            @RequestParam(
                    value = "movieId",
                    required = false
            ) Long movieId,
            HttpSession session,
            Model model) {

        String userId = (String) session.getAttribute("loginUserId");

        // 로그인하지 않은 경우
        if (userId == null) {
            return "redirect:/login?required=true";
        }

        // 선택한 영화가 없으면 영화 목록에서 먼저 선택
        if (movieId == null) {
            return "redirect:/movies?reviewRequired=true";
        }

        MovieDetailDto movie =
                movieService.getMovieDetail(movieId);

        // 저장에는 영화 ID를 사용하고 화면에는 영화 제목을 표시합니다.
        model.addAttribute("movieId", movieId);
        model.addAttribute("movie", movie);

        return "review";
    }
}
