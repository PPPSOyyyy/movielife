package com.yse.dev.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.yse.dev.Entity.Favorite;
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserId(String userId);
    List<Favorite> findByUserIdOrderByCreatedAtDesc(String userId);
    boolean existsByUserIdAndMovieId(String userId, Long movieId);
    Optional<Favorite> findByUserIdAndMovieId(String userId, Long movieId);
    void deleteByUserId(String userId);
    void deleteByUserIdAndMovieId(String userId, Long movieId);
}
