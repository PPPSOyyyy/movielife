package com.yse.dev.Controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 국내/해외 영화 담당자가 구현할 때 이 임시 매핑을 삭제하고 연결해 주세요. */
@Controller
public class TeamPageController {
    @GetMapping("/movies/domestic")
    public String domestic(Model model) {
        model.addAttribute("teamPageTitle", "국내영화");
        return "team-placeholder";
    }
    @GetMapping("/movies/foreign")
    public String foreign(Model model) {
        model.addAttribute("teamPageTitle", "해외영화");
        return "team-placeholder";
    }
}
