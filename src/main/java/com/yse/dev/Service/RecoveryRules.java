package com.yse.dev.Service;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.security.MessageDigest;
public final class RecoveryRules {
    private RecoveryRules() {}
    public static final Map<String,String> QUESTIONS;
    static {
        Map<String,String> q = new LinkedHashMap<>();
        q.put("school", "졸업한 초등학교 이름은?");
        q.put("pet", "첫 번째 반려동물의 이름은?");
        q.put("place", "가장 기억에 남는 장소는?");
        q.put("nickname", "어린 시절 별명은?");
        q.put("teacher", "가장 좋아했던 선생님의 성함은?");
        QUESTIONS = Collections.unmodifiableMap(q);
    }
    public static String name(String value) {
        String v=value==null?"":value.trim();
        if(v.isEmpty() || v.length()>80) throw new IllegalArgumentException("이름은 1~80자로 입력해 주세요.");
        return v;
    }
    public static String email(String value) {
        String v=value==null?"":value.trim().toLowerCase(Locale.ROOT);
        if(v.length()>254 || !v.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("이메일 형식을 확인해 주세요.");
        return v;
    }
    public static String question(String value) {
        if(!QUESTIONS.containsKey(value==null?"":value)) throw new IllegalArgumentException("보안 질문을 선택해 주세요.");
        return value;
    }
    public static String answer(String value) {
        String v=Normalizer.normalize(value==null?"":value.trim(),Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        if(v.isBlank() || v.length()>200) throw new IllegalArgumentException("보안 답변은 1~200자로 입력해 주세요.");
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
