package com.yse.dev.DTO;
import lombok.Data;
@Data
public class CatalogFilter {
    private Integer genre,year,provider,startYear,endYear;
    private Double minRating;
    private String rating="";
    private String period;
    private String certification="";
    private String sort="";
    private String query="";
    private String tab="all";
    private String country="US";
    private int page=1;
    public void normalize(boolean countryPage){
        certification=certification==null?"":certification;
        tab=tab==null?"all":tab;
        sort=sort==null?"":sort;
        page=Math.max(1,Math.min(500,page));
        query=query==null?"":query.trim(); if(query.length()>100)query=query.substring(0,100);
        if(!java.util.List.of("all","topRated","upcoming").contains(tab==null?"":tab))tab="all";
        if(!java.util.List.of("latest","popular","recommended").contains(sort==null?"":sort))sort=countryPage?"latest":("topRated".equals(tab)?"recommended":"popular");
        if(!java.util.List.of("","ALL","12","15","19","UNKNOWN").contains(certification==null?"":certification))certification="";
        rating=rating==null?"":rating;
        country=country==null?"US":country;
        if(period!=null) {
            year=null;startYear=null;endYear=null;
            if("older".equals(period))endYear=1949;
            else if(java.util.List.of("2020","2010","2000","1990","1980","1970","1960","1950").contains(period)) {
                startYear=Integer.parseInt(period);endYear=startYear+9;
            } else period="";
        }
        int max=java.time.LocalDate.now().getYear()+5;
        if(year!=null&&(year<1870||year>max))year=null;
        if(year!=null && startYear==null && endYear==null){startYear=year;endYear=year;}
        if(startYear!=null&&(startYear<1870||startYear>max))startYear=null;
        if(endYear!=null&&(endYear<1870||endYear>max))endYear=null;
        if(startYear!=null&&endYear!=null&&startYear>endYear){int t=startYear;startYear=endYear;endYear=t;}
    }
}
