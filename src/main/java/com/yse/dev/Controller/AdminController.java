package com.yse.dev.Controller;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.yse.dev.Entity.Favorite;
import com.yse.dev.Entity.Member;
import com.yse.dev.Entity.Review;
import com.yse.dev.Repository.FavoriteRepository;
import com.yse.dev.Repository.MemberRepository;
import com.yse.dev.Repository.ReviewRepository;
import com.yse.dev.Service.MovieService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class AdminController {


    // ==========================================
    // 관리자 아이디
    // ==========================================
    private static final String ADMIN_USER_ID = "admin";


    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final MovieService movieService;


    // ==========================================
    // 관리자 페이지
    // ==========================================
    @GetMapping("/admin")
    public String adminPage(
            @RequestParam(value = "view", defaultValue = "dashboard") String view,
            HttpSession session,
            Model model) {


        String loginUserId =
                (String) session.getAttribute("loginUserId");


        // 로그인 안 한 경우
        if (loginUserId == null) {

            return "redirect:/login?returnUrl=/admin";
        }


        // admin이 아닌 경우
        if (!ADMIN_USER_ID.equals(loginUserId)) {

            return "redirect:/?adminDenied=true";
        }


        // ==========================================
        // 통계
        // ==========================================
        long memberCount =
                memberRepository.count();

        long reviewCount =
                reviewRepository.count();

        long favoriteCount =
                favoriteRepository.count();


        // ==========================================
        // 전체 목록
        // ==========================================
        List<Member> members =
                memberRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"id"));

        List<Review> reviews =
                reviewRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"createdAt","id"));

        List<Favorite> favorites =
                favoriteRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"createdAt","id"));


        // ==========================================
        // 관리자 화면에서 사용할 영화 제목 조회
        //
        // Review / Favorite에는 TMDB 영화 ID만 저장되어 있으므로
        // 화면에 숫자 ID 대신 영화명을 보여 주기 위해 제목 맵을 만든다.
        // ==========================================
        Map<Long, String> movieTitles =
                buildMovieTitleMap(
                        reviews,
                        favorites
                );


        // ==========================================
        // 화면 전달
        // ==========================================
        model.addAttribute(
                "memberCount",
                memberCount
        );

        model.addAttribute(
                "reviewCount",
                reviewCount
        );

        model.addAttribute(
                "favoriteCount",
                favoriteCount
        );

        model.addAttribute(
                "members",
                members
        );

        model.addAttribute(
                "reviews",
                reviews
        );

        model.addAttribute(
                "favorites",
                favorites
        );

        model.addAttribute(
                "movieTitles",
                movieTitles
        );


        // 허용된 관리자 화면만 사용
        String adminView = switch (view) {
            case "members", "reviews", "favorites" -> view;
            default -> "dashboard";
        };

        model.addAttribute(
                "adminView",
                adminView
        );

        return "admin";
    }


    // ==========================================
    // 리뷰 / 찜에 들어 있는 영화 ID를 영화명으로 변환
    // ==========================================
    private Map<Long, String> buildMovieTitleMap(
            List<Review> reviews,
            List<Favorite> favorites) {


        Set<Long> movieIds =
                new LinkedHashSet<>();


        for (Review review : reviews) {

            if (
                review != null &&
                review.getMovieId() != null
            ) {

                movieIds.add(
                        review.getMovieId()
                );
            }
        }


        for (Favorite favorite : favorites) {

            if (
                favorite != null &&
                favorite.getMovieId() != null
            ) {

                movieIds.add(
                        favorite.getMovieId()
                );
            }
        }


        Map<Long, String> movieTitles =
                new LinkedHashMap<>();


        for (Long movieId : movieIds) {

            String title;


            try {

                title =
                        movieService.getMovieTitle(
                                movieId
                        );


                if (
                    title == null ||
                    title.isBlank()
                ) {

                    title = "영화 정보 없음";
                }

            } catch (Exception e) {

                // TMDB가 일시적으로 응답하지 않아도
                // 관리자 페이지 전체가 오류 나지 않도록 처리
                title = "영화 정보 없음";
            }


            movieTitles.put(
                    movieId,
                    title
            );
        }


        return movieTitles;
    }


    // ==========================================
    // 회원 강제 삭제
    // ==========================================
    @Transactional
    @PostMapping("/admin/members/{userId}/delete")
    public String deleteMember(
            @PathVariable("userId") String userId,
            HttpSession session) {


        if (!isAdmin(session)) {

            return "redirect:/";
        }


        // 관리자 자기 자신은 삭제 금지
        if (ADMIN_USER_ID.equals(userId)) {

            return "redirect:/admin?view=members&cannotDeleteAdmin=true";
        }


        Member member =
                memberRepository
                        .findByUserId(userId)
                        .orElse(null);


        if (member != null) {

            // 해당 회원의 찜 삭제
            favoriteRepository.deleteByUserId(
                    userId
            );

            // 해당 회원의 리뷰 삭제
            reviewRepository.deleteByUserId(
                    userId
            );

            // 회원 삭제
            memberRepository.delete(
                    member
            );
        }


        return "redirect:/admin?view=members&memberDeleted=true";
    }


    // ==========================================
    // 리뷰 관리자 삭제
    // ==========================================
    @Transactional
    @PostMapping("/admin/reviews/{reviewId}/delete")
    public String deleteReview(
            @PathVariable("reviewId") Long reviewId,
            HttpSession session) {


        if (!isAdmin(session)) {

            return "redirect:/";
        }


        if (reviewRepository.existsById(reviewId)) {

            reviewRepository.deleteById(
                    reviewId
            );
        }


        return "redirect:/admin?view=reviews&reviewDeleted=true";
    }


    // ==========================================
    // 찜 관리자 삭제
    // ==========================================
    @Transactional
    @PostMapping("/admin/favorites/{favoriteId}/delete")
    public String deleteFavorite(
            @PathVariable("favoriteId") Long favoriteId,
            HttpSession session) {


        if (!isAdmin(session)) {

            return "redirect:/";
        }


        if (favoriteRepository.existsById(favoriteId)) {

            favoriteRepository.deleteById(
                    favoriteId
            );
        }


        return "redirect:/admin?view=favorites&favoriteDeleted=true";
    }


    // ==========================================
    // 관리자 확인 공통 메서드
    // ==========================================
    private boolean isAdmin(
            HttpSession session) {


        String loginUserId =
                (String) session.getAttribute("loginUserId");


        return ADMIN_USER_ID.equals(
                loginUserId
        );
    }
}
