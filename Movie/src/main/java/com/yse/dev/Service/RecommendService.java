package com.yse.dev.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.yse.dev.DTO.RecommendDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RecommendService {

    private final String apiKey = "005f181dd5057bff5a7f9827147574fa";

    public List<RecommendDto> getTrendingRecommendations() {
        String url = "https://api.themoviedb.org/3/trending/movie/day?api_key=" + apiKey + "&language=ko-KR";
        return fetchMovies(url);
    }

    public List<RecommendDto> getGenreRecommendations(int genreId) {
        String url = "https://api.themoviedb.org/3/discover/movie?api_key=" + apiKey + "&with_genres=" + genreId + "&language=ko-KR";
        return fetchMovies(url);
    }

    private List<RecommendDto> fetchMovies(String url) {
        List<RecommendDto> movieList = new ArrayList<>();
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            // JSON 응답을 자바의 Map 형태로 바로 받아옵니다 (라이브러리 불필요!)
            Map<String, Object> responseMap = restTemplate.getForObject(url, Map.class);
            
            if (responseMap != null && responseMap.containsKey("results")) {
                // results 키에 담긴 영화 목록(List)을 꺼냅니다.
                List<Map<String, Object>> results = (List<Map<String, Object>>) responseMap.get("results");
                
                for (Map<String, Object> item : results) {
                    RecommendDto movie = new RecommendDto();
                    
                    // 영화 ID
                    if (item.get("id") != null) {
                        movie.setId(((Number) item.get("id")).longValue());
                    }
                    
                    // 영화 제목
                    movie.setTitle((String) item.get("title"));
                    
                    // 포스터 이미지 경로 조합
                    String posterPath = (String) item.get("poster_path");
                    if (posterPath != null) {
                        movie.setPosterPath("https://image.tmdb.org/t/p/w500" + posterPath);
                    }
                    
                    // 평점
                    if (item.get("vote_average") != null) {
                        movie.setVoteAverage(((Number) item.get("vote_average")).doubleValue());
                    }
                    
                    // 개봉일
                    movie.setReleaseDate((String) item.get("release_date"));
                    
                    // 줄거리
                    movie.setOverview((String) item.get("overview"));

                    movieList.add(movie);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return movieList;
    }
    // 특정 영화 상세 정보 가져오기
    public RecommendDto getMovieDetails(Long movieId) {
        String url = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + apiKey + "&language=ko-KR";
        RecommendDto movie = new RecommendDto();
        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> item = restTemplate.getForObject(url, Map.class);
            
            if (item != null) {
                movie.setId(((Number) item.get("id")).longValue());
                movie.setTitle((String) item.get("title"));
                
                String posterPath = (String) item.get("poster_path");
                if (posterPath != null) {
                    movie.setPosterPath("https://image.tmdb.org/t/p/w500" + posterPath);
                }
                
                if (item.get("vote_average") != null) {
                    movie.setVoteAverage(((Number) item.get("vote_average")).doubleValue());
                }
                movie.setReleaseDate((String) item.get("release_date"));
                movie.setOverview((String) item.get("overview"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return movie;
    }
 // 영화 제목 키워드로 TMDB에서 검색하여 단일 영화 정보(RecommendDto)를 반환하는 메서드
    public RecommendDto searchMovieByTitle(String title) {
        try {
            // 💡 "YOUR_TMDB_API_KEY" 대신 멤버 변수 apiKey를 사용하도록 수정
            String url = "https://api.themoviedb.org/3/search/movie?api_key=" + apiKey 
                       + "&query=" + java.net.URLEncoder.encode(title, "UTF-8") 
                       + "&language=ko-KR";

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> firstMovie = results.get(0);
                    
                    RecommendDto dto = new RecommendDto();
                    dto.setId(firstMovie.get("id") != null ? Long.valueOf(firstMovie.get("id").toString()) : null);
                    dto.setTitle((String) firstMovie.get("title"));
                    dto.setOverview((String) firstMovie.get("overview"));
                    
                    String posterPath = (String) firstMovie.get("poster_path");
                    if (posterPath != null) {
                        dto.setPosterPath("https://image.tmdb.org/t/p/w500" + posterPath);
                    }
                    
                    dto.setVoteAverage(firstMovie.get("vote_average") != null ? Double.valueOf(firstMovie.get("vote_average").toString()) : 0.0);
                    dto.setReleaseDate((String) firstMovie.get("release_date"));
                    
                    return dto;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
}