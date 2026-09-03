package com.yse.dev.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.yse.dev.DTO.LoginDto;
import com.yse.dev.DTO.MemberDto;
import com.yse.dev.DTO.ProfileDto;
import com.yse.dev.Service.MemberService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

// [1. 회원 관리] 화면 렌더링 및 폼 데이터 처리 Controller
@Controller
@RequestMapping("/member") // /api/members 보다 웹 경로에 맞게 간결하게 수정
@RequiredArgsConstructor
public class MemberController {
	private final MemberService memberService;
    // ==========================================
    // 1. 화면 보여주기 (GET)
    // ==========================================
	@GetMapping("/")
    public String index() {
        return "index"; // templates 폴더의 index.html을 띄워줍니다.
    }
	
    @GetMapping("/signup")
    public String signupForm() {
        return "signup"; // templates 폴더의 signup.html을 띄움
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login"; // templates 폴더의 login.html을 띄움
    }

    @GetMapping("/mypage")
    public String mypageForm(HttpSession session) {
        // TODO: 로그인 안 된 유저가 접근하면 튕겨내는 로직 필요
        return "mypage"; // templates 폴더의 mypage.html을 띄움
    }


    // ==========================================
    // 2. 폼(Form) 데이터 처리 (POST)
    // ==========================================

    @PostMapping("/signup")
    public String signup(MemberDto memberDto) { // @RequestBody 제거 (Form 전송 방식)
        memberService.signup(memberDto);
        return "redirect:/member/login"; // 가입 성공 시 로그인 페이지로 강제 이동
    }

    @PostMapping("/login")
    public String login(LoginDto loginDto, HttpSession session) {
        try {
            // 1. 서비스의 실제 로그인 메서드 호출 (DB에서 유저 찾아오기)
            MemberDto loginUser = memberService.login(loginDto);

            // 2. [핵심] 조회된 진짜 유저 정보를 세션에 저장!
            session.setAttribute("loginUser", loginUser);

            // 3. 메인 홈으로 이동
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            // 로그인 실패 시 다시 로그인 페이지로 (필요시 에러 메시지 처리)
            return "redirect:/member/login?error";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); 
        return "redirect:/"; // 로그아웃 후 메인 홈으로 이동
        
    }

    // HTML 기본 폼은 PUT/DELETE를 지원하지 않아 POST로 변경합니다.
    @PostMapping("/profile")
    public String updateProfile(ProfileDto profileDto, HttpSession session) {
        // TODO: DB 업데이트
        return "redirect:/member/mypage"; // 수정 후 마이페이지로 새로고침
    }

    @PostMapping("/withdraw")
    public String withdraw(HttpSession session) {
        // TODO: DB 데이터 삭제
        session.invalidate(); 
        return "redirect:/"; // 탈퇴 후 메인 홈으로 이동
    }
}