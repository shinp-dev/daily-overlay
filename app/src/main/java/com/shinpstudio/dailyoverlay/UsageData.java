package com.shinpstudio.dailyoverlay;
import android.app.AppOpsManager;
import android.app.usage.*;
import android.content.*;
import android.content.pm.*;
import android.os.Process;
import java.time.*;
import java.util.*;

final class UsageData {
    static final String[] CATEGORIES={"SNS","ゲーム","その他"};
    record App(String pkg,String label,int category){}
    record Snapshot(List<UsageEngine.Session> sessions, Map<String,App> apps, boolean permitted){
        long[] categories(){long[] totals=new long[3];for(var s:sessions){App a=apps.get(s.pkg());if(a!=null)totals[a.category()]+=s.duration();}return totals;}
    }
    static boolean permitted(Context c){AppOpsManager a=c.getSystemService(AppOpsManager.class);return a.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,Process.myUid(),c.getPackageName())==AppOpsManager.MODE_ALLOWED;}
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("daily",0);}
    static Map<String,App> apps(Context c){
        PackageManager pm=c.getPackageManager();Set<String> homes=new HashSet<>();
        for(ResolveInfo r:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0))homes.add(r.activityInfo.packageName);
        Map<String,App> out=new HashMap<>();
        for(ResolveInfo r:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)){
            ApplicationInfo a=r.activityInfo.applicationInfo;String p=a.packageName;
            if(p.equals(c.getPackageName())||homes.contains(p))continue;
            String label=pm.getApplicationLabel(a).toString();String normalized=(p+" "+label).toLowerCase(Locale.ROOT);
            int def=a.category==ApplicationInfo.CATEGORY_GAME || normalized.matches(".*(othello|reversi|chess|shogi|囲碁|オセロ|将棋|リバーシ).*" )?1:
                a.category==ApplicationInfo.CATEGORY_SOCIAL || Set.of("com.twitter.android","jp.naver.line.android","com.instagram.android","com.facebook.katana","com.zhiliaoapp.musically","com.threads.barcelona","com.discord").contains(p)?0:2;
            if(normalized.contains("overlay"))def=2;
            int cat=prefs(c).getInt("cat:"+p,def);out.put(p,new App(p,label,Math.max(0,Math.min(2,cat))));
        }return out;
    }
    static Snapshot read(Context c,LocalDate day){
        Map<String,App> apps=apps(c); if(!permitted(c))return new Snapshot(List.of(),apps,false);
        ZoneId zone=ZoneId.systemDefault();long start=day.atStartOfDay(zone).toInstant().toEpochMilli();long end=Math.min(System.currentTimeMillis(),day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli());
        if(end<=start)return new Snapshot(List.of(),apps,true);
        UsageStatsManager manager=c.getSystemService(UsageStatsManager.class);List<UsageEngine.Event> events=new ArrayList<>();
        try{UsageEvents raw=manager.queryEvents(day.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),end);if(raw==null)return new Snapshot(List.of(),apps,false);
            UsageEvents.Event e=new UsageEvents.Event();while(raw.hasNextEvent()){raw.getNextEvent(e);events.add(new UsageEngine.Event(e.getTimeStamp(),e.getEventType(),e.getPackageName(),e.getClassName()));}
        }catch(SecurityException ex){return new Snapshot(List.of(),apps,false);}
        events.sort(Comparator.comparingLong(UsageEngine.Event::time));return new Snapshot(UsageEngine.sessions(events,start,end,apps.keySet()),apps,true);
    }
    static String shortTime(long ms){long m=ms/60000;return m>=60?(m/60)+"h"+(m%60)+"m":m+"m";}
    static String time(long ms){long m=ms/60000;return m>=60?(m/60)+"時間"+(m%60)+"分":m+"分";}
}
