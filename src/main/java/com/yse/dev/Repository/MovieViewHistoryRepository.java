package com.yse.dev.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yse.dev.Entity.MovieViewHistory;

public interface MovieViewHistoryRepository extends JpaRepository<MovieViewHistory, Long> {
    Optional<MovieViewHistory> findByUserIdAndMovieId(String userId, Long movieId);
    List<MovieViewHistory> findByUserIdOrderByViewCountDescLastViewedAtDesc(String userId);
    void deleteByUserId(String userId);
}
