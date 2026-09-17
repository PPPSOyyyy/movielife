package com.yse.dev.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountPageController {

    @GetMapping("/account/find-id")
    public String findId() {
        return "account-find-id";
    }

    @GetMapping("/account/find-password")
    public String findPassword() {
        return "account-find-password";
    }

    // 기존 주소로 들어온 경우에도 페이지가 깨지지 않도록 비밀번호 찾기로 연결
    @GetMapping("/account/recovery")
    public String recovery() {
        return "redirect:/account/find-password";
    }
}
