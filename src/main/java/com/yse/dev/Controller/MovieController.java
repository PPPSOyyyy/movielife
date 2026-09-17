package com.yse.dev.Controller;
import com.yse.dev.DTO.CatalogFilter;
import com.yse.dev.Service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
@Controller @RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;
    private final CatalogModel catalog;
    private final MovieViewService movieViewService;
    @GetMapping({"/movies","/movie-list"})
    public String movieList(@ModelAttribute CatalogFilter filter,Model model){
        // 영화 목록·인물 검색·배우 출연작 병합은 CatalogModel 이 한 번의 병렬 요청으로 처리합니다.
        catalog.populate(filter,null,model);
        model.addAttribute("pageTitle",!filter.getQuery().isBlank()?"‘"+filter.getQuery()+"’ 검색 결과":"topRated".equals(filter.getTab())?"평점 높은 영화":"upcoming".equals(filter.getTab())?"개봉 예정 영화":"영화 둘러보기");
        return "movie-list";
    }
    @GetMapping("/movies/{movieId}")
    public String movieDetail(
            @PathVariable(name = "movieId") Long movieId,
            Model model,
            HttpSession session) {

        String userId = (String) session.getAttribute("loginUserId");

        // 로그인한 회원의 상세 조회를 추천 학습용으로 기록합니다.
        // 같은 영화를 반복 새로고침해도 하루 1회만 조회 횟수가 증가합니다.
        try {
            movieViewService.recordView(userId, movieId);
        } catch (Exception ignored) {
            // 조회 기록 실패가 영화 상세 화면 자체를 막지 않게 합니다.
        }

        model.addAttribute("movie", movieService.getMovieDetail(movieId));
        return "movie-detail";
    }
}
