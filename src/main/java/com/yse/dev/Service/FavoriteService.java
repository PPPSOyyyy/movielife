package com.yse.dev.Service;
import java.util.List;
import com.yse.dev.Entity.Favorite;
import com.yse.dev.Repository.FavoriteRepository;
import com.yse.dev.Repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    @Transactional
    public Favorite addFavorite(String userId, Long movieId) {
        if (movieId == null || movieId <= 0) throw new IllegalArgumentException("영화 정보가 올바르지 않습니다.");
        memberRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다."));
        // 동시 클릭과 재요청에도 찜을 한 번만 등록합니다.
        return favoriteRepository.findByUserIdAndMovieId(userId, movieId).orElseGet(() -> {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setMovieId(movieId);
            return favoriteRepository.save(favorite);
        });
    }
    @Transactional
    public void deleteFavorite(String userId, Long movieId) {
        memberRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다."));
        favoriteRepository.deleteByUserIdAndMovieId(userId, movieId);
    }
    @Transactional(readOnly = true)
    public List<Favorite> getMyFavorites(String userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    @Transactional(readOnly = true)
    public boolean isFavorite(String userId, Long movieId) {
        return favoriteRepository.existsByUserIdAndMovieId(userId, movieId);
    }
}
