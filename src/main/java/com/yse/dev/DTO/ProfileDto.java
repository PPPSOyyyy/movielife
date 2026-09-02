package com.yse.dev.DTO;

import lombok.Data;

@Data
public class ProfileDto {
    private String nickname; // 변경할 닉네임
    private String password; // 변경할 비밀번호 (입력했을 때만 처리)
}