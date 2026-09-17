package com.yse.dev.Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;


@ControllerAdvice(
        assignableTypes = {
                MovieController.class,
                ReviewPageController.class,
                PersonController.class
        }
)
public class MoviePageExceptionHandler {


    // =========================================================
    // 영화 / 배우 관련 오류 처리
    // =========================================================

    @ExceptionHandler({
            RestClientException.class,
            IllegalArgumentException.class
    })
    public String unavailable(

            Exception error,

            HttpServletRequest request,

            HttpServletResponse response,

            Model model) {


        // =====================================================
        // 콘솔에 실제 오류 강제 출력
        // =====================================================

        System.err.println();
        System.err.println(
                "======================================================"
        );
        System.err.println(
                "[movieLife 오류 발생]"
        );
        System.err.println(
                "요청 주소 : "
                + request.getRequestURI()
        );
        System.err.println(
                "QueryString : "
                + request.getQueryString()
        );
        System.err.println(
                "예외 종류 : "
                + error.getClass().getName()
        );
        System.err.println(
                "예외 내용 : "
                + error.getMessage()
        );
        System.err.println(
                "======================================================"
        );


        error.printStackTrace();


        System.err.println(
                "======================================================"
        );
        System.err.println();


        // =====================================================
        // 404 여부 판단
        // =====================================================

        boolean missing =

                error
                instanceof HttpClientErrorException.NotFound

                ||

                error
                instanceof IllegalArgumentException;


        response.setStatus(
                missing
                        ? HttpServletResponse.SC_NOT_FOUND
                        : HttpServletResponse.SC_SERVICE_UNAVAILABLE
        );


        // =====================================================
        // 화면에 표시할 내용
        // =====================================================

        if (
            missing
        ) {

            model.addAttribute(
                    "errorTitle",
                    "영화를 찾을 수 없습니다"
            );


            model.addAttribute(
                    "errorDescription",
                    "요청한 영화 또는 배우 정보를 찾지 못했습니다."
            );

        } else {

            model.addAttribute(
                    "errorTitle",
                    "영화 정보를 불러오지 못했습니다"
            );


            model.addAttribute(
                    "errorDescription",
                    "외부 영화 정보를 불러오는 중 문제가 발생했습니다."
            );
        }


        // 디버깅 중에만 표시
        model.addAttribute(
                "errorDetail",
                error.getMessage()
        );


        return "error";
    }
}