package com.yse.dev.Service;

import com.yse.dev.DTO.LoginDto;
import com.yse.dev.DTO.MemberDto;
import com.yse.dev.DTO.ProfileDto;
import com.yse.dev.Entity.Member;
import com.yse.dev.Repository.FavoriteRepository;
import com.yse.dev.Repository.MemberRepository;
import com.yse.dev.Repository.MemberPreferenceRepository;
import com.yse.dev.Repository.MovieViewHistoryRepository;
import com.yse.dev.Repository.ReviewRepository;
import com.yse.dev.Repository.ReviewCommentRepository;
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
    private final ReviewCommentRepository reviewCommentRepository;
    private final MemberPreferenceRepository memberPreferenceRepository;
    private final MovieViewHistoryRepository movieViewHistoryRepository;
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
        member.setName(RecoveryRules.name(dto.getName()));
        member.setEmail(RecoveryRules.email(dto.getEmail()));
        if(memberRepository.existsByEmail(member.getEmail())) throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        member.setSecurityQuestion(RecoveryRules.question(dto.getSecurityQuestion()));
        member.setSecurityAnswerHash(passwordEncoder.encode(RecoveryRules.answer(dto.getSecurityAnswer())));
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
        reviewCommentRepository.deleteCommentsOnReviewsByUserId(userId);
        reviewCommentRepository.deleteByUserId(userId);
        reviewRepository.deleteByUserId(userId);
        memberPreferenceRepository.deleteByUserId(userId);
        movieViewHistoryRepository.deleteByUserId(userId);
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

    @Transactional(readOnly=true)
    public boolean isEmailDuplicate(String email) { return memberRepository.existsByEmail(RecoveryRules.email(email)); }
    @Transactional(readOnly=true)
    public String findId(String name, String email) {
        Member m=memberRepository.findByNameAndEmail(RecoveryRules.name(name),RecoveryRules.email(email))
            .orElseThrow(()->new IllegalArgumentException("입력한 정보와 일치하는 회원이 없습니다."));
        return m.getUserId();
    }
    @Transactional(readOnly=true)
    public String recoveryQuestion(String id) {
        Member m=getMemberByUserId(id);
        if(m.getSecurityAnswerHash()==null) throw new IllegalArgumentException("복구 정보가 등록되지 않은 계정입니다. 로그인 후 회원정보 관리에서 등록하거나 관리자에게 문의해 주세요.");
        return RecoveryRules.QUESTIONS.get(m.getSecurityQuestion());
    }
    @Transactional(readOnly=true)
    public String verifyAnswer(String id, String answer) {
        Member m=getMemberByUserId(id);
        if(m.getSecurityAnswerHash()==null || !passwordEncoder.matches(RecoveryRules.answer(answer),m.getSecurityAnswerHash()))
            throw new IllegalArgumentException("보안 질문 답변이 일치하지 않습니다.");
        return m.getPassword(); // 세션 내부에서만 보관; 비밀번호 변경 시 기존 복구 권한 무효화
    }
    @Transactional
    public void resetPassword(String id,String proof,String password,String confirm) {
        Member m=memberRepository.findByUserIdForUpdate(id).orElseThrow(this::invalidLogin);
        if(!java.util.Objects.equals(proof,m.getPassword())) throw new IllegalArgumentException("복구를 처음부터 다시 진행해 주세요.");
        if(!java.util.Objects.equals(password,confirm)) throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        MemberValidation.password(password);
        m.setPassword(passwordEncoder.encode(password));
        memberRepository.saveAndFlush(m);
    }
    @Transactional
    public void updateAccount(String id, com.yse.dev.DTO.AccountDto dto) {
        Member m=memberRepository.findByUserIdForUpdate(id).orElseThrow(this::invalidLogin);
        String nickname=MemberValidation.nickname(dto.getNickname());
        if(!nickname.equals(m.getNickname()) && memberRepository.existsByNickname(nickname)) throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        if(dto.getName()!=null && !dto.getName().isBlank()) m.setName(RecoveryRules.name(dto.getName()));
        if(dto.getEmail()!=null && !dto.getEmail().isBlank()) {
            String email=RecoveryRules.email(dto.getEmail());
            if(!email.equals(m.getEmail())) {
                if(!matches(dto.getCurrentPassword(),m.getPassword())) throw new IllegalArgumentException("이메일 또는 복구 정보 변경에는 현재 비밀번호가 필요합니다.");
                if(memberRepository.existsByEmail(email)) throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                m.setEmail(email);
            }
        }
        if(dto.getSecurityAnswer()!=null && !dto.getSecurityAnswer().isBlank()) {
            if(!matches(dto.getCurrentPassword(),m.getPassword())) throw new IllegalArgumentException("복구 정보 변경에는 현재 비밀번호가 필요합니다.");
            m.setSecurityQuestion(RecoveryRules.question(dto.getSecurityQuestion()));
            m.setSecurityAnswerHash(passwordEncoder.encode(RecoveryRules.answer(dto.getSecurityAnswer())));
        }
        m.setNickname(nickname);
        memberRepository.saveAndFlush(m);
    }
    @Transactional
    public void changePassword(String id, com.yse.dev.DTO.AccountDto dto) {
        if(!java.util.Objects.equals(dto.getPassword(),dto.getPasswordConfirm())) throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        MemberValidation.password(dto.getPassword());
        updateProfile(id,new ProfileDto(null,dto.getCurrentPassword(),dto.getPassword()));
    }

}
