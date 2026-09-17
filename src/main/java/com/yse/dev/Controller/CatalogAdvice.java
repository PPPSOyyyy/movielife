package com.yse.dev.Controller;
import com.yse.dev.Service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import java.util.List;
@ControllerAdvice(assignableTypes={MovieController.class,TeamPageController.class,HomeController.class})
@RequiredArgsConstructor
public class CatalogAdvice {
    private final MovieService movies;
    @ModelAttribute public void providers(Model m,jakarta.servlet.http.HttpServletRequest request){
        m.addAttribute("securityQuestions",com.yse.dev.Service.RecoveryRules.QUESTIONS);
        String path=request.getRequestURI();
        if(!(path.equals("/")||path.equals("/ott")||path.equals("/popular")||path.equals("/movie-list")||path.startsWith("/movies")))return;
        try {m.addAttribute("krProviders",movies.krProviders());}
        catch(org.springframework.web.client.RestClientException e){m.addAttribute("krProviders",List.of());m.addAttribute("providerError","KR 제공처 정보를 불러오지 못했습니다.");}
    }
}
