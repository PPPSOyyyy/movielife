package com.yse.dev.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.yse.dev.DTO.LoginDto;
import com.yse.dev.DTO.MemberDto;
import com.yse.dev.DTO.ProfileDto;
import com.yse.dev.Entity.Member;
import com.yse.dev.Service.MemberService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;


    // ==========================================
    // 회원가입
    // ==========================================
    @PostMapping("/signup")
    public ResponseEntity<String> signup(
            @RequestBody MemberDto memberDto) {

        try {

            memberService.signup(memberDto);

            return ResponseEntity.ok(
                    "회원가입이 성공적으로 완료되었습니다."
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // ==========================================
    // 로그인
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<String> login(
            @RequestBody LoginDto loginDto,
            HttpSession session) {

        try {

            // DB에서 아이디와 비밀번호 확인
            Member loginMember =
                    memberService.login(loginDto);


            // 로그인 성공한 회원 아이디를 세션에 저장
            session.setAttribute(
                    "loginUserId",
                    loginMember.getUserId()
            );


            return ResponseEntity.ok(
                    "로그인에 성공했습니다."
            );


        } catch (IllegalArgumentException e) {

            // 아이디 또는 비밀번호가 틀렸을 경우
            return ResponseEntity
                    .status(401)
                    .body(e.getMessage());
        }
    }


    // ==========================================
    // 로그아웃
    // ==========================================
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            HttpSession session) {

        session.invalidate();

        return ResponseEntity.ok(
                "로그아웃 되었습니다."
        );
    }


    // ==========================================
    // 프로필 수정
    // ==========================================
    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(
            @RequestBody ProfileDto profileDto,
            HttpSession session) {

        String userId =
                (String) session.getAttribute(
                        "loginUserId"
                );


        // 로그인 안 되어있으면
        if (userId == null) {

            return ResponseEntity
                    .status(401)
                    .body("로그인이 필요합니다.");
        }


        try {

            memberService.updateProfile(
                    userId,
                    profileDto
            );


            return ResponseEntity.ok(
                    "프로필 정보가 수정되었습니다."
            );


        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // ==========================================
    // 회원 탈퇴
    // ==========================================
    @DeleteMapping("/withdraw")
    public ResponseEntity<String> withdraw(
            HttpSession session) {

        String userId =
                (String) session.getAttribute(
                        "loginUserId"
                );


        // 로그인 안 되어있으면
        if (userId == null) {

            return ResponseEntity
                    .status(401)
                    .body("로그인이 필요합니다.");
        }


        try {

            memberService.withdraw(userId);

            // 탈퇴한 뒤 세션 종료
            session.invalidate();


            return ResponseEntity.ok(
                    "회원 탈퇴 처리가 완료되었습니다."
            );


        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // ==========================================
    // 아이디 중복확인
    // ==========================================
    @GetMapping("/check-userid")
    public ResponseEntity<Boolean> checkUserId(
            @RequestParam("userId") String userId) {

        boolean duplicate =
                memberService
                        .isUserIdDuplicate(userId);


        return ResponseEntity.ok(
                duplicate
        );
    }


    // ==========================================
    // 닉네임 중복확인
    // ==========================================
    @GetMapping("/check-nickname")
    public ResponseEntity<Boolean> checkNickname(
            @RequestParam("nickname") String nickname) {

        boolean duplicate =
                memberService
                        .isNicknameDuplicate(nickname);


        return ResponseEntity.ok(
                duplicate
        );
    }

}