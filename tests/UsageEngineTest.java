package com.shinpstudio.dailyoverlay;
import java.util.*;
import static com.shinpstudio.dailyoverlay.UsageEngine.*;
class UsageEngineTest {
    static Event e(long time,int type,String pkg,String activity){return new Event(time,type,pkg,activity);}
    static void check(String name,long expected,long actual){if(expected!=actual)throw new AssertionError(name+": expected "+expected+", got "+actual);System.out.println("PASS "+name);}
    static Map<String,Long> sum(List<Event> es,long from,long to){return totals(sessions(es,from,to,Set.of("a","b")));}
    public static void main(String[] args){
        check("midnight carry-in",30,sum(List.of(e(0,RESUME,"a","A"),e(130,PAUSE,"a","A")),100,200).getOrDefault("a",0L));
        check("screen off excluded",30,sum(List.of(e(0,RESUME,"a","A"),e(20,SCREEN_OFF,null,null),e(90,SCREEN_ON,null,null),e(100,PAUSE,"a","A")),0,110).getOrDefault("a",0L));
        check("keyguard excluded",30,sum(List.of(e(0,RESUME,"a","A"),e(20,LOCK,null,null),e(90,UNLOCK,null,null),e(100,PAUSE,"a","A")),0,110).getOrDefault("a",0L));
        var change=sum(List.of(e(0,RESUME,"a","A"),e(20,RESUME,"b","B"),e(22,PAUSE,"a","A")),0,100);
        check("app switch A",20,change.getOrDefault("a",0L));check("app switch B",80,change.getOrDefault("b",0L));
        check("same package activity switch",100,sum(List.of(e(0,RESUME,"a","A"),e(20,RESUME,"a","B"),e(22,STOP,"a","A")),0,100).getOrDefault("a",0L));
        check("home excluded",20,sum(List.of(e(0,RESUME,"a","A"),e(20,RESUME,"home","H")),0,100).getOrDefault("a",0L));
        check("shutdown excluded",20,sum(List.of(e(0,RESUME,"a","A"),e(20,SHUTDOWN,null,null)),0,100).getOrDefault("a",0L));
        check("no invented use",0,sum(List.of(),0,100).size());
        check("session merge",1,sessions(List.of(e(0,RESUME,"a","A"),e(30,RESUME,"a","B")),0,100,Set.of("a")).size());
    }
}
