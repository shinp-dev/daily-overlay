package com.shinpstudio.dailyoverlay;

import java.util.*;

/** Reconstruct a single foreground app timeline. No Android dependencies. */
final class UsageEngine {
    static final int RESUME=1, PAUSE=2, SCREEN_ON=15, SCREEN_OFF=16, LOCK=17, UNLOCK=18, STOP=23, SHUTDOWN=26, STARTUP=27;
    record Event(long time, int type, String pkg, String activity) {}
    record Session(long start, long end, String pkg) { long duration(){return end-start;} }
    static List<Session> sessions(List<Event> events,long start,long end,Set<String> allowed) {
        List<Session> result=new ArrayList<>();
        String pkg=null, activity=null; boolean screen=true, unlocked=true; long cursor=start;
        for(Event e:events){
            if(e.time()>=end) break;
            long t=Math.max(start,e.time());
            if(t>cursor && screen && unlocked && pkg!=null && allowed.contains(pkg)) add(result,cursor,t,pkg);
            cursor=Math.max(cursor,t);
            switch(e.type()){
                case RESUME -> {pkg=e.pkg();activity=e.activity();}
                case PAUSE,STOP -> {if(Objects.equals(pkg,e.pkg()) && (e.activity()==null || Objects.equals(activity,e.activity()))){pkg=null;activity=null;}}
                case SCREEN_ON -> screen=true;
                case SCREEN_OFF -> screen=false;
                case LOCK -> unlocked=false;
                case UNLOCK -> unlocked=true;
                case SHUTDOWN,STARTUP -> {pkg=null;activity=null;screen=false;unlocked=false;}
            }
        }
        if(end>cursor && screen && unlocked && pkg!=null && allowed.contains(pkg)) add(result,cursor,end,pkg);
        return result;
    }
    private static void add(List<Session> r,long a,long b,String p){
        if(!r.isEmpty()){Session last=r.get(r.size()-1);if(last.end()==a && last.pkg().equals(p)){r.set(r.size()-1,new Session(last.start(),b,p));return;}}
        r.add(new Session(a,b,p));
    }
    static Map<String,Long> totals(List<Session> sessions){Map<String,Long> m=new HashMap<>(); for(Session s:sessions)m.merge(s.pkg(),s.duration(),Long::sum);return m;}
}
