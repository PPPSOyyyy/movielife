package com.yse.dev.Service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.yse.dev.Entity.Member;
import com.yse.dev.Repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements CommandLineRunner {

    private static final String ADMIN_ID = "admin";
    private static final String ADMIN_PASSWORD = "admin1234!";
    private static final String ADMIN_NICKNAME = "관리자";

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        // 기존 관리자 비밀번호를 재시작 때 덮어쓰지 않습니다.
        if (memberRepository.findByUserId(ADMIN_ID).isPresent()) return;
        Member admin = new Member();
        admin.setUserId(ADMIN_ID);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));

        if (admin.getNickname() == null || admin.getNickname().isBlank()) {
            String nickname = memberRepository.existsByNickname(ADMIN_NICKNAME)
                    ? "movieLife관리자"
                    : ADMIN_NICKNAME;
            admin.setNickname(nickname);
        }

        memberRepository.save(admin);
    }
}
