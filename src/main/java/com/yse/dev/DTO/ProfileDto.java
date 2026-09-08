package com.yse.dev.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {

    // 닉네임
    private String nickname;


    // 현재 비밀번호
    private String currentPassword;


    // 새 비밀번호
    private String password;

}