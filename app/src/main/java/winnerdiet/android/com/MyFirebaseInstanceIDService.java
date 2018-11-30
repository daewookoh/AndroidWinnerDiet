package winnerdiet.android.com;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.io.IOException;


public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = "TTT";
    Common common = new Common(this);

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        SharedPreferences put_pref = getSharedPreferences("shared_pref", MODE_PRIVATE);
        SharedPreferences.Editor put_editor = put_pref.edit();
        put_editor.putString("device_token", refreshedToken);
        put_editor.commit();

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        try {
            sendRegistrationToServer(refreshedToken);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // [END refresh_token]

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) throws IOException {
        // TODO: Implement this method to send token to your app server.

        String url = getResources().getString(R.string.api_url);

        String device_id =  common.getSP("device_id");
        String device_token =  common.getSP("device_token");
        String device_model =  common.getSP("device_model");
        String app_version =  common.getSP("app_version");

        ContentValues values = new ContentValues();
        values.put("action", "sendDeviceInfo");
        values.put("app_name", "dreamteams");
        values.put("device_type", "Android");
        values.put("device_id", device_id);
        values.put("device_token", device_token);
        values.put("device_model", device_model);
        values.put("app_version", app_version);

        HttpAsyncRequest httpAssyncRequest = new HttpAsyncRequest(url, values);
        httpAssyncRequest.execute();

    }

    // 비동기식 http 통신
    public class HttpAsyncRequest extends AsyncTask<Void, Void, String> {

        private String url;
        private ContentValues values;

        public HttpAsyncRequest(String url, ContentValues values) {

            common.log("비동기식 http 접속");
            this.url = url;
            this.values = values;
        }

        @Override
        protected String doInBackground(Void... params) {

            String result; // 요청 결과를 저장할 변수.
            //RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            //result = requestHttpURLConnection.request(url, values); // 해당 URL로 부터 결과물을 얻어온다.
            result = common.httpRequest(url, values);

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            common.log(s);
        }

    }
}
