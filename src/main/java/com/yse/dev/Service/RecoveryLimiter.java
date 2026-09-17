package com.yse.dev.Service;
import org.springframework.stereotype.Component;
import java.util.*;
/** 단일 서버용 제한. 다중 인스턴스에서는 공유 저장소로 교체해야 합니다. */
@Component
public class RecoveryLimiter {
    private final Map<String,Window> windows=new HashMap<>();
    private record Window(long until,int count) {}
    public synchronized void check(String key,int max) {
        long now=System.currentTimeMillis();
        windows.entrySet().removeIf(e->e.getValue().until()<now);
        Window w=windows.get(key);
        if(w!=null && w.count()>=max) throw new IllegalArgumentException("요청이 너무 많습니다. 15분 후 다시 시도해 주세요.");
        if(w==null && windows.size()>=10000) throw new IllegalArgumentException("잠시 후 다시 시도해 주세요.");
        windows.put(key,new Window(w==null?now+900000:w.until(),w==null?1:w.count()+1));
    }
}
