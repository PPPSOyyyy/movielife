package com.yse.dev.Service;
import java.nio.charset.StandardCharsets;

/** 회원가입과 프로필 수정에서 공통으로 사용하는 서버 검증 규칙. */
public final class MemberValidation {
    private MemberValidation() { }
    public static String userId(String value) {
        String result = value == null ? "" : value.trim();
        if (!result.matches("[A-Za-z0-9_]{4,20}"))
            throw new IllegalArgumentException("아이디는 영문, 숫자, 밑줄로 4~20자 입력해 주세요.");
        return result;
    }
    public static String nickname(String value) {
        String result = value == null ? "" : value.trim();
        if (!result.matches("[가-힣A-Za-z0-9_]{2,20}"))
            throw new IllegalArgumentException("닉네임은 한글, 영문, 숫자, 밑줄로 2~20자 입력해 주세요.");
        return result;
    }
    public static void password(String value) {
        if (value == null || value.length() < 8 || value.length() > 50
                || value.getBytes(StandardCharsets.UTF_8).length > 72
                || !value.matches(".*[A-Za-z].*") || !value.matches(".*[0-9].*")
                || !value.matches(".*[^A-Za-z0-9\\s].*") || value.matches("(?s).*\\s.*"))
            throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 포함하여 공백 없이 8~50자로 입력해 주세요. (최대 72바이트)");
    }
}
