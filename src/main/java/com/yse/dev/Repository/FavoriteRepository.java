package com.yse.dev.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yse.dev.Entity.Favorite;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // 특정 사용자가 찜한 영화 조회
    List<Favorite> findByUserId(String userId);

    // 특정 사용자가 특정 영화를 찜했는지 확인
    boolean existsByUserIdAndMovieId(String userId, Long movieId);

    // 특정 사용자의 특정 영화 찜 찾기
    Favorite findByUserIdAndMovieId(String userId, Long movieId);
}