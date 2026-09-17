package com.yse.dev.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yse.dev.Entity.MovieViewHistory;
import com.yse.dev.Repository.MovieViewHistoryRepository;


@Service
public class MovieViewService {

    private final MovieViewHistoryRepository repository;

    public MovieViewService(MovieViewHistoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordView(String userId, Long movieId) {
        if (userId == null || userId.isBlank() || movieId == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        MovieViewHistory history = repository.findByUserIdAndMovieId(userId, movieId)
                .orElse(null);

        if (history == null) {
            history = new MovieViewHistory();
            history.setUserId(userId);
            history.setMovieId(movieId);
            history.setViewCount(1);
            history.setLastViewedAt(now);
            repository.save(history);
            return;
        }

        LocalDate lastDate = history.getLastViewedAt() == null
                ? null
                : history.getLastViewedAt().toLocalDate();

        if (!LocalDate.now().equals(lastDate)) {
            history.setViewCount(Math.max(1, history.getViewCount() == null ? 1 : history.getViewCount()) + 1);
        }

        history.setLastViewedAt(now);
        repository.save(history);
    }

    @Transactional(readOnly = true)
    public List<MovieViewHistory> topViews(String userId, int limit) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }
        List<MovieViewHistory> rows = repository.findByUserIdOrderByViewCountDescLastViewedAtDesc(userId);
        return rows.subList(0, Math.min(Math.max(0, limit), rows.size()));
    }
}
