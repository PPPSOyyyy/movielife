package com.yse.dev.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yse.dev.DTO.LoginDto;
import com.yse.dev.DTO.MemberDto;
import com.yse.dev.DTO.ProfileDto;
import com.yse.dev.Entity.Member;
import com.yse.dev.Repository.MemberRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;


    // ==========================================
    // 회원가입
    // ==========================================
    @Transactional
    public void signup(MemberDto memberDto) {

        if (isUserIdDuplicate(memberDto.getUserId())) {

            throw new IllegalArgumentException(
                    "이미 사용 중인 아이디입니다."
            );
        }


        if (isNicknameDuplicate(memberDto.getNickname())) {

            throw new IllegalArgumentException(
                    "이미 사용 중인 닉네임입니다."
            );
        }


        Member member =
                Member.toEntity(memberDto);


        memberRepository.save(member);
    }



    // ==========================================
    // 로그인
    // ==========================================
    @Transactional(readOnly = true)
    public Member login(LoginDto loginDto) {

        Member member =
                memberRepository
                        .findByUserId(
                                loginDto.getUserId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 아이디입니다."
                                )
                        );


        if (!member.getPassword()
                .equals(loginDto.getPassword())) {

            throw new IllegalArgumentException(
                    "비밀번호가 일치하지 않습니다."
            );
        }


        return member;
    }



    // ==========================================
    // 회원 아이디로 정보 조회
    // ==========================================
    @Transactional(readOnly = true)
    public Member getMemberByUserId(
            String userId) {

        return memberRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "회원 정보를 찾을 수 없습니다."
                        )
                );
    }



    // ==========================================
    // 프로필 수정
    // ==========================================
    @Transactional
    public void updateProfile(
            String userId,
            ProfileDto profileDto) {

        // 아직 다음 단계에서 구현
    }



    // ==========================================
    // 회원 탈퇴
    // ==========================================
    @Transactional
    public void withdraw(
            String userId) {

        // 아직 다음 단계에서 구현
    }



    // ==========================================
    // 아이디 중복확인
    // ==========================================
    @Transactional(readOnly = true)
    public boolean isUserIdDuplicate(
            String userId) {

        return memberRepository
                .existsByUserId(userId);
    }



    // ==========================================
    // 닉네임 중복확인
    // ==========================================
    @Transactional(readOnly = true)
    public boolean isNicknameDuplicate(
            String nickname) {

        return memberRepository
                .existsByNickname(nickname);
    }

}