package winnerdiet.android.com;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class StepCheckService extends Service implements SensorEventListener {

    Common common = new Common(this);

    public static int count = 0;
    //public static int count1 = 0;
    //public static int count2 = 0;
    public static int count_default = 0;

    NotificationCompat.Builder builder;

    private Sensor stepCountSensor;
    //private Sensor stepDetectSensor;
    private Sensor accelerometerSensor;
    private SensorManager sensorManager;

    @Override
    public void onCreate() {
        super.onCreate();
        common.log("onCreate-StepCheckService");
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        PackageManager pm = this.getPackageManager();
        final boolean step_sensor_exist = pm.hasSystemFeature(PackageManager.
                FEATURE_SENSOR_STEP_DETECTOR
        );

        if(!step_sensor_exist)
        {
            return;
        }


        // TYPE_STEP_COUNTER : 걸음수 표준
        // TYPE_STEP_DETECTOR : TYPE_STEP_COUNTER 보다 걸음수가 3%가량 적게나와서 사용안함
        // TYPE_ACCELEROMETER : 가속센서를 이용, 민감도를 커스터마이징 할수 있지만 조절 어렵고 배터리소모량 많아 사용안함
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        //stepDetectSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        //accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(this, stepCountSensor, SensorManager.SENSOR_DELAY_NORMAL);
        //sensorManager.registerListener(this, stepDetectSensor, SensorManager.SENSOR_DELAY_NORMAL);

        //common.putSP("step_count", "4344");

        String step_count = common.getSP("step_count");

        if(!step_count.isEmpty())
        {
            count = Integer.parseInt(step_count);
        }

        myStartForeground(step_count);
        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_DATE_CHANGED));

    }

    private final Receiver mReceiver = new Receiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if(action.equals(Intent.ACTION_DATE_CHANGED))
            {
                reportStep();
            }
        }
    };

    public void myStartForeground(String text){

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        String CHANNEL_ID = "StepCounter";
        String CHANNEL_NAME = "만보기";

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(null,null);
            channel.setShowBadge(false);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        }

        if(text.isEmpty())
        {
            text = "만보기 실행중";
        }
        else {
            //text = "오늘 걸음수 : " + text + "보";
            text = text + " 걸음";
        }

        common.log("text" + text);

        Bitmap icon = BitmapFactory.decodeResource(this.getResources(),
                R.mipmap.logo_round_512);
        builder.setSmallIcon(R.mipmap.noti_icon_60)
                .setLargeIcon(icon)
                //.setContent(remoteViews)
                .setContentTitle(text)
                .setContentText("오늘도 건강한 하루 되세요 ^^")
                //.setSound(null)
                //.setPriority(Notification.PRIORITY_MIN)
                .setContentIntent(pendingIntent);

        startForeground(2, builder.build());

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        //if (stepCountSensor != null) {
            sensorManager.registerListener(this, stepCountSensor, SensorManager.SENSOR_DELAY_NORMAL);
        //}
        /*
        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (stepDetectSensor != null) {
            sensorManager.registerListener(this, stepDetectSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        */
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            count = 0;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        common.log("onSensorchanged");

        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if(count_default==0) {

                String step_count = common.getSP("step_count");
                if(step_count.isEmpty()) {
                    step_count = "0";
                }
                //common.log("A"+step_count);
                count_default = (int)event.values[0] - Integer.parseInt(step_count);
                //common.log("B"+String.valueOf(count_default));
            }

            count = (int)event.values[0] - count_default;
            //common.log("C"+String.valueOf(count));
            reportStep();
        }

        /*
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            calculate(event.timestamp, event.values[0], event.values[1], event.values[2]);
        }

        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            count2++;
            //if(event.values[0] == 1.0f) {
            //reportStep();
            //}
        }
        */
    }

    private void reportStep(){

        //count++;


        //if(count%10==0) {
            long now = System.currentTimeMillis();
            Date date = new Date(now);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            //SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmm");
            String today = sdf.format(date);

            common.putSP("step_count", String.valueOf(count));
            String step_record_date =  common.getSP("step_record_date");

            if(!step_record_date.equals(today)){

                String lastday = common.getSP("step_record_date");
                common.putSP(lastday,String.valueOf(count));
                common.putSP("step_record_date", today);
                common.putSP("step_count", "0");
                count = 0;
                count_default=0;
            }
        //}

        common.log("오늘걸음수 : " + count);
        myStartForeground(Integer.toString(count));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void httpLog(String msg){


        String url = getResources().getString(R.string.api_url);
        ContentValues values = new ContentValues();
        values.put("action", "httpLog");
        values.put("msg", msg);

        //HttpAsyncRequest httpAssyncRequest = new HttpAsyncRequest(url, values);
        //httpAssyncRequest.execute();

    }

    // http 통신
    public String httpRequest(String _url, ContentValues _params){

        // HttpURLConnection 참조 변수.
        HttpURLConnection urlConn = null;
        // URL 뒤에 붙여서 보낼 파라미터.
        StringBuffer sbParams = new StringBuffer();

        /**
         * 1. StringBuffer에 파라미터 연결
         * */
        // 보낼 데이터가 없으면 파라미터를 비운다.
        if (_params == null)
            sbParams.append("");
            // 보낼 데이터가 있으면 파라미터를 채운다.
        else {
            // 파라미터가 2개 이상이면 파라미터 연결에 &가 필요하므로 스위칭할 변수 생성.
            boolean isAnd = false;
            // 파라미터 키와 값.
            String key;
            String value;

            for(Map.Entry<String, Object> parameter : _params.valueSet()){
                key = parameter.getKey();
                value = parameter.getValue().toString();

                // 파라미터가 두개 이상일때, 파라미터 사이에 &를 붙인다.
                if (isAnd)
                    sbParams.append("&");

                sbParams.append(key).append("=").append(value);

                // 파라미터가 2개 이상이면 isAnd를 true로 바꾸고 다음 루프부터 &를 붙인다.
                if (!isAnd)
                    if (_params.size() >= 2)
                        isAnd = true;
            }
        }

        /**
         * 2. HttpURLConnection을 통해 web의 데이터를 가져온다.
         * */
        try{
            URL url = new URL(_url);
            urlConn = (HttpURLConnection) url.openConnection();

            // [2-1]. urlConn 설정.
            urlConn.setRequestMethod("POST"); // URL 요청에 대한 메소드 설정 : POST.
            urlConn.setRequestProperty("Accept-Charset", "UTF-8"); // Accept-Charset 설정.
            urlConn.setRequestProperty("Context_Type", "application/x-www-form-urlencoded;cahrset=UTF-8");

            // [2-2]. parameter 전달 및 데이터 읽어오기.
            String strParams = sbParams.toString(); //sbParams에 정리한 파라미터들을 스트링으로 저장. 예)id=id1&pw=123;
            BufferedReader reader;
            String line;
            String page;
            try (OutputStream os = urlConn.getOutputStream()) {
                os.write(strParams.getBytes("UTF-8")); // 출력 스트림에 출력.
                os.flush(); // 출력 스트림을 플러시(비운다)하고 버퍼링 된 모든 출력 바이트를 강제 실행.
                os.close(); // 출력 스트림을 닫고 모든 시스템 자원을 해제.
            }

            // [2-3]. 연결 요청 확인.
            // 실패 시 null을 리턴하고 메서드를 종료.
            if (urlConn.getResponseCode() != HttpURLConnection.HTTP_OK)
                return null;

            // [2-4]. 읽어온 결과물 리턴.
            // 요청한 URL의 출력물을 BufferedReader로 받는다.
            reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "UTF-8"));

            // 출력물의 라인과 그 합에 대한 변수.
            page = "";

            // 라인을 받아와 합친다.
            while ((line = reader.readLine()) != null){
                page += line;
            }

            return page;

        } catch (MalformedURLException e) { // for URL.
            e.printStackTrace();
        } catch (IOException e) { // for openConnection().
            e.printStackTrace();
        } finally {
            if (urlConn != null)
                urlConn.disconnect();
        }

        return null;

    }

    // 비동기식 http 통신
    public class HttpAsyncRequest extends AsyncTask<Void, Void, String> {

        private String url;
        private ContentValues values;

        public HttpAsyncRequest(String url, ContentValues values) {

            this.url = url;
            this.values = values;
        }

        @Override
        protected String doInBackground(Void... params) {

            String result; // 요청 결과를 저장할 변수.
            //RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            //result = requestHttpURLConnection.request(url, values); // 해당 URL로 부터 결과물을 얻어온다.
            result = httpRequest(url, values);

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }

    }

}

