package com.yse.dev.Service;

import com.yse.dev.DTO.LoginDto;
import com.yse.dev.DTO.MemberDto;
import com.yse.dev.DTO.ProfileDto;
import com.yse.dev.Entity.Member;
import com.yse.dev.Repository.FavoriteRepository;
import com.yse.dev.Repository.MemberRepository;
import com.yse.dev.Repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final FavoriteRepository favoriteRepository;
    private final ReviewRepository reviewRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public void signup(MemberDto dto) {
        String userId = MemberValidation.userId(dto.getUserId());
        String nickname = MemberValidation.nickname(dto.getNickname());
        MemberValidation.password(dto.getPassword());
        if (memberRepository.existsByUserId(userId)) throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        if (memberRepository.existsByNickname(nickname)) throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        Member member = new Member();
        member.setUserId(userId);
        member.setNickname(nickname);
        member.setPassword(passwordEncoder.encode(dto.getPassword()));
        memberRepository.saveAndFlush(member);
    }

    @Transactional
    public Member login(LoginDto dto) {
        String userId = dto.getUserId() == null ? "" : dto.getUserId().trim();
        Member member = memberRepository.findByUserId(userId).orElseThrow(this::invalidLogin);
        if (!matches(dto.getPassword(), member.getPassword())) throw invalidLogin();
        // 기존 평문 계정은 첫 로그인 때 암호화하여 이전 회원도 계속 사용할 수 있습니다.
        if (!isEncoded(member.getPassword())) {
            member.setPassword(passwordEncoder.encode(dto.getPassword()));
            memberRepository.save(member);
        }
        return member;
    }

    @Transactional(readOnly = true)
    public Member getMemberByUserId(String userId) {
        return memberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
    }

    @Transactional
    public void updateProfile(String userId, ProfileDto dto) {
        Member member = memberRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        String nickname = dto.getNickname() == null ? member.getNickname() : MemberValidation.nickname(dto.getNickname());
        if (!nickname.equals(member.getNickname()) && memberRepository.existsByNickname(nickname))
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            if (!matches(dto.getCurrentPassword(), member.getPassword()))
                throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
            MemberValidation.password(dto.getPassword());
            member.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        member.setNickname(nickname);
        memberRepository.saveAndFlush(member);
    }

    @Transactional
    public void withdraw(String userId, String password) {
        Member member = memberRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        if (!matches(password, member.getPassword())) throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        // 같은 아이디로 재가입해도 이전 찜/리뷰가 노출되지 않도록 함께 삭제합니다.
        favoriteRepository.deleteByUserId(userId);
        reviewRepository.deleteByUserId(userId);
        memberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public boolean isUserIdDuplicate(String value) {
        return memberRepository.existsByUserId(MemberValidation.userId(value));
    }
    @Transactional(readOnly = true)
    public boolean isNicknameDuplicate(String value) {
        return memberRepository.existsByNickname(MemberValidation.nickname(value));
    }
    private boolean matches(String input, String saved) {
        if (input == null || input.isEmpty() || saved == null) return false;
        return isEncoded(saved) ? passwordEncoder.matches(input, saved) : saved.equals(input);
    }
    private boolean isEncoded(String value) {
        return value != null && value.matches("^\\$2[aby]\\$.*");
    }
    private IllegalArgumentException invalidLogin() {
        return new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다. 입력한 정보를 다시 확인해 주세요.");
    }
}
