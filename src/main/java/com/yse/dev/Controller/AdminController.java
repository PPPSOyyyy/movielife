package com.yse.dev.Controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.yse.dev.Entity.Favorite;
import com.yse.dev.Entity.Member;
import com.yse.dev.Entity.Review;
import com.yse.dev.Repository.FavoriteRepository;
import com.yse.dev.Repository.MemberRepository;
import com.yse.dev.Repository.ReviewRepository;

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


    // ==========================================
    // 관리자 페이지
    // ==========================================
    @GetMapping("/admin")
    public String adminPage(
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
                memberRepository.findAll();

        List<Review> reviews =
                reviewRepository.findAll();

        List<Favorite> favorites =
                favoriteRepository.findAll();


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


        return "admin";
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

            return "redirect:/admin?cannotDeleteAdmin=true";
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


        return "redirect:/admin?memberDeleted=true";
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


        return "redirect:/admin?reviewDeleted=true";
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


        return "redirect:/admin?favoriteDeleted=true";
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