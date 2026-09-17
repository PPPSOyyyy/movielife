package com.yse.dev.Controller;
import com.yse.dev.DTO.CatalogFilter;
import com.yse.dev.Service.CatalogModel;
import lombok.RequiredArgsConstructor;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller @RequiredArgsConstructor
public class TeamPageController {
    private static final Map<String,String> COUNTRIES=countries();
    private final CatalogModel catalog;
    @GetMapping("/movies/domestic") public String domestic(@ModelAttribute CatalogFilter f,Model m){return render(false,"KR",f,m);}
    @GetMapping("/movies/foreign") public String foreign(@ModelAttribute CatalogFilter f,Model m){String c=(f.getCountry()==null?"US":f.getCountry()).toUpperCase(Locale.ROOT);return render(true,COUNTRIES.containsKey(c)?c:"US",f,m);}
    private String render(boolean foreign,String country,CatalogFilter f,Model m){
        catalog.populate(f,country,m);
        m.addAttribute("foreignPage",foreign);m.addAttribute("pageTitle",foreign?"해외영화":"국내영화");
        m.addAttribute("pageDescription",foreign?"세계 여러 나라의 다양한 영화를 만나보세요.":"한국 영화의 다양한 이야기를 만나보세요.");
        m.addAttribute("eyebrow",foreign?"WORLD CINEMA":"KOREAN CINEMA");
        m.addAttribute("basePath",foreign?"/movies/foreign":"/movies/domestic");
        m.addAttribute("countryName",foreign?COUNTRIES.get(country):"한국");m.addAttribute("selectedCountry",country);m.addAttribute("countries",COUNTRIES);
        return "country-movies";
    }
    private static Map<String, String> countries() {

        Map<String, String> countries =
                new LinkedHashMap<>();


        countries.put(
                "US",
                "미국"
        );


        countries.put(
                "JP",
                "일본"
        );


        countries.put(
                "GB",
                "영국"
        );


        countries.put(
                "FR",
                "프랑스"
        );


        countries.put(
                "DE",
                "독일"
        );


        countries.put(
                "CA",
                "캐나다"
        );


        countries.put(
                "IN",
                "인도"
        );


        countries.put(
                "ES",
                "스페인"
        );


        countries.put(
                "IT",
                "이탈리아"
        );


        countries.put(
                "AU",
                "호주"
        );


        countries.put(
                "CN",
                "중국"
        );


        countries.put(
                "HK",
                "홍콩"
        );


        countries.put(
                "TW",
                "대만"
        );


        return Collections.unmodifiableMap(
                countries
        );
    }
}