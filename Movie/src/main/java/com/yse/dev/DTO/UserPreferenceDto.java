package com.yse.dev.DTO;

import java.util.List;

public class UserPreferenceDto {
    private Long id;
    private String userName;
    private List<String> favoriteGenres;
    private List<String> favoriteMovies;

    public UserPreferenceDto(Long id, String userName, List<String> favoriteGenres, List<String> favoriteMovies) {
        this.id = id;
        this.userName = userName;
        this.favoriteGenres = favoriteGenres;
        this.favoriteMovies = favoriteMovies;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public List<String> favoriteGenres() { return favoriteGenres; }
    public List<String> getFavoriteGenres() { return favoriteGenres; }
    public void setFavoriteGenres(List<String> favoriteGenres) { this.favoriteGenres = favoriteGenres; }
    public List<String> getFavoriteMovies() { return favoriteMovies; }
    public void setFavoriteMovies(List<String> favoriteMovies) { this.favoriteMovies = favoriteMovies; }
}