package com.yse.dev.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.yse.dev.DTO.MovieDto;
import com.yse.dev.DTO.TmdbResponse;

@Service
public class MovieService {

    @Value("${tmdb.api.key}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

    public List<MovieDto> getPopularMovies() {

        String url = "https://api.themoviedb.org/3/movie/popular"
                + "?api_key=" + apiKey
                + "&language=ko-KR"
                + "&page=1";

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(TmdbResponse.class)
                .getResults();
    }
    
    public List<MovieDto> getTopRatedMovies() {

        String url = "https://api.themoviedb.org/3/movie/top_rated"
                + "?api_key=" + apiKey
                + "&language=ko-KR"
                + "&page=1";

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(TmdbResponse.class)
                .getResults();
    }
    
    public List<MovieDto> getUpcomingMovies() {

        String url = "https://api.themoviedb.org/3/movie/upcoming"
                + "?api_key=" + apiKey
                + "&language=ko-KR"
                + "&region=KR"
                + "&page=1";

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(TmdbResponse.class)
                .getResults();
    }
}