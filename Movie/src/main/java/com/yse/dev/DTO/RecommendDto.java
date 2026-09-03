package com.yse.dev.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendDto {
    private Long id;             
    private String title;        
    private String posterPath;   
    private double voteAverage;  
    private String releaseDate;  
    private String overview;     
}