package com.yse.dev.Service;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.yse.dev.DTO.MovieDetailDto;
import com.yse.dev.DTO.MovieDto;

@Service
public class MovieService {
    @Value("${tmdb.api.key}") private String apiKey;
    @Value("${tmdb.api.base-url}") private String baseUrl;
    private final RestTemplate restTemplate;
    private record CachedResponse(long expiresAt, Map<String, Object> value) { }
    private final Map<URI, CachedResponse> cache = new LinkedHashMap<>(256, .75f, true);

    public MovieService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(4000);
        factory.setReadTimeout(7000);
        restTemplate = new RestTemplate(factory);
    }

    private UriComponentsBuilder endpoint(String path) {
        return UriComponentsBuilder.fromUriString(baseUrl + path)
                .queryParam("api_key", apiKey).queryParam("language", "ko-KR");
    }
    private int page(int value) { return Math.max(1, Math.min(500, value)); }

    @SuppressWarnings("unchecked")
    private Map<String, Object> request(UriComponentsBuilder builder) {
        URI uri = builder.build().encode().toUri();
        synchronized (cache) {
            CachedResponse cached = cache.get(uri);
            if (cached != null && cached.expiresAt() > System.currentTimeMillis()) return cached.value();
        }
        Map<String, Object> value = restTemplate.getForObject(uri, Map.class);
        if (value == null) throw new ResourceAccessException("영화 응답이 비어 있습니다.");
        synchronized (cache) {
            // 영화 목록/상세만 5분 캐시. 회원 활동은 캐시하지 않습니다.
            cache.put(uri, new CachedResponse(System.currentTimeMillis() + 300_000, value));
            while (cache.size() > 200) cache.remove(cache.keySet().iterator().next());
        }
        return value;
    }

    public Map<String, Object> getPopularMovies(int page) {
        return request(endpoint("/movie/popular").queryParam("page", page(page)));
    }
    public Map<String, Object> searchMovies(String query, int page) {
        return request(endpoint("/search/movie").queryParam("query", query)
                .queryParam("include_adult", false).queryParam("page", page(page)));
    }
    public Map<String, Object> getMoviesByGenre(int genreId, int page) {
        return getFilteredMovies(genreId, null, null, null, page);
    }
    public Map<String, Object> getOttMovies(Integer provider, int page) {
        UriComponentsBuilder builder = endpoint("/discover/movie").queryParam("watch_region", "KR")
                .queryParam("with_watch_monetization_types", "flatrate")
                .queryParam("include_adult", false).queryParam("sort_by", "popularity.desc")
                .queryParam("page", page(page));
        if (provider != null) builder.queryParam("with_watch_providers", provider);
        return request(builder);
    }
    public Map<String, Object> getFilteredMovies(Integer genre, Double minRating, Integer year, Integer provider, int page) {
        UriComponentsBuilder builder = endpoint("/discover/movie").queryParam("include_adult", false)
                .queryParam("sort_by", "popularity.desc").queryParam("page", page(page));
        if (genre != null) builder.queryParam("with_genres", genre);
        if (minRating != null && minRating > 0) builder.queryParam("vote_average.gte", minRating).queryParam("vote_count.gte", 50);
        if (year != null) builder.queryParam("primary_release_year", year);
        if (provider != null) builder.queryParam("watch_region", "KR").queryParam("with_watch_providers", provider)
                .queryParam("with_watch_monetization_types", "flatrate");
        return request(builder);
    }
    public Map<String, Object> getTopRatedMovies(int page) {
        return request(endpoint("/movie/top_rated").queryParam("region", "KR").queryParam("page", page(page)));
    }
    public Map<String, Object> getUpcomingMovies(int page) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        // 한국 극장 개봉일이 내일~6개월 뒤인 영화. API의 실제 페이지 수를 그대로 사용합니다.
        return request(endpoint("/discover/movie").queryParam("region", "KR")
                .queryParam("with_release_type", "3|2").queryParam("include_adult", false)
                .queryParam("release_date.gte", today.plusDays(1))
                .queryParam("release_date.lte", today.plusMonths(6))
                .queryParam("sort_by", "popularity.desc").queryParam("page", page(page)));
    }

    public List<MovieDto> convertToMovieList(Map<String, Object> response) {
        List<MovieDto> result = new ArrayList<>();
        if (response == null) return result;
        for (Map<?, ?> movie : objects(response.get("results"))) {
            if (!(movie.get("id") instanceof Number id)) continue;
            MovieDto dto = new MovieDto();
            dto.setId(id.longValue());
            dto.setTitle(text(movie, "title", "제목 정보 없음"));
            dto.setOverview(text(movie, "overview", ""));
            dto.setPosterPath(text(movie, "poster_path", ""));
            dto.setBackdropPath(text(movie, "backdrop_path", ""));
            dto.setReleaseDate(text(movie, "release_date", ""));
            dto.setVoteAverage(number(movie, "vote_average").doubleValue());
            List<Integer> genres = new ArrayList<>();
            if (movie.get("genre_ids") instanceof List<?> ids)
                for (Object genre : ids) if (genre instanceof Number n) genres.add(n.intValue());
            dto.setGenreIds(genres);
            dto.setOttProviders(new ArrayList<>());
            result.add(dto);
        }
        return result;
    }

    public MovieDetailDto getMovieDetail(Long movieId) {
        if (movieId == null || movieId <= 0) throw new IllegalArgumentException("영화 정보가 올바르지 않습니다.");
        Map<String, Object> movie = request(endpoint("/movie/" + movieId)
                .queryParam("append_to_response", "credits,release_dates,watch/providers"));
        MovieDetailDto dto = new MovieDetailDto();
        dto.setId(movieId);
        dto.setTitle(text(movie, "title", "제목 정보 없음"));
        dto.setOriginalTitle(text(movie, "original_title", ""));
        dto.setOverview(text(movie, "overview", ""));
        dto.setPosterPath(text(movie, "poster_path", ""));
        dto.setBackdropPath(text(movie, "backdrop_path", ""));
        dto.setReleaseDate(text(movie, "release_date", ""));
        dto.setVoteAverage(number(movie, "vote_average").doubleValue());
        dto.setRuntime(number(movie, "runtime").intValue());
        dto.setGenres(names(movie.get("genres")));
        List<String> countries = new ArrayList<>();
        for (Map<?, ?> country : objects(movie.get("production_countries"))) {
            String code = text(country, "iso_3166_1", "");
            countries.add(code.isBlank() ? text(country, "name", "정보 없음")
                    : new Locale("", code).getDisplayCountry(Locale.KOREAN));
        }
        dto.setProductionCountries(countries);
        Map<?, ?> credits = map(movie.get("credits"));
        dto.setCast(names(credits.get("cast")).stream().limit(8).toList());
        dto.setDirector(objects(credits.get("crew")).stream()
                .filter(c -> "Director".equals(c.get("job"))).map(c -> text(c, "name", ""))
                .findFirst().orElse("정보 없음"));
        String certification = "정보 없음";
        for (Map<?, ?> release : objects(map(movie.get("release_dates")).get("results"))) {
            if (!"KR".equals(release.get("iso_3166_1"))) continue;
            for (Map<?, ?> date : objects(release.get("release_dates"))) {
                String value = text(date, "certification", "");
                if (value.isBlank()) continue;
                certification = switch (value) {
                    case "ALL", "0" -> "전체 관람가";
                    case "12" -> "12세 이상 관람가";
                    case "15" -> "15세 이상 관람가";
                    case "18", "19" -> "청소년 관람불가";
                    default -> value;
                };
                break;
            }
        }
        dto.setCertification(certification);
        dto.setOttProviders(providerNames(movie.get("watch/providers")));
        return dto;
    }

    public List<String> getOttProviders(Long movieId) {
        return providerNames(request(endpoint("/movie/" + movieId + "/watch/providers")));
    }
    public Integer getMovieProviderId(String providerName) {
        for (Map<?, ?> provider : objects(request(endpoint("/watch/providers/movie")
                .queryParam("watch_region", "KR")).get("results"))) {
            if (providerName.equalsIgnoreCase(text(provider, "provider_name", ""))
                    && provider.get("provider_id") instanceof Number id) return id.intValue();
        }
        return null;
    }
    private List<String> providerNames(Object value) {
        Map<?, ?> kr = map(map(map(value).get("results")).get("KR"));
        List<String> result = new ArrayList<>();
        for (String kind : List.of("flatrate", "rent", "buy")) {
            for (Map<?, ?> provider : objects(kr.get(kind))) {
                String name = text(provider, "provider_name", "");
                if (!name.isBlank() && !result.contains(name)) result.add(name);
            }
        }
        return result;
    }
    private Map<?, ?> map(Object value) { return value instanceof Map<?, ?> m ? m : Map.of(); }
    private List<Map<?, ?>> objects(Object value) {
        List<Map<?, ?>> result = new ArrayList<>();
        if (value instanceof List<?> list) for (Object item : list) if (item instanceof Map<?, ?> m) result.add(m);
        return result;
    }
    private List<String> names(Object value) {
        return objects(value).stream().map(m -> text(m, "name", "")).filter(n -> !n.isBlank()).toList();
    }
    private String text(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }
    private Number number(Map<?, ?> map, String key) { return map.get(key) instanceof Number n ? n : 0; }
}
