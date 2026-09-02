package com.yse.dev.DTO;

import lombok.Data;

@Data
public class LoginDto {
    private String userId;   // 입력한 아이디
    private String password; // 입력한 비밀번호
}