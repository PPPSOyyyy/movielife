package com.yse.dev.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiRecommendService {

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    public String getCustomAiRecommendation(String genre, String mood, String rating) {
        // 1. 장르와 기분을 조합하여 TMDB 장르 ID 목록 생성 
        // (extractGenreAndMoodIds는 '국내','해외'라는 단어를 무시하므로 그대로 두면 됩니다)
        String combinedGenreIds = extractGenreAndMoodIds(genre, mood);
        
        // 🔥 2. genre 문자열에 '국내' 또는 '해외'가 포함되어 있는지 확인해서 언어 필터 생성
        String originFilter = "";
        if (genre != null) {
            if (genre.contains("국내")) {
                originFilter = "&with_original_language=ko";
            } else if (genre.contains("해외")) {
                originFilter = "&without_original_language=ko"; // 한국어 제외
            }
        }

        // 3. TMDB Discover API URL 구성 (originFilter를 URL에 합치기)
        String apiUrl = String.format(
            "https://api.themoviedb.org/3/discover/movie?api_key=%s&language=ko-KR&sort_by=vote_average.desc&vote_count.gte=100&with_genres=%s%s&page=1",
            tmdbApiKey, combinedGenreIds, originFilter
        );

        RestTemplate restTemplate = new RestTemplate();
        try {
            Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);
            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                
                List<String> titles = results.stream()
                    .limit(20)
                    .map(movie -> (String) movie.get("title"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

                return String.join(",", titles);
            }
        } catch (Exception e) {
            System.out.println("TMDB 자동 추천 조회 오류: " + e.getMessage());
        }

        return "쇼생크 탈출, 대부, 인셉션, 인터스텔라, 다크 나이트, 기생충";
    }

 // 장르와 기분을 조합하여 TMDB 장르 ID를 뽑아내는 메서드
    private String extractGenreAndMoodIds(String genreStr, String moodStr) {
        Set<String> idSet = new LinkedHashSet<>();

        // 1) 사용자가 고른 장르 매핑 (추가된 장르 포함)
        if (genreStr != null && !genreStr.contains("상관없음")) {
            for (String token : genreStr.split(",")) {
                String g = token.trim();
                if (g.contains("액션")) idSet.add("28");
                else if (g.contains("스릴러")) idSet.add("53");
                else if (g.contains("공포")) idSet.add("27");
                else if (g.contains("SF")) idSet.add("878");
                else if (g.contains("드라마")) idSet.add("18");
                else if (g.contains("로맨스")) idSet.add("10749");
                else if (g.contains("코미디")) idSet.add("35");
                else if (g.contains("애니메이션")) idSet.add("16");
                // 🔥 새로 추가된 장르
                else if (g.contains("범죄")) idSet.add("80");
                else if (g.contains("미스터리")) idSet.add("9648");
                else if (g.contains("판타지")) idSet.add("14");
                else if (g.contains("음악")) idSet.add("10402");
            }
        }

        // 2) 사용자가 고른 기분(Mood) 매핑 (추가된 기분 포함)
        if (moodStr != null && !moodStr.contains("상관없음")) {
            for (String token : moodStr.split(",")) {
                String m = token.trim();
                
                // 기존 기분
                if (m.contains("웃고")) idSet.add("35");               // 코미디
                else if (m.contains("감동")) { idSet.add("18"); idSet.add("10751"); } // 드라마, 가족
                else if (m.contains("설레")) idSet.add("10749");            // 로맨스
                else if (m.contains("긴장")) { idSet.add("53"); idSet.add("27"); }   // 스릴러, 공포
                else if (m.contains("생각")) { idSet.add("9648"); idSet.add("878"); } // 미스터리, SF
                else if (m.contains("편하게")) { idSet.add("16"); idSet.add("35"); }  // 애니메이션, 코미디
                
                // 🔥 새로 추가된 기분
                else if (m.contains("스트레스")) { idSet.add("28"); idSet.add("80"); idSet.add("35"); } // 액션, 범죄, 코미디
                else if (m.contains("화려한") || m.contains("눈이")) { idSet.add("14"); idSet.add("878"); } // 판타지, SF
                else if (m.contains("귀가")) { idSet.add("10402"); idSet.add("10749"); } // 음악, 로맨스
                else if (m.contains("가족")) { idSet.add("10751"); idSet.add("16"); } // 가족, 애니메이션
            }
        }

        // 아무것도 선택하지 않았을 때 기본값 (액션)
        if (idSet.isEmpty()) {
            return "28";
        }

        // 콤마로 연결 (예: "28,80,35")
        return String.join("|", idSet);
    }
}