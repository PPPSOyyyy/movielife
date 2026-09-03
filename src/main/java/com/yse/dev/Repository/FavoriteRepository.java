package com.yse.dev.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yse.dev.Entity.Favorite;


public interface FavoriteRepository
        extends JpaRepository<Favorite, Long> {


    // ==========================================
    // 특정 사용자의 전체 찜 목록
    // ==========================================

    List<Favorite> findByUserId(
        String userId
    );



    // ==========================================
    // 동일 사용자 + 동일 영화 찜 여부
    // ==========================================

    boolean existsByUserIdAndMovieId(
        String userId,
        Long movieId
    );



    // ==========================================
    // 특정 찜 데이터 조회
    // ==========================================

    Optional<Favorite> findByUserIdAndMovieId(
        String userId,
        Long movieId
    );

}