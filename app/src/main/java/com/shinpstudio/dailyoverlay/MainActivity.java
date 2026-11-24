package com.shinpstudio.dailyoverlay;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout page; SharedPreferences prefs; TextView status;
    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onCreate(Bundle b){super.onCreate(b);prefs=UsageData.prefs(this); build();}
    @Override public void onResume(){super.onResume();if(status!=null)status.setText("重ねて表示: "+(Settings.canDrawOverlays(this)?"許可済み":"未許可")+" / 使用状況: "+(UsageData.permitted(this)?"許可済み":"未許可"));}
    void build(){
        ScrollView s=new ScrollView(this);page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(20),dp(30),dp(20),dp(30));s.addView(page);setContentView(s);s.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());s.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});s.requestApplyInsets();
        heading("Daily Overlay",26);text("小さな利用時間表示と、画面に貼れるメモ。データは端末内だけで処理します。");
        status=text("");button("重ねて表示の許可",()->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
        button("使用状況へのアクセスを許可",()->startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)button("通知を許可（固定解除・終了用）",()->requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7));
        heading("オーバーレイ",19);toggle("利用時間を表示","showUsage",true);toggle("メモを表示","showNote",true);
        button("オーバーレイ開始 / 設定反映",()->{if(!Settings.canDrawOverlays(this)){Toast.makeText(this,"先に重ねて表示を許可してください",Toast.LENGTH_SHORT).show();return;}if(!prefs.getBoolean("showUsage",true)&&!prefs.getBoolean("showNote",true)){stopService(new Intent(this,OverlayService.class));return;}startForegroundService(new Intent(this,OverlayService.class).setAction(OverlayService.REFRESH));});
        button("オーバーレイ終了",()->stopService(new Intent(this,OverlayService.class)));
        heading("利用時間",19);text("SNS・ゲーム以外はその他。ホーム画面とこのアプリは集計対象外。");slider("文字サイズ", "usageText",10,16,11);slider("上からの追加余白 (dp)","usageOffset",0,60,0);button("アプリの分類を変更",this::classify);
        heading("メモ",19);EditText edit=new EditText(this);edit.setGravity(Gravity.TOP);edit.setMinLines(3);edit.setMaxLines(8);edit.setText(prefs.getString("note",""));edit.setHint("ここにメモを書く");page.addView(edit);
        button("メモ保存",()->{prefs.edit().putString("note",edit.getText().toString()).apply();refreshIfRunning();Toast.makeText(this,"保存しました",Toast.LENGTH_SHORT).show();});
        toggle("ピン留め（背後へのタップ透過）","pinned",false);slider("透明度 (%)","noteAlpha",25,80,75);slider("メモの文字サイズ (sp)","noteText",12,26,16);slider("横幅 (dp)","noteWidth",100,340,180);slider("高さ (dp)","noteHeight",70,380,140);
        String[] names={"黄色","緑","青","白"};Spinner color=new Spinner(this);color.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));color.setSelection(prefs.getInt("noteColor",0));page.addView(color);color.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(prefs.getInt("noteColor",0)!=pos){prefs.edit().putInt("noteColor",pos).apply();refreshIfRunning();}}});
        button("メモ位置をリセット",()->{prefs.edit().remove("noteX").remove("noteY").apply();refreshIfRunning();});
        text("固定中は通知またはこの設定画面からピン解除できます。利用時間は概算で、OSが保持するイベントの範囲内で表示します。");
    }
    void refreshIfRunning(){if(OverlayService.running)startService(new Intent(this,OverlayService.class).setAction(OverlayService.REFRESH));}
    TextView text(String value){TextView t=new TextView(this);t.setText(value);t.setTextSize(14);t.setTextColor(Color.DKGRAY);t.setPadding(0,dp(5),0,dp(5));page.addView(t);return t;}
    void heading(String value,int size){TextView t=text(value);t.setTextSize(size);t.setTextColor(0xff234c43);t.setTypeface(null,1);t.setPadding(0,dp(16),0,dp(4));}
    void button(String label,Runnable r){Button b=new Button(this);b.setText(label);b.setAllCaps(false);page.addView(b);b.setOnClickListener(v->r.run());}
    void toggle(String label,String key,boolean def){Switch s=new Switch(this);s.setText(label);s.setChecked(prefs.getBoolean(key,def));page.addView(s);s.setOnCheckedChangeListener((b,c)->{prefs.edit().putBoolean(key,c).apply();refreshIfRunning();});}
    void slider(String label,String key,int min,int max,int def){TextView t=text(label+": "+prefs.getInt(key,def));SeekBar s=new SeekBar(this);s.setMax(max-min);s.setProgress(prefs.getInt(key,def)-min);page.addView(s);s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){t.setText(label+": "+(p+min));}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){prefs.edit().putInt(key,b.getProgress()+min).apply();refreshIfRunning();}});}
    void classify(){List<UsageData.App> apps=new ArrayList<>(UsageData.apps(this).values());apps.sort(Comparator.comparing(UsageData.App::label,String.CASE_INSENSITIVE_ORDER));LinearLayout list=new LinearLayout(this);list.setOrientation(1);list.setPadding(dp(12),0,dp(12),0);ScrollView sc=new ScrollView(this);sc.addView(list);Map<String,Spinner> choices=new HashMap<>();for(var app:apps){LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);TextView label=new TextView(this);label.setText(app.label());line.addView(label,new LinearLayout.LayoutParams(0,dp(48),1));Spinner sp=new Spinner(this);sp.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,UsageData.CATEGORIES));sp.setSelection(app.category());line.addView(sp,new LinearLayout.LayoutParams(dp(108),dp(48)));choices.put(app.pkg(),sp);list.addView(line);}new AlertDialog.Builder(this).setTitle("アプリの分類").setView(sc).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{var e=prefs.edit();choices.forEach((pkg,sp)->e.putInt("cat:"+pkg,sp.getSelectedItemPosition()));e.apply();refreshIfRunning();}).show();}
}
