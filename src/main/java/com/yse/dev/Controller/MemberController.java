package com.yse.dev.Controller;

import java.util.Map;

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
import com.yse.dev.DTO.WithdrawDto;
import com.yse.dev.Entity.Member;
import com.yse.dev.Service.MemberService;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<?> signup(
            @RequestBody MemberDto memberDto) {

        try {

            memberService.signup(
                    memberDto
            );


            return ResponseEntity.ok(
                    Map.of("message", "회원가입이 성공적으로 완료되었습니다.")
            );


        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of("message", e.getMessage())
                    );

        }

    }



    // ==========================================
    // 로그인
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginDto loginDto,
            HttpSession session, HttpServletRequest request) {

        try {

            Member member =
                    memberService.login(
                            loginDto
                    );


            request.changeSessionId();

            // 로그인 회원 아이디 세션 저장
            session.setAttribute(
                    "loginUserId",
                    member.getUserId()
            );


            return ResponseEntity.ok(
                    Map.of(
                            "message", "",
                            "userId", member.getUserId(),
                            "nickname", member.getNickname()
                    )
            );


        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of("message", e.getMessage())
                    );

        }

    }



    // ==========================================
    // 현재 로그인한 회원 정보
    // ==========================================
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(
            HttpSession session) {


        String userId =
                (String) session.getAttribute(
                        "loginUserId"
                );


        if (userId == null) {

            return ResponseEntity
                    .status(401)
                    .body(
                            Map.of("message", "로그인이 필요합니다.")
                    );

        }


        Member member =
                memberService
                        .getMemberByUserId(
                                userId
                        );


        Map<String,Object> info=new java.util.LinkedHashMap<>();
        info.put("userId",member.getUserId()); info.put("nickname",member.getNickname());
        info.put("name",member.getName()); info.put("email",member.getEmail());
        info.put("securityQuestion",member.getSecurityQuestion());
        info.put("recoveryReady",member.getSecurityAnswerHash()!=null);
        return ResponseEntity.ok(info);
    }



    // ==========================================
    // 로그아웃
    // ==========================================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
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
    public ResponseEntity<?> updateProfile(
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
                            Map.of("message", "로그인이 필요합니다.")
                    );

        }


        try {

            memberService.updateProfile(
                    userId,
                    profileDto
            );


            return ResponseEntity.ok(
                    Map.of("message", "프로필 정보가 수정되었습니다.")
            );


        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of("message", e.getMessage())
                    );

        }

    }



    // ==========================================
    // 회원 탈퇴
    // ==========================================
    @DeleteMapping("/withdraw")
    public ResponseEntity<?> withdraw(

            @RequestBody WithdrawDto withdrawDto,

            HttpSession session) {


        String userId =
                (String) session.getAttribute(
                        "loginUserId"
                );


        // 로그인 확인
        if (userId == null) {

            return ResponseEntity
                    .status(401)
                    .body(
                            Map.of("message", "로그인이 필요합니다.")
                    );

        }


        try {

            /*
             * 현재 로그인 회원의 비밀번호와
             * 사용자가 입력한 비밀번호 확인 후 탈퇴
             */

            memberService.withdraw(

                    userId,

                    withdrawDto.getPassword()

            );


            // 탈퇴 성공 후 세션 삭제
            session.invalidate();


            return ResponseEntity.ok(
                    Map.of("message", "회원 탈퇴 처리가 완료되었습니다.")
            );


        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of("message", e.getMessage())
                    );

        }

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