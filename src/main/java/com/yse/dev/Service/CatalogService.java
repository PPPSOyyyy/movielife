package com.yse.dev.Service;
import com.yse.dev.DTO.CatalogFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.time.LocalDate;
@Service @RequiredArgsConstructor
public class CatalogService {
    /** 검색어와 이름이 맞는 인물 중 출연작을 함께 보여줄 최대 인원 */
    public static final int MAX_MATCHED_PEOPLE = 3;
    private final MovieService movies;
    public Map<String,Object> search(CatalogFilter f,String country) { return searchPage(f,country,f.getPage()); }
    public Map<String,Object> searchPage(CatalogFilter f,String country,int apiPage) {
        if(!f.getQuery().isBlank()) {
            Map<String,Object> p=new LinkedHashMap<>();
            p.put("query",f.getQuery());p.put("page",apiPage);p.put("include_adult",false);
            return movies.tmdb("/search/movie",p);
        }
        return movies.tmdb("/discover/movie",discoverParams(f,country,apiPage,null));
    }
    /** 배우·감독 이름 검색: 해당 인물들이 참여한 영화를 discover(with_people)로 조회합니다. 상세 필터도 함께 적용됩니다. */
    public Map<String,Object> peopleMoviesPage(CatalogFilter f,String country,List<Long> personIds,int apiPage) {
        if(personIds==null||personIds.isEmpty())return Map.of("results",List.of(),"total_pages",0,"total_results",0);
        StringJoiner ids=new StringJoiner("|");personIds.forEach(id->ids.add(String.valueOf(id)));
        Map<String,Object> p=discoverParams(f,country,apiPage,ids.toString());
        // 인물 검색 결과는 인기순이 자연스럽고, 추천순의 최소 투표 수 조건은 필모그래피를 지나치게 줄이므로 제외합니다.
        p.put("sort_by","popularity.desc");p.remove("vote_count.gte");
        return movies.tmdb("/discover/movie",p);
    }
    public Map<String,Object> searchPeople(String query,int apiPage) {
        Map<String,Object> p=new LinkedHashMap<>();
        p.put("query",query);p.put("page",apiPage);p.put("include_adult",false);
        return movies.tmdb("/search/person",p);
    }
    /** 인물 검색 결과에서 성인물 출연자를 제외한 목록 */
    public List<Map<?,?>> visiblePeople(Map<String,Object> personResult) {
        List<Map<?,?>> out=new ArrayList<>();
        if(personResult.get("results") instanceof List<?> l)for(Object o:l)
            if(o instanceof Map<?,?> m && !Boolean.TRUE.equals(m.get("adult")) && m.get("id") instanceof Number)out.add(m);
        return out;
    }
    /** 검색어와 이름(한글/원어)이 실제로 맞는 인물 ID를 인기순으로 최대 MAX_MATCHED_PEOPLE 명 고릅니다. */
    public List<Long> matchingPeople(String query,Map<String,Object> personResult) {
        String q=compact(query);
        if(q.isEmpty())return List.of();
        List<Map<?,?>> people=new ArrayList<>(visiblePeople(personResult));
        people.sort(Comparator.comparingDouble((Map<?,?> m)->m.get("popularity") instanceof Number n?n.doubleValue():0).reversed());
        List<Long> ids=new ArrayList<>();
        for(Map<?,?> m:people) {
            String name=compact(Objects.toString(m.get("name"),"")),original=compact(Objects.toString(m.get("original_name"),""));
            boolean match=(!name.isEmpty()&&(name.contains(q)||q.contains(name)))||(!original.isEmpty()&&(original.contains(q)||q.contains(original)));
            if(match&&m.get("id") instanceof Number n){ids.add(n.longValue());if(ids.size()>=MAX_MATCHED_PEOPLE)break;}
        }
        return ids;
    }
    private static String compact(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{Punct}]+","");}
    private Map<String,Object> discoverParams(CatalogFilter f,String country,int apiPage,String withPeople) {
        Map<String,Object> p=new LinkedHashMap<>();
        p.put("page",apiPage);p.put("include_adult",false);p.put("include_video",false);
        String sort=switch(f.getSort()){case "latest"->"primary_release_date.desc";case "recommended"->"popularity.desc";default->"popularity.desc";};

        p.put("sort_by",sort);
        if("recommended".equals(f.getSort()))p.put("vote_count.gte",30);
        if(country!=null)p.put("with_origin_country",country);
        if(withPeople!=null)p.put("with_people",withPeople);
        if(f.getGenre()!=null)p.put("with_genres",f.getGenre());
        String rating=f.getRating();
        if("under5".equals(rating)){p.put("vote_average.lte",4.999);p.putIfAbsent("vote_count.gte",1);}
        else if("five".equals(rating)){p.put("vote_average.gte",5);p.put("vote_average.lte",5.999);p.putIfAbsent("vote_count.gte",1);}
        else if(java.util.List.of("5","6","7","8","9").contains(rating))p.put("vote_average.gte",rating);
        else if(f.getMinRating()!=null&&f.getMinRating()>=0&&f.getMinRating()<=10)p.put("vote_average.gte",f.getMinRating());
        if(f.getYear()!=null)p.put("primary_release_year",f.getYear());
        if(f.getStartYear()!=null)p.put("primary_release_date.gte",f.getStartYear()+"-01-01");
        if(f.getEndYear()!=null)p.put("primary_release_date.lte",f.getEndYear()+"-12-31");
        if("latest".equals(f.getSort()) && !"upcoming".equals(f.getTab())) {
            String until=Objects.toString(p.get("primary_release_date.lte"),LocalDate.now().toString());
            p.put("primary_release_date.lte",until.compareTo(LocalDate.now().toString())>0?LocalDate.now():until);
        }
        if("upcoming".equals(f.getTab())) {p.put("region","KR");p.put("with_release_type","2|3");p.put("release_date.gte",LocalDate.now().plusDays(1));p.put("release_date.lte",LocalDate.now().plusMonths(6));}
        if(f.getProvider()!=null){p.put("watch_region","KR");p.put("with_watch_providers",f.getProvider());p.put("with_watch_monetization_types","flatrate|free|ads|rent|buy");}
        // exact rating verified again from KR release_dates, never inferred from adult flag
        if(java.util.List.of("ALL","12","15","19").contains(f.getCertification())) {
            String apiCode=movies.krCertificationValue(f.getCertification());
            if(apiCode!=null){p.put("certification_country","KR");p.put("19".equals(f.getCertification())?"certification.gte":"certification",apiCode);}
        }
        return p;
    }
}
