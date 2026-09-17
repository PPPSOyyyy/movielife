package com.yse.dev.DTO;
import lombok.Data;
@Data
public class AccountDto {
    private String name, nickname, email, currentPassword, password, passwordConfirm;
    private String securityQuestion, securityAnswer;
}
