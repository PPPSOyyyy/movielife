package com.yse.dev.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

            // 아이디 / 비밀번호 확인
            Member member =
                    memberService.login(loginDto);


            // ★ 로그인 성공한 회원 아이디를 세션에 저장
            session.setAttribute(
                    "loginUserId",
                    member.getUserId()
            );


            // 콘솔 확인용
            System.out.println(
                    "================================"
            );

            System.out.println(
                    "로그인 성공"
            );

            System.out.println(
                    "세션 ID : "
                    + session.getId()
            );

            System.out.println(
                    "로그인 회원 아이디 : "
                    + session.getAttribute(
                            "loginUserId"
                    )
            );

            System.out.println(
                    "================================"
            );


            return ResponseEntity.ok(
                    "로그인에 성공했습니다."
            );


        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }



    // ==========================================
    // 현재 로그인한 회원 정보
    // ==========================================
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(
            HttpSession session) {


        // 세션에서 로그인한 아이디 꺼내기
        String userId =
                (String) session.getAttribute(
                        "loginUserId"
                );


        // 콘솔 확인용
        System.out.println(
                "================================"
        );

        System.out.println(
                "/me 요청"
        );

        System.out.println(
                "현재 세션 ID : "
                + session.getId()
        );

        System.out.println(
                "세션 로그인 아이디 : "
                + userId
        );

        System.out.println(
                "================================"
        );


        // 로그인하지 않은 상태
        if (userId == null) {

            return ResponseEntity
                    .status(401)
                    .body(
                            "로그인이 필요합니다."
                    );
        }


        // DB에서 로그인 회원 정보 찾기
        Member member =
                memberService
                        .getMemberByUserId(
                                userId
                        );


        return ResponseEntity.ok(
                member
        );
    }



    // ==========================================
    // 로그아웃
    // ==========================================
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            HttpSession session) {


        System.out.println(
                "로그아웃 회원 : "
                + session.getAttribute(
                        "loginUserId"
                )
        );


        // 세션 삭제
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


        if (userId == null) {

            return ResponseEntity
                    .status(401)
                    .body(
                            "로그인이 필요합니다."
                    );
        }


        memberService.updateProfile(
                userId,
                profileDto
        );


        return ResponseEntity.ok(
                "프로필 정보가 수정되었습니다."
        );
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


        if (userId == null) {

            return ResponseEntity
                    .status(401)
                    .body(
                            "로그인이 필요합니다."
                    );
        }


        memberService.withdraw(
                userId
        );


        // 탈퇴 후 세션 삭제
        session.invalidate();


        return ResponseEntity.ok(
                "회원 탈퇴 처리가 완료되었습니다."
        );
    }



    // ==========================================
    // 아이디 중복확인
    // ==========================================
    @GetMapping("/check-userid")
    public ResponseEntity<Boolean> checkUserId(

            @RequestParam("userId")
            String userId) {


        boolean duplicate =
                memberService
                        .isUserIdDuplicate(
                                userId
                        );


        return ResponseEntity.ok(
                duplicate
        );
    }



    // ==========================================
    // 닉네임 중복확인
    // ==========================================
    @GetMapping("/check-nickname")
    public ResponseEntity<Boolean> checkNickname(

            @RequestParam("nickname")
            String nickname) {


        boolean duplicate =
                memberService
                        .isNicknameDuplicate(
                                nickname
                        );


        return ResponseEntity.ok(
                duplicate
        );
    }

}