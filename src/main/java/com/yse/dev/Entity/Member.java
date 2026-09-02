package com.yse.dev.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "member")
@Data
public class Member {

    @Id // 이 필드를 기본키(PK)로 설정합니다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT (1, 2, 3... 자동 증가)
    private Long id; // DB 내부 관리용 고유 번호

    @Column(name = "user_id", unique = true, nullable = false, length = 50)
    private String userId; // 로그인 아이디 (중복 불가, 필수 입력)

    @Column(nullable = false, length = 100)
    private String password; // 비밀번호 (암호화되어 저장되므로 길게 잡습니다)

    @Column(unique = true, nullable = false, length = 50)
    private String nickname; // 닉네임 (중복 불가)

    // DTO를 Entity로 변환하는 편리한 메서드 (서비스 단에서 사용)
    public static Member toEntity(com.yse.dev.DTO.MemberDto dto) {
        Member entity = new Member();
        entity.setUserId(dto.getUserId());
        entity.setPassword(dto.getPassword());
        entity.setNickname(dto.getNickname());
        return entity;
    }
}