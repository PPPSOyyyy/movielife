package com.yse.dev.DTO;

import lombok.Data;

@Data
public class MemberDto {
    private String userId;   // 회원 아이디
    private String password; // 비밀번호
    private String nickname; // 닉네임
}