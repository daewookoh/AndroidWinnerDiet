package winnerdiet.android.com;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.facebook.FacebookSdk.getApplicationContext;

public class Receiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Common common = new Common(context);
        String step_device = common.getSP("step_device");

        String action= intent.getAction();

        if( action.equals(Intent.ACTION_BOOT_COMPLETED) && step_device.equals("app")){
            restartService();
        }

        if( action.equals(Intent.ACTION_MY_PACKAGE_REPLACED) && step_device.equals("app")){
            restartService();
        }

    }

    private void restartService()
    {
        PackageManager pm = getApplicationContext().getPackageManager();
        final boolean step_sensor_exist = pm.hasSystemFeature(PackageManager.
                FEATURE_SENSOR_STEP_DETECTOR
        );

        final boolean step_accelerometer_exist = pm.hasSystemFeature(PackageManager.
                FEATURE_SENSOR_ACCELEROMETER
        );

        if(step_sensor_exist)
        {
            Intent serviceIntent = new Intent(getApplicationContext(), StepCheckService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(getApplicationContext(), serviceIntent );
            }else{
                getApplicationContext().startService(serviceIntent);
            }
        }
    }

}

