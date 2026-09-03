package com.yse.dev.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Data;


@Entity
@Table(
    name = "favorite",
    uniqueConstraints = {

        @UniqueConstraint(
            name = "uk_favorite_user_movie",
            columnNames = {
                "user_id",
                "movie_id"
            }
        )

    }
)
@Data
public class Favorite {


    // ==========================================
    // 찜 고유 번호
    // ==========================================

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;



    // ==========================================
    // 회원 아이디
    // ==========================================

    @Column(
        name = "user_id",
        nullable = false,
        length = 50
    )
    private String userId;



    // ==========================================
    // TMDB 영화 ID
    // ==========================================

    @Column(
        name = "movie_id",
        nullable = false
    )
    private Long movieId;



    // ==========================================
    // 찜 등록 시간
    // ==========================================

    @Column(
        name = "created_at",
        nullable = false
    )
    private LocalDateTime createdAt;



    // ==========================================
    // 저장 직전에 등록시간 설정
    // ==========================================

    @PrePersist
    public void prePersist() {

        createdAt =
            LocalDateTime.now();

    }

}