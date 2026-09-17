package com.yse.dev.Controller;
import com.yse.dev.Service.*;
import com.yse.dev.DTO.AccountDto;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class AccountController {
    private final MemberService members;
    private final RecoveryLimiter limiter;
    private record Grant(String id,String proof,long expires) implements java.io.Serializable {}
    private String id(HttpSession s) {
        Object id=s.getAttribute("loginUserId");
        if(id==null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,"로그인이 필요합니다.");
        return id.toString();
    }
    private void limit(HttpServletRequest req,String id) {
        limiter.check("ip:"+req.getRemoteAddr(),30);
        if(id!=null) limiter.check("id:"+id.trim().toLowerCase(Locale.ROOT),10);
    }
    @GetMapping("/security-questions") public Map<String,String> questions() { return RecoveryRules.QUESTIONS; }
    @GetMapping("/check-email") public boolean email(@RequestParam(name="email") String email) { return members.isEmailDuplicate(email); }
    @PostMapping("/find-id") public Map<String,String> find(@RequestBody Map<String,String> b,HttpServletRequest r) {
        limit(r,null); return Map.of("userId",members.findId(b.get("name"),b.get("email")));
    }
    @PostMapping("/recovery/question") public Map<String,String> question(@RequestBody Map<String,String> b,HttpServletRequest r) {
        String id=MemberValidation.userId(b.get("userId")); limit(r,id);
        r.getSession().removeAttribute("resetGrant");
        return Map.of("question",members.recoveryQuestion(id));
    }
    @PostMapping("/recovery/verify") public Map<String,String> verify(@RequestBody Map<String,String> b,HttpServletRequest r) {
        String id=MemberValidation.userId(b.get("userId")); limit(r,id);
        r.getSession().removeAttribute("resetGrant");
        String proof=members.verifyAnswer(id,b.get("answer"));
        r.changeSessionId();
        r.getSession().setAttribute("resetGrant",new Grant(id,proof,System.currentTimeMillis()+300000));
        return Map.of("message","답변을 확인했습니다. 5분 안에 새 비밀번호를 설정해 주세요.");
    }
    @PostMapping("/recovery/reset") public Map<String,String> reset(@RequestBody AccountDto b,HttpServletRequest r) {
        HttpSession s=r.getSession();
        synchronized(s) {
            Object value=s.getAttribute("resetGrant");
            if(!(value instanceof Grant g) || g.expires()<System.currentTimeMillis()) throw new IllegalArgumentException("복구 인증이 만료되었습니다. 처음부터 진행해 주세요.");
            limit(r,g.id());
            members.resetPassword(g.id(),g.proof(),b.getPassword(),b.getPasswordConfirm());
            s.invalidate();
        }
        return Map.of("message","새 비밀번호를 설정했습니다. 다시 로그인해 주세요.");
    }
    @PutMapping("/account") public Map<String,String> account(@RequestBody AccountDto b,HttpSession s) {
        members.updateAccount(id(s),b); return Map.of("message","개인정보를 저장했습니다.");
    }
    @PutMapping("/password") public Map<String,String> password(@RequestBody AccountDto b,HttpServletRequest r) {
        members.changePassword(id(r.getSession()),b); r.changeSessionId();
        return Map.of("message","비밀번호를 변경했습니다.");
    }
}
