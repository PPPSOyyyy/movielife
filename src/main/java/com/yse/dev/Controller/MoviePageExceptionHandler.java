package com.yse.dev.Controller;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@ControllerAdvice(assignableTypes = {MovieController.class, ReviewPageController.class})
public class MoviePageExceptionHandler {
    @ExceptionHandler({RestClientException.class, IllegalArgumentException.class})
    public String unavailable(Exception error, Model model, HttpServletResponse response) {
        boolean missing = error instanceof HttpClientErrorException.NotFound || error instanceof IllegalArgumentException;
        response.setStatus(missing ? 404 : 503);
        model.addAttribute("errorTitle", missing ? "영화를 찾을 수 없습니다" : "영화 정보를 불러오지 못했습니다");
        model.addAttribute("errorDescription", "영화 목록으로 돌아가 다시 선택해 주세요.");
        return "error";
    }
}
