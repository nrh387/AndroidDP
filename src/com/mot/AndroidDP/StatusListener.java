package com.mot.AndroidDP;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by bkmr38 on 6/14/2016.
 */
public class StatusListener extends Service {
    NotificationManager notifer;
    Intent statusChanged =null;
    Timer timer = null;
    @Override
    public void onCreate()
    {
        Log.i("StatusListener","onCreate");
        super.onCreate();
        notifer = (NotificationManager) getSystemService(Activity.NOTIFICATION_SERVICE);
        statusChanged=new Intent();
        timer=new Timer();
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        Log.i("StatusListener","onStart");
        super.onStart(intent, startId);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //发送广播
                ReceiveData();
            }
        }, 1000,3000);

    }

    @Override
    public void onDestroy()
    {
        Log.i("StatusListener","onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private String GetLastTime(){
        SharedPreferences sp = getSharedPreferences("dp", Context.MODE_PRIVATE);
        String time = sp.getString("QueryTime","");
        if(time.isEmpty()){
            time ="2016-06-12 18:50:32";
        }
        return time;
    }

    private void StoreLastTime(String time){
        SharedPreferences sp = getSharedPreferences("dp",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=sp.edit();
        editor.putString("QueryTime",time);
        editor.commit();
    }
    private String GetCurrentTime(){
        SimpleDateFormat formatter    =  new   SimpleDateFormat    ("yyyy-MM-dd HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        String str  = formatter.format(curDate);
        return str;
    }
    private void ReceiveData(){
        String time = GetLastTime();
        String currenttime=GetCurrentTime();
        List<StatusData> dataList = HttpUtility.QueryDPStatus(time);
        if(dataList.size()>0) {
            StoreLastTime(currenttime);
            SQLHelper sqlHeler = new SQLHelper(getApplicationContext());
            for(StatusData data : dataList) {

                sqlHeler.insert(data.DPName,data.Status,data.Time,data.Type);

                if(data.Type.equals("Warning")) {
                    SendNotification(data.DPName+":"+data.Status);
                }
            }

            SendStatusChangedBroadcast();
        }
    }

    private void SendStatusChangedBroadcast(){
        Log.i("StatusListener","SendStatusChangedBroadcast");
        statusChanged.setAction(GlobalPara.Status_Changed_Action);
        sendBroadcast(statusChanged);
    }

    private void SendNotification(String msg){
        Notification no = new Notification(R.drawable.ic_launcher,msg,System.currentTimeMillis());
        no.defaults=Notification.DEFAULT_ALL;
        Intent intent=new Intent(this,FragementStatus.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        no.setLatestEventInfo(this,"AndroidDP",msg,pendingIntent);
        notifer.notify("Testinfo",R.drawable.ic_launcher,no);
    }


}
