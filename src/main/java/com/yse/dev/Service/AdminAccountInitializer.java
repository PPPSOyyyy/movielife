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
        Member admin = memberRepository.findByUserId(ADMIN_ID).orElseGet(Member::new);
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
