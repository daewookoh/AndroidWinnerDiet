package winnerdiet.android.com;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Receiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Common common = new Common(context);
        String step_device = common.getSP("step_device");

        String action= intent.getAction();

        if( action.equals(Intent.ACTION_BOOT_COMPLETED) && step_device.equals("app")){
            Intent serviceIntent = new Intent(context, StepCheckService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent );
            }else{
                context.startService(serviceIntent);
            }
        }

        if( action.equals(Intent.ACTION_MY_PACKAGE_REPLACED) && step_device.equals("app")){
            Intent serviceIntent = new Intent(context, StepCheckService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent );
            }else{
                context.startService(serviceIntent);
            }
        }

    }

}

