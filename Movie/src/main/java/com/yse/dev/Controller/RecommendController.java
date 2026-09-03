package com.yse.dev.Controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.yse.dev.DTO.MemberDto;
import com.yse.dev.DTO.RecommendDto;
import com.yse.dev.Service.AiRecommendService;
import com.yse.dev.Service.RecommendService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/movie-recommend")
public class RecommendController {

    private final RecommendService recommendService;
    private final AiRecommendService aiRecommendService;

    public RecommendController(RecommendService recommendService, AiRecommendService aiRecommendService) {
        this.recommendService = recommendService;
        this.aiRecommendService = aiRecommendService;
    }

    // 1. 트렌드 목록
    @GetMapping("")
    public String recommendMain(Model model) {
        List<RecommendDto> movies = recommendService.getTrendingRecommendations();
        model.addAttribute("movieList", movies);
        model.addAttribute("currentGenre", 0);
        return "movie-recommend";
    }

    // 2. 장르별 추천
    @GetMapping("/genre")
    public String recommendByGenre(@RequestParam("id") int genreId, Model model) {
        List<RecommendDto> movies = recommendService.getGenreRecommendations(genreId);
        model.addAttribute("movieList", movies);
        model.addAttribute("currentGenre", genreId);
        return "movie-recommend";
    }

    // 3. 영화 상세 페이지
    @GetMapping("/detail")
    public String movieDetail(@RequestParam("id") Long movieId, Model model) {
        RecommendDto movie = recommendService.getMovieDetails(movieId);
        model.addAttribute("movie", movie);
        return "movie-detail";
    }

    // 4. 로그인 유저 맞춤 제미나이 AI 추천
    @GetMapping("/ai-recommend")
    public String aiCustomRecommend(HttpSession session, Model model) {
        // 세션에서 로그인한 유저 정보 가져오기
        MemberDto loginUser = (MemberDto) session.getAttribute("loginUser");

        // 로그인이 되어있지 않다면 로그인 페이지로 강제 이동
        if (loginUser == null) {
            return "redirect:/member/login";
        }

        // 로그인된 유저의 이름을 활용해 제미나이에게 성향 분석 및 영화 추천 요청
        String aiResponseText = aiRecommendService.getAiRecommendedMovieTitles(loginUser.getNickname());

        // 쉼표(,) 기준 분리
        String[] movieTitles = aiResponseText.split(",");

        // TMDB 실데이터 검색 매핑
        List<RecommendDto> aiRecommendedMovieList = new ArrayList<>();
        for (String title : movieTitles) {
            RecommendDto movie = recommendService.searchMovieByTitle(title.trim());
            if (movie != null) {
                aiRecommendedMovieList.add(movie);
            }
        }

        model.addAttribute("movieList", aiRecommendedMovieList);
        model.addAttribute("currentGenre", 0);

        return "movie-recommend";
    }
    @GetMapping("/ai")
    public String aiRecommendTab(HttpSession session, Model model) {
        // 1. 로그인 유저 확인
        MemberDto loginUser = (MemberDto) session.getAttribute("loginUser");
        if (loginUser == null) {
            // 로그인이 안 되어 있다면 로그인 페이지로 보냅니다.
            return "redirect:/member/login";
        }

        // 2. 제미나이가 유저 닉네임 기반으로 성향 분석 후 추천 영화 제목들 반환
        String aiResponseText = aiRecommendService.getAiRecommendedMovieTitles(loginUser.getNickname());
        String[] movieTitles = aiResponseText.split(",");

        // 3. TMDB 실데이터 검색 매핑
        List<RecommendDto> aiRecommendedMovieList = new ArrayList<>();
        for (String title : movieTitles) {
            RecommendDto movie = recommendService.searchMovieByTitle(title.trim());
            if (movie != null) {
                aiRecommendedMovieList.add(movie);
            }
        }

        // 4. 화면에 전달
        model.addAttribute("movieList", aiRecommendedMovieList);
        model.addAttribute("currentGenre", -1);

        return "movie-recommend";
    }
}