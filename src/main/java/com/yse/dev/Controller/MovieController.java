package com.yse.dev.Controller;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.yse.dev.DTO.MovieDto;
import com.yse.dev.Service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;

@Controller
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;
    @GetMapping({"/movies", "/movie-list"})
    public String movieList(@RequestParam(value="query", required=false) String query,
            @RequestParam(value="genre", required=false) Integer genre,
            @RequestParam(value="minRating", required=false) Double minRating,
            @RequestParam(value="year", required=false) Integer year,
            @RequestParam(value="provider", required=false) Integer provider,
            @RequestParam(value="tab", defaultValue="all") String tab,
            @RequestParam(value="page", defaultValue="1") int page, Model model) {
        page = Math.max(1, Math.min(500, page));
        query = query == null ? "" : query.trim();
        if (query.length() > 100) query = query.substring(0, 100);
        if (!List.of("all", "topRated", "upcoming").contains(tab)) tab = "all";
        if (genre != null && genre <= 0) genre = null;
        if (provider != null && provider <= 0) provider = null;
        if (minRating != null && (!Double.isFinite(minRating) || minRating < 0 || minRating > 10)) minRating = null;
        if (year != null && (year < 1888 || year > LocalDate.now().getYear() + 10)) year = null;
        boolean searching = !query.isBlank();
        boolean filtering = !searching && (genre != null || minRating != null || year != null || provider != null);
        if (searching) { genre = null; minRating = null; year = null; provider = null; tab = "all"; }
        if (filtering) tab = "all";
        String title = searching ? "‘" + query + "’ 검색 결과" : filtering ? "필터 검색 결과"
                : switch (tab) { case "topRated" -> "평점 높은 영화"; case "upcoming" -> "개봉 예정 영화"; default -> "영화 둘러보기"; };
        Map<String, Object> response = Map.of();
        try {
            response = load(query, genre, minRating, year, provider, tab, page, searching, filtering);
            int last = Math.max(1, Math.min(500, count(response, "total_pages")));
            if (page > last) {
                page = last;
                response = load(query, genre, minRating, year, provider, tab, page, searching, filtering);
            }
        } catch (RestClientException e) {
            model.addAttribute("catalogError", "영화 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
        List<MovieDto> movies = movieService.convertToMovieList(response);
        int totalPages = Math.max(1, Math.min(500, count(response, "total_pages")));
        int startPage = Math.max(1, Math.min(page - 2, totalPages - 4));
        model.addAttribute("movies", movies);
        model.addAttribute("query", query);
        model.addAttribute("selectedGenre", genre);
        model.addAttribute("selectedMinRating", minRating);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedProvider", provider);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalResults", count(response, "total_results"));
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", Math.min(totalPages, startPage + 4));
        model.addAttribute("pageTitle", title);
        model.addAttribute("activeTab", tab);
        model.addAttribute("currentYear", LocalDate.now().getYear());
        return "movie-list";
    }
    private Map<String, Object> load(String query, Integer genre, Double rating, Integer year,
            Integer provider, String tab, int page, boolean searching, boolean filtering) {
        if (searching) return movieService.searchMovies(query, page);
        if (filtering) return movieService.getFilteredMovies(genre, rating, year, provider, page);
        return switch (tab) {
            case "topRated" -> movieService.getTopRatedMovies(page);
            case "upcoming" -> movieService.getUpcomingMovies(page);
            default -> movieService.getPopularMovies(page);
        };
    }
    @GetMapping("/movies/{movieId}")
    public String movieDetail(@PathVariable("movieId") Long movieId, Model model) {
        model.addAttribute("movie", movieService.getMovieDetail(movieId));
        return "movie-detail";
    }
    private int count(Map<String, Object> value, String key) {
        return value != null && value.get(key) instanceof Number n ? n.intValue() : 0;
    }
}
