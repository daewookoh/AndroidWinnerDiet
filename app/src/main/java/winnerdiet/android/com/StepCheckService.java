package winnerdiet.android.com;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StepCheckService extends Service implements SensorEventListener {

    Common common = new Common(this);

    public static int count = 0;
    //public static int count1 = 0;
    //public static int count2 = 0;
    public static int count_default = 0;

    NotificationCompat.Builder builder;

    private Sensor stepCountSensor;
    //private Sensor stepDetectSensor;
    //private Sensor accelerometerSensor;
    private SensorManager sensorManager;

    private static final int ACCEL_RING_SIZE = 50;
    private static final int VEL_RING_SIZE = 10;

    // change this threshold according to your sensitivity preferences
    private static final float STEP_THRESHOLD = 50f;

    private static final int STEP_DELAY_NS = 11000000;

    private int accelRingCounter = 0;
    private float[] accelRingX = new float[ACCEL_RING_SIZE];
    private float[] accelRingY = new float[ACCEL_RING_SIZE];
    private float[] accelRingZ = new float[ACCEL_RING_SIZE];
    private int velRingCounter = 0;
    private float[] velRing = new float[VEL_RING_SIZE];
    private long lastStepTimeNs = 0;
    private float oldVelocityEstimate = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        common.log("onCreate-StepCheckService");
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // TYPE_STEP_COUNTER : 걸음수 표준
        // TYPE_STEP_DETECTOR : TYPE_STEP_COUNTER 보다 걸음수가 3%가량 적게나와서 사용안함
        // TYPE_ACCELEROMETER : 가속센서를 이용, 민감도를 커스터마이징 할수 있지만 조절 어렵고 배터리소모량 많아 사용안함
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        //stepDetectSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        //accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

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
                    NotificationManager.IMPORTANCE_NONE);
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

        if (stepCountSensor != null) {
            sensorManager.registerListener(this, stepCountSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
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

        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if(count_default==0) {

                String step_count = common.getSP("step_count");
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

    public void calculate(long timeNs, float x, float y, float z){
        float[] currentAccel = new float[3];
        currentAccel[0] = x;
        currentAccel[1] = y;
        currentAccel[2] = z;

        // First step is to update our guess of where the global z vector is.
        accelRingCounter++;
        accelRingX[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[0];
        accelRingY[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[1];
        accelRingZ[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[2];

        float[] worldZ = new float[3];
        worldZ[0] = this.sum(accelRingX) / Math.min(accelRingCounter, ACCEL_RING_SIZE);
        worldZ[1] = this.sum(accelRingY) / Math.min(accelRingCounter, ACCEL_RING_SIZE);
        worldZ[2] = this.sum(accelRingZ) / Math.min(accelRingCounter, ACCEL_RING_SIZE);

        float normalization_factor = this.norm(worldZ);

        worldZ[0] = worldZ[0] / normalization_factor;
        worldZ[1] = worldZ[1] / normalization_factor;
        worldZ[2] = worldZ[2] / normalization_factor;

        float currentZ = this.dot(worldZ, currentAccel) - normalization_factor;
        velRingCounter++;
        velRing[velRingCounter % VEL_RING_SIZE] = currentZ;

        float velocityEstimate = this.sum(velRing);

        if (velocityEstimate > 5) {
            //common.log("vel : " + velocityEstimate);
            //common.log("st  : " + Float.toString(STEP_THRESHOLD));
            //common.log("time: " + Long.toString(timeNs-lastStepTimeNs));
            //common.log("last: " + Long.toString(lastStepTimeNs));
            //common.log("DELA: " + Long.toString(STEP_DELAY_NS));
        }

        if (velocityEstimate > STEP_THRESHOLD
                && oldVelocityEstimate <= STEP_THRESHOLD
                && (timeNs - lastStepTimeNs > STEP_DELAY_NS)) {
            reportStep();
            lastStepTimeNs = timeNs;

        }

        oldVelocityEstimate = velocityEstimate;
    }

    public static float sum(float[] array) {
        float retval = 0;
        for (int i = 0; i < array.length; i++) {
            retval += array[i];
        }
        return retval;
    }

    public static float norm(float[] array) {
        float retval = 0;
        for (int i = 0; i < array.length; i++) {
            retval += array[i] * array[i];
        }
        return (float) Math.sqrt(retval);
    }


    public static float dot(float[] a, float[] b) {
        float retval = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        return retval;
    }
}

