package com.yse.dev.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiRecommendService {

    @Value("${gemini.api.key}")
    private String apiKey;

    public String getAiRecommendedMovieTitles(String userNickname) {
        // 💡 넉넉하게 8~10개 정도의 영화를 추천하도록 프롬프트 수정
        String prompt = String.format(
            "사용자 '%s'의 취향 프로필을 AI가 자동으로 설정하고, 그 취향에 어울리는 영화를 8~10개 정도 추천해주세요. " +
            "다른 미사여구나 번호 매기기 없이, 오직 영화 제목들만 쉼표(,)로 구분해서 한 줄로 출력해주세요. (예: 인셉션, 인터스텔라, 테넷, 매트릭스)",
            userNickname
        );

        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=" + apiKey;
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            Map<String, Object> body = response.getBody();
            
            if (body != null) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> candidateContent = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) candidateContent.get("parts");
                    return (String) parts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return "인셉션, 인터스텔라, 테넷, 매트릭스, 라라랜드";
    }
}