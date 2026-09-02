package com.yse.dev.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class HomeController {


    // ==========================================
    // 메인 화면
    // ==========================================
    @GetMapping("/")
    public String indexPage() {

        return "index";
    }


    // ==========================================
    // 회원가입 화면
    // ==========================================
    @GetMapping("/signup")
    public String signupPage() {

        return "signup";
    }


    // ==========================================
    // 로그인 화면
    // ==========================================
    @GetMapping("/login")
    public String loginPage() {

        return "login";
    }


    // ==========================================
    // 마이페이지 화면
    // ==========================================
    @GetMapping("/mypage")
    public String mypagePage() {

        return "mypage";
    }

}