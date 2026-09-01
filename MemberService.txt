package com.yse.dev.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yse.dev.DTO.ProfileDto;
import com.yse.dev.DTO.LoginDto;
import com.yse.dev.DTO.MemberDto;

import lombok.RequiredArgsConstructor;

// [1. 회원 관리] 핵심 비즈니스 로직을 처리하는 Service
@Service
@RequiredArgsConstructor
public class MemberService {

    // private final MemberRepository memberRepository;
    // private final PasswordEncoder passwordEncoder; 

    // 회원가입 로직

    @Transactional
    public void signup(MemberDto memberDto) {
        // 1. 아이디 중복 검증 (한 번 더 체크)
        if (isUserIdDuplicate(memberDto.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        
        // 2. 비밀번호 암호화 (BCrypt)
        // String encodedPassword = passwordEncoder.encode(memberDto.getPassword());
        // memberDto.setPassword(encodedPassword);

        // 3. DB에 저장 (Entity 변환 후 Repository 호출)
        // MemberEntity memberEntity = MemberEntity.toEntity(memberDto);
        // memberRepository.save(memberEntity);
    }

    // 로그인 로직
    // @return 로그인 성공 시 유저 정보(Dto), 실패 시 null 또는 예외 발생

    @Transactional(readOnly = true)
    public MemberDto login(LoginDto loginDto) {
        // 1. DB에서 아이디로 회원 조회
        // MemberEntity memberEntity = memberRepository.findByUserId(loginDto.getUserId())
        //        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));
        
        // 2. 비밀번호 일치 여부 확인
        // if (!passwordEncoder.matches(loginDto.getPassword(), memberEntity.getPassword())) {
        //     throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        // }

        // 3. 성공 시 DTO로 변환하여 컨트롤러로 반환
        // return MemberDto.toDto(memberEntity);
        return new MemberDto(); // 임시 반환값
    }

    // 프로필 수정 로직

    @Transactional
    public void updateProfile(String userId, ProfileDto profileDto) {
        // 1. 회원 조회
        // MemberEntity memberEntity = memberRepository.findByUserId(userId)
        //        .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 2. 닉네임 및 비밀번호(입력했을 경우) 업데이트
        // if(profileDto.getPassword() != null && !profileDto.getPassword().isEmpty()) {
        //     memberEntity.setPassword(passwordEncoder.encode(profileDto.getPassword()));
        // }
        // memberEntity.setNickname(profileDto.getNickname());
        
        // (JPA를 사용한다면 변경 감지(Dirty Checking) 기능으로 자동 저장)
    }

    // 회원 탈퇴 로직
    
    @Transactional
    public void withdraw(String userId) {
        // 회원 정보를 DB에서 완전히 삭제(delete)하거나, 탈퇴 상태로 변경(update)
        // memberRepository.deleteByUserId(userId);
    }

    // [HTML 화면용] 아이디 중복 확인
    
    @Transactional(readOnly = true)
    public boolean isUserIdDuplicate(String userId) {
        // return memberRepository.existsByUserId(userId);
        return false; // 임시 반환
    }

    // [HTML 화면용] 닉네임 중복 확인
    
    @Transactional(readOnly = true)
    public boolean isNicknameDuplicate(String nickname) {
        // return memberRepository.existsByNickname(nickname);
        return false; // 임시 반환
    }
}