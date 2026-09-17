package com.yse.dev.Service;
import java.util.*;
public final class KoreanCertification {
    private KoreanCertification() {}
    public static String normalize(String value) {
        return switch(value==null?"":value.trim().toUpperCase(Locale.ROOT)) {
            case "ALL", "0", "전체관람가" -> "ALL";
            case "12" -> "12"; case "15" -> "15";
            case "18", "19", "청소년관람불가", "청소년 관람불가" -> "19";
            case "RESTRICTED SCREENING", "제한상영가" -> "RESTRICTED";
            default -> "UNKNOWN";
        };
    }
    public static boolean adult(String code) {return "19".equals(code)||"RESTRICTED".equals(code);}
    public static int rank(String code) {return switch(code){case "ALL"->0;case "12"->12;case "15"->15;case "19"->19;case "RESTRICTED"->20;default->-1;};}
    public static String from(Object results) {
        String best="UNKNOWN";
        if(results instanceof List<?> regions) for(Object region:regions) {
            if(!(region instanceof Map<?,?> m) || !"KR".equals(m.get("iso_3166_1")))continue;
            if(m.get("release_dates") instanceof List<?> dates)for(Object date:dates) {
                if(!(date instanceof Map<?,?> d))continue;
                String code=normalize(Objects.toString(d.get("certification"),""));
                if(rank(code)>rank(best))best=code;
            }
        }
        return best;
    }
    public static String label(String code) {return switch(code){case "ALL"->"전체관람가";case "12"->"12세 이상";case "15"->"15세 이상";case "19"->"청소년 관람불가";case "RESTRICTED"->"제한상영가";default->"한국 관람등급 정보 없음";};}
}
