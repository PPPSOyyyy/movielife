package com.yse.dev.Service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.yse.dev.Entity.Favorite;
import com.yse.dev.Repository.FavoriteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    // 찜 추가
    public Favorite addFavorite(String userId, Long movieId) {

        // 이미 찜한 영화인지 확인
        if (favoriteRepository.existsByUserIdAndMovieId(userId, movieId)) {
            throw new IllegalArgumentException(
                    "이미 찜한 영화입니다.");
        }

        Favorite favorite = new Favorite();

        favorite.setUserId(userId);
        favorite.setMovieId(movieId);

        return favoriteRepository.save(favorite);
    }

    // 찜 취소
    public void deleteFavorite(String userId, Long movieId) {

        Favorite favorite =
                favoriteRepository.findByUserIdAndMovieId(
                        userId,
                        movieId
                );

        if (favorite == null) {
            throw new IllegalArgumentException(
                    "찜한 영화를 찾을 수 없습니다.");
        }

        favoriteRepository.delete(favorite);
    }

    // 내가 찜한 영화 목록
    public List<Favorite> getMyFavorites(String userId) {

        return favoriteRepository.findByUserId(userId);
    }

    // 찜 여부 확인
    public boolean isFavorite(String userId, Long movieId) {

        return favoriteRepository.existsByUserIdAndMovieId(
                userId,
                movieId
        );
    }
}