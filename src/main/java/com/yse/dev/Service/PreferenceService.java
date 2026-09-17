package com.yse.dev.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yse.dev.DTO.PreferenceDto;
import com.yse.dev.Entity.MemberPreference;
import com.yse.dev.Repository.MemberPreferenceRepository;


@Service
public class PreferenceService {

    private final MemberPreferenceRepository repository;

    public PreferenceService(MemberPreferenceRepository repository) {
        this.repository = repository;
    }

    // 영화 탐색 화면에서 실제 사용하는 주요 장르와 맞춥니다.
    private static final LinkedHashMap<Integer, String> GENRES = new LinkedHashMap<>();

    static {
        GENRES.put(28, "액션");
        GENRES.put(12, "모험");
        GENRES.put(16, "애니메이션");
        GENRES.put(35, "코미디");
        GENRES.put(18, "드라마");
        GENRES.put(10751, "가족");
        GENRES.put(27, "공포");
        GENRES.put(10749, "로맨스");
        GENRES.put(878, "SF");
        GENRES.put(53, "스릴러");
    }

    public List<Map<String, Object>> genreOptions() {
        List<Map<String, Object>> result = new ArrayList<>();
        GENRES.forEach((id, name) -> result.add(Map.of("id", id, "name", name)));
        return result;
    }

    @Transactional(readOnly = true)
    public boolean isCompleted(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        return repository.findByUserId(userId)
                .map(MemberPreference::isOnboardingCompleted)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<Integer> getGenreIds(String userId) {
        return repository.findByUserId(userId)
                .map(MemberPreference::getGenreIds)
                .map(this::parseGenreIds)
                .orElseGet(List::of);
    }

    @Transactional
    public List<Integer> save(String userId, PreferenceDto dto) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        boolean skip = dto != null && dto.isSkip();

        List<Integer> requested = dto == null || dto.getGenreIds() == null
                ? List.of()
                : dto.getGenreIds().stream().distinct().toList();

        if (!skip) {
            if (requested.size() < 3 || requested.size() > 5) {
                throw new IllegalArgumentException("좋아하는 장르를 3개 이상 5개 이하로 선택해 주세요.");
            }

            Set<Integer> allowed = GENRES.keySet();
            if (requested.stream().anyMatch(id -> !allowed.contains(id))) {
                throw new IllegalArgumentException("장르 선택 정보를 다시 확인해 주세요.");
            }
        }

        MemberPreference preference = repository.findByUserId(userId)
                .orElseGet(MemberPreference::new);

        preference.setUserId(userId);
        preference.setGenreIds(skip ? "" : join(requested));
        preference.setOnboardingCompleted(true);
        repository.save(preference);

        return skip ? List.of() : requested;
    }

    private String join(List<Integer> ids) {
        return ids.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
    }

    private List<Integer> parseGenreIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        List<Integer> result = new ArrayList<>();
        for (String token : raw.split(",")) {
            try {
                int id = Integer.parseInt(token.trim());
                if (GENRES.containsKey(id)) {
                    result.add(id);
                }
            } catch (NumberFormatException ignored) {
                // 잘못된 과거 값은 추천에서 무시합니다.
            }
        }
        return result;
    }
}
