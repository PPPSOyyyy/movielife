package com.yse.dev.Service;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.yse.dev.DTO.*;
import com.yse.dev.DTO.PersonDetailDto.PersonMovieDto;
@Service @RequiredArgsConstructor
public class PersonService {
    private final MovieService tmdb;
    public PersonDetailDto getPersonDetail(Long id){return getPersonDetail(id,1,"","");}
    public PersonDetailDto getPersonDetail(Long id,int page,String role,String certification){
        if(id==null||id<=0)throw new IllegalArgumentException("인물 정보가 올바르지 않습니다.");
        // 인물 기본정보 / 필모그래피 / 영어 이름을 병렬로 조회합니다.
        List<Supplier<Map<String,Object>>> tasks=List.of(
            ()->tmdb.tmdb("/person/"+id,Map.of()),
            ()->tmdb.tmdb("/person/"+id+"/movie_credits",Map.of()),
            // TMDB person detail does not promise original_name: retrieve English name explicitly.
            ()->{try {return tmdb.tmdb("/person/"+id,Map.of("language","en-US"));}
                 catch(org.springframework.web.client.RestClientException e){return Map.of();}}
        );
        List<Map<String,Object>> loaded=tmdb.parallel(tasks);
        Map<String,Object> p=loaded.get(0);
        Map<String,Object> credits=loaded.get(1);
        PersonDetailDto dto=new PersonDetailDto();dto.setId(id);
        dto.setName(text(p,"name"));dto.setProfilePath(text(p,"profile_path"));dto.setBiography(text(p,"biography"));
        dto.setBirthday(text(p,"birthday"));dto.setDeathday(text(p,"deathday"));dto.setPlaceOfBirth(text(p,"place_of_birth"));dto.setKnownForDepartment(text(p,"known_for_department"));
        if(!dto.getBiography().matches("(?s).*[가-힣].*"))dto.setBiography("");
        dto.setOriginalName(text(loaded.get(2),"name"));
        if(!List.of("cast","director").contains(role))role="Directing".equals(dto.getKnownForDepartment())?"director":"cast";
        dto.setRole(role);certification="19".equals(certification)?"19":"";dto.setCertification(certification);
        Map<Long,Map<?,?>> unique=new LinkedHashMap<>();
        Object value=credits.get("director".equals(role)?"crew":"cast");
        if(value instanceof List<?> list)for(Object o:list)if(o instanceof Map<?,?> m && m.get("id") instanceof Number n){
            if("director".equals(role)&&!"Director".equals(m.get("job")))continue;
            // 성인 콘텐츠(TMDB adult)는 필모그래피와 페이지 수 계산에서 제외합니다.
            if(Boolean.TRUE.equals(m.get("adult")))continue;
            unique.putIfAbsent(n.longValue(),m);
        }
        List<Map<?,?>> works=new ArrayList<>(unique.values());
        works.sort(Comparator.comparing((Map<?,?> m)->text(m,"release_date")).reversed());
        int pages=Math.max(1,(works.size()+23)/24);page=Math.max(1,Math.min(page,pages));dto.setPage(page);dto.setTotalPages(pages);
        List<Map<?,?>> chunk=works.subList(Math.min(works.size(),(page-1)*24),Math.min(works.size(),page*24));
        List<MovieDto> visible=tmdb.convertToMovieList(Map.of("results",chunk),certification);
        List<PersonMovieDto> output=new ArrayList<>();
        for(MovieDto movie:visible){
            Map<?,?> source=unique.get(movie.getId());PersonMovieDto work=new PersonMovieDto();
            work.setId(movie.getId());work.setTitle(movie.getTitle());work.setPosterPath(movie.getPosterPath());work.setReleaseDate(movie.getReleaseDate());work.setVoteAverage(movie.getVoteAverage());work.setAdultsOnly(movie.isAdultsOnly());
            work.setCharacter("director".equals(role)?"감독":text(source,"character"));output.add(work);
        }
        dto.setMovies(output);return dto;
    }
    private static String text(Map<?,?> m,String key){return Objects.toString(m.get(key),"");}
}
