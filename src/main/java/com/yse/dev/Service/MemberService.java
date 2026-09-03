package com.yse.dev.Service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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


    /*
     * BCrypt 비밀번호 암호화 객체
     */
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();



    // ==========================================
    // 회원가입
    // ==========================================
    @Transactional
    public void signup(
            MemberDto memberDto) {


        // 아이디 중복확인
        if (
            isUserIdDuplicate(
                memberDto.getUserId()
            )
        ) {

            throw new IllegalArgumentException(
                "이미 사용 중인 아이디입니다."
            );

        }


        // 닉네임 중복확인
        if (
            isNicknameDuplicate(
                memberDto.getNickname()
            )
        ) {

            throw new IllegalArgumentException(
                "이미 사용 중인 닉네임입니다."
            );

        }


        Member member =
                Member.toEntity(
                    memberDto
                );


        /*
         * ★ 비밀번호 BCrypt 암호화
         *
         * 예:
         *
         * 1234
         *
         * ↓
         *
         * $2a$10$..........
         */

        String encodedPassword =
                passwordEncoder.encode(
                    memberDto.getPassword()
                );


        member.setPassword(
            encodedPassword
        );


        memberRepository.save(
            member
        );

    }



    // ==========================================
    // 로그인
    // ==========================================
    @Transactional
    public Member login(
            LoginDto loginDto) {


        Member member =
                memberRepository
                    .findByUserId(
                        loginDto.getUserId()
                    )
                    .orElseThrow(() ->

                        new IllegalArgumentException(
                            "아이디 또는 비밀번호가 올바르지 않습니다."
                        )

                    );


        String savedPassword =
                member.getPassword();


        String inputPassword =
                loginDto.getPassword();



        /*
         * =================================================
         * 기존 회원과 새 회원 모두 로그인 가능하게 처리
         * =================================================
         *
         * 새 회원
         * → BCrypt로 저장되어 있음
         *
         * 기존 회원
         * → 평문 비밀번호가 DB에 남아 있을 수 있음
         */

        boolean passwordMatches;



        // BCrypt 비밀번호인지 확인
        if (
            savedPassword != null &&
            (
                savedPassword.startsWith("$2a$") ||
                savedPassword.startsWith("$2b$") ||
                savedPassword.startsWith("$2y$")
            )
        ) {


            /*
             * 암호화된 비밀번호 비교
             */

            passwordMatches =
                    passwordEncoder.matches(
                        inputPassword,
                        savedPassword
                    );


        } else {


            /*
             * 기존 평문 회원
             */

            passwordMatches =
                    savedPassword != null &&
                    savedPassword.equals(
                        inputPassword
                    );


            /*
             * 기존 평문 회원이 로그인에 성공하면
             * 그 자리에서 BCrypt로 자동 변경
             */

            if (passwordMatches) {


                member.setPassword(

                    passwordEncoder.encode(
                        inputPassword
                    )

                );


                memberRepository.save(
                    member
                );

            }

        }



        if (!passwordMatches) {

            throw new IllegalArgumentException(
                "아이디 또는 비밀번호가 올바르지 않습니다."
            );

        }


        return member;

    }



    // ==========================================
    // 회원 정보 조회
    // ==========================================
    @Transactional(readOnly = true)
    public Member getMemberByUserId(
            String userId) {


        return memberRepository
                .findByUserId(
                    userId
                )
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


        Member member =
                memberRepository
                    .findByUserId(
                        userId
                    )
                    .orElseThrow(() ->

                        new IllegalArgumentException(
                            "회원 정보를 찾을 수 없습니다."
                        )

                    );



        // ==========================================
        // 닉네임 변경
        // ==========================================

        if (
            profileDto.getNickname() != null &&
            !profileDto.getNickname()
                    .trim()
                    .isEmpty()
        ) {


            String newNickname =
                    profileDto
                        .getNickname()
                        .trim();


            /*
             * 현재 닉네임과 다를 경우에만
             * 중복검사를 합니다.
             */

            if (
                !newNickname.equals(
                    member.getNickname()
                )
            ) {


                if (
                    memberRepository
                        .existsByNickname(
                            newNickname
                        )
                ) {

                    throw new IllegalArgumentException(
                        "이미 사용 중인 닉네임입니다."
                    );

                }


                member.setNickname(
                    newNickname
                );

            }

        }



        // ==========================================
        // 비밀번호 변경
        // ==========================================

        if (
            profileDto.getPassword() != null &&
            !profileDto.getPassword()
                    .isBlank()
        ) {


            /*
             * ★ 새 비밀번호도 반드시 BCrypt 암호화
             */

            String encodedPassword =
                    passwordEncoder.encode(
                        profileDto.getPassword()
                    );


            member.setPassword(
                encodedPassword
            );

        }


        memberRepository.save(
            member
        );

    }



    // ==========================================
    // 회원 탈퇴
    // ==========================================
    @Transactional
    public void withdraw(
            String userId,
            String password) {


        Member member =
                memberRepository
                    .findByUserId(
                        userId
                    )
                    .orElseThrow(() ->

                        new IllegalArgumentException(
                            "회원 정보를 찾을 수 없습니다."
                        )

                    );


        // 비밀번호 입력 확인
        if (
            password == null ||
            password.isBlank()
        ) {

            throw new IllegalArgumentException(
                "비밀번호를 입력해 주세요."
            );

        }



        String savedPassword =
                member.getPassword();


        boolean passwordMatches;



        /*
         * BCrypt 저장 회원
         */

        if (
            savedPassword != null &&
            (
                savedPassword.startsWith("$2a$") ||
                savedPassword.startsWith("$2b$") ||
                savedPassword.startsWith("$2y$")
            )
        ) {


            passwordMatches =
                    passwordEncoder.matches(
                        password,
                        savedPassword
                    );


        } else {


            /*
             * 아직 평문으로 남아있는 기존 회원
             */

            passwordMatches =
                    savedPassword != null &&
                    savedPassword.equals(
                        password
                    );

        }



        // 비밀번호 틀림
        if (!passwordMatches) {

            throw new IllegalArgumentException(
                "비밀번호가 올바르지 않습니다."
            );

        }


        // 비밀번호 맞으면 회원 삭제
        memberRepository.delete(
            member
        );

    }



    // ==========================================
    // 아이디 중복확인
    // ==========================================
    @Transactional(readOnly = true)
    public boolean isUserIdDuplicate(
            String userId) {


        return memberRepository
                .existsByUserId(
                    userId
                );

    }



    // ==========================================
    // 닉네임 중복확인
    // ==========================================
    @Transactional(readOnly = true)
    public boolean isNicknameDuplicate(
            String nickname) {


        return memberRepository
                .existsByNickname(
                    nickname
                );

    }

}