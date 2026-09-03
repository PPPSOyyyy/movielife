package com.yse.dev.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import com.yse.dev.DTO.MovieDto;
import com.yse.dev.Service.MovieService;

@RestController
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/movies/popular")
    public List<MovieDto> getPopularMovies() {
        return movieService.getPopularMovies();
    }
    
    @GetMapping("/movies/top-rated")
    public List<MovieDto> getTopRatedMovies() {
        return movieService.getTopRatedMovies();
    }
    
    @GetMapping("/movies/upcoming")
    public List<MovieDto> getUpcomingMovies() {
        return movieService.getUpcomingMovies();
    }
}