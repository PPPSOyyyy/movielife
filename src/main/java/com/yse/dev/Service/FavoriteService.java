package com.yse.dev.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yse.dev.Entity.Favorite;
import com.yse.dev.Repository.FavoriteRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class FavoriteService {


    private final FavoriteRepository
            favoriteRepository;



    // ==========================================
    // 찜 추가
    // ==========================================

    @Transactional
    public Favorite addFavorite(
            String userId,
            Long movieId) {


        // 로그인 회원 확인
        if (
            userId == null ||
            userId.isBlank()
        ) {

            throw new IllegalArgumentException(
                "로그인이 필요합니다."
            );

        }


        // 영화 ID 확인
        if (movieId == null) {

            throw new IllegalArgumentException(
                "영화 정보가 올바르지 않습니다."
            );

        }


        /*
         * 동일한 사용자가
         * 동일한 영화를 이미 찜했는지 확인
         */

        boolean alreadyFavorite =

            favoriteRepository
                .existsByUserIdAndMovieId(
                    userId,
                    movieId
                );


        if (alreadyFavorite) {

            throw new IllegalArgumentException(
                "이미 찜한 영화입니다."
            );

        }


        /*
         * 중복이 아닐 때만
         * 새로운 Favorite 생성
         */

        Favorite favorite =
            new Favorite();


        favorite.setUserId(
            userId
        );


        favorite.setMovieId(
            movieId
        );


        return favoriteRepository.save(
            favorite
        );

    }



    // ==========================================
    // 찜 취소
    // ==========================================

    @Transactional
    public void deleteFavorite(
            String userId,
            Long movieId) {


        Favorite favorite =

            favoriteRepository
                .findByUserIdAndMovieId(
                    userId,
                    movieId
                )
                .orElseThrow(() ->

                    new IllegalArgumentException(
                        "찜한 영화를 찾을 수 없습니다."
                    )

                );


        favoriteRepository.delete(
            favorite
        );

    }



    // ==========================================
    // 내가 찜한 영화 목록
    // ==========================================

    @Transactional(readOnly = true)
    public List<Favorite> getMyFavorites(
            String userId) {


        return favoriteRepository
                .findByUserId(
                    userId
                );

    }



    // ==========================================
    // 현재 영화 찜 여부 확인
    // ==========================================

    @Transactional(readOnly = true)
    public boolean isFavorite(
            String userId,
            Long movieId) {


        return favoriteRepository
                .existsByUserIdAndMovieId(
                    userId,
                    movieId
                );

    }

}