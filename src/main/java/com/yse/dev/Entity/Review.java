package com.yse.dev.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "review")
@Data
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인한 사용자 아이디
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    // 영화 API에서 가져온 영화 ID
    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    // 별점 (0.5 ~ 5.0)
    @Column(nullable = false)
    private Integer rating;

    // 리뷰 내용
    @Column(nullable = false, length = 1000)
    private String content;

    // 작성일
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 수정일
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}