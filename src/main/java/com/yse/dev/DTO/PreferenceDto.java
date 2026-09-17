package com.yse.dev.DTO;

import java.util.ArrayList;
import java.util.List;

public class PreferenceDto {
    private List<Integer> genreIds = new ArrayList<>();
    private boolean skip;

    public List<Integer> getGenreIds() { return genreIds; }
    public void setGenreIds(List<Integer> genreIds) { this.genreIds = genreIds; }
    public boolean isSkip() { return skip; }
    public void setSkip(boolean skip) { this.skip = skip; }
}
