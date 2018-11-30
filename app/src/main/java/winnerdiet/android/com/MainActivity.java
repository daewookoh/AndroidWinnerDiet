package winnerdiet.android.com;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.facebook.share.widget.ShareDialog;
import com.google.firebase.iid.FirebaseInstanceId;
import com.kakao.auth.AuthType;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.response.model.UserProfile;
import com.kakao.util.exception.KakaoException;
import com.kakao.util.helper.log.Logger;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity {

    WebView webView;
    ProgressBar progressBar;
    SwipeRefreshLayout refreshLayout;
    public static Context mContext;
    Common common = new Common(this);

    //만보기
    BroadcastReceiver receiver;
    String serviceData;
    Intent manboService;
    String step_record_date;

    //네이버로그인
    public static OAuthLogin mOAuthLoginModule;
    private static OAuthLogin mOAuthLoginInstance;

    //카카오로그인
    SessionCallback callback;

    //sns로그인공용
    String login_success_yn = "N";
    String email = "";
    String nickname = "";
    String enc_id = "";
    String profile_image = "";
    String age = "";
    String gender = "";
    String id = "";
    String name = "";
    String birthday = "";

    //sns공유
    private ContentShare mContentShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        //만보기
        /*
        receiver = new PlayingReceiver();
        IntentFilter mainFilter = new IntentFilter("make.a.yong.manbo");
        manboService = new Intent(this, StepCheckService.class);
        registerReceiver(receiver, mainFilter);
        startService(manboService);
        */

        webView = (WebView) findViewById(R.id.webViewMain);
        progressBar = (ProgressBar) findViewById(R.id.progressBarMain);



        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshMain);

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //새로고침 소스
                webView.reload();
            }
        });

        // URL 세팅
        String sUrl = getIntent().getStringExtra("sUrl");
        if(sUrl==null) {
            sUrl = getResources().getString(R.string.default_url);
        }

        // 웹뷰 옵션세팅
        setWebview(webView);
        // 웹뷰 로드
        webView.loadUrl(sUrl);

        // 카카오로그인
        callback = new SessionCallback();
        Session.getCurrentSession().addCallback(callback);
    }

    class PlayingReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("PlayignReceiver", "IN");
            serviceData = intent.getStringExtra("stepService");
            common.log(serviceData);

            final Toast toast = Toast.makeText(getApplicationContext(), serviceData, Toast.LENGTH_SHORT);
            toast.show();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    toast.cancel();
                }
            }, 100);

        }
    }


    public void setWebview(final WebView webView)
    {
        WebSettings set = webView.getSettings();
        set.setJavaScriptEnabled(true);
        set.setLoadWithOverviewMode(true); // 한페이지에 전체화면이 다 들어가도록
        set.setJavaScriptCanOpenWindowsAutomatically(true);
        set.setSupportMultipleWindows(true); // <a>태그에서 target="_blank" 일 경우 외부 브라우저를 띄움
        set.setUserAgentString(getResources().getString(R.string.user_agent));

        webView.setWebViewClient(new WebViewClient() {

            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                //kcp 결제 처리
                if (url != null && (url.startsWith("vguardend:") )){
                    return false;
                }

                if (url != null && (url.startsWith("intent:") )) {
                    Log.e("1번 intent://" , url);
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        Intent existPackage = getPackageManager().getLaunchIntentForPackage(intent.getPackage());
                        if (existPackage != null) {
                            view.getContext().startActivity(intent);
                        } else {
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                            marketIntent.setData(Uri.parse("market://details?id="+intent.getPackage()));
                            view.getContext().startActivity(marketIntent);
                        }
                        return true;
                    }catch (Exception e) {
                        Log.e(TAG,e.getMessage());
                    }
                } else if (url != null && url.startsWith("market://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) {
                            view.getContext().startActivity(intent);
                        }
                        return true;
                    } catch (URISyntaxException e) {
                        Log.e(TAG,e.getMessage());
                    }
                }

                view.loadUrl(url);
                return true;
            }

            public void onPageStarted(WebView view, String url,
                                      android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                //refreshLayout.setRefreshing(true);
            }

            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.INVISIBLE);
                refreshLayout.setRefreshing(false);

                common.log(url.toString());
                if(url.endsWith(String.valueOf(R.string.default_url)))
                {
                    refreshLayout.setEnabled(false);
                }
                else
                {
                    refreshLayout.setEnabled(true);
                }
/*
                if(url.contains("/step.php"))
                {
                    try {
                        String step_data = makeStepData();
                        common.log(step_data);

                        String data = "act=setStepInfo&step_data="+step_data;
                        String enc_data = Base64.encodeToString(data.getBytes(), 0);
                        common.log("jsNativeToServer(enc_data) : step_data");
                        webView.loadUrl("javascript:jsNativeToServer('" + enc_data + "')");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
*/
                sendDeviceInfo();

            }

            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                if (message.startsWith("[ChangeDate]")) {
                    result.confirm();
                    //uiCallback.openDatePicker(webView);
                    return true;
                } else {
                    return false;
                }
            }
        });

        webView.addJavascriptInterface(new JavaScriptInterface(), getResources().getString(R.string.js_name));

    }
/*
    public String makeStepData() throws JSONException {
        JSONObject json = new JSONObject();

        int i=0;
        for(i=0; i<30; i++) {
            long now = System.currentTimeMillis() - (24*60*60*1000*i);
            Date date = new Date(now);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String sel_date = sdf.format(date);

            if(i==0) {
                if(StepCheckService.count>0) {
                    json.put(sel_date, StepCheckService.count);
                }
            }else{
                String a = common.getSP(sel_date);
                common.log(a);
                if(!a.isEmpty()) {
                    json.put(sel_date, a);
                }
            }
        }

        return json.toString();
    }
*/
    private class JavaScriptInterface {

        @JavascriptInterface
        public void appLogin(String data) {

            common.log("appLogin() -> " + data);

            switch (data) {
                case "NAVER":
                    loginNaver();
                    break;

                case "KAKAO":
                    loginKako();
                    break;

            }
        }

        //SNS 공유
        @JavascriptInterface
        public void contentShare(final String shareData) {
            common.log("contentShare() -> " + shareData);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mContentShare == null) mContentShare = new ContentShare();
                    try {
                        //페이스북 전용 dialog
                        ShareDialog shareDialog = new ShareDialog(MainActivity.this);
                        mContentShare.start(MainActivity.this, shareData, shareDialog);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

        }

        @JavascriptInterface
        public void downImage(String urlString) throws IOException {
            common.log("downImage() ->" + urlString);

            //URL url = new URL(urlString);
            // 권한을 부여받았는지 확인
            if (grantExternalStoragePermission()) {
                Bitmap bitmap = BitmapFactory.decodeStream((InputStream) new URL(urlString).getContent());
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "a", "description");
                Toast.makeText(getApplicationContext(), "이미지 다운로드 성공", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private boolean grantExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }else{
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                Toast.makeText(getApplicationContext(), "다운로드를 다시 시도해 주세요", Toast.LENGTH_LONG).show();
                return false;
            }
        }else{
            return true;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //카카오 로그인
    private void loginKako() {
        Session.getCurrentSession().open(AuthType.KAKAO_LOGIN_ALL,MainActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class SessionCallback implements ISessionCallback {

        @Override
        public void onSessionOpened() {
            requestMe();
        }

        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            if(exception != null) {
                Logger.e(exception);
            }
        }
    }

    private void requestMe() {

        UserManagement.requestMe(new MeResponseCallback() {
            @Override
            public void onFailure(ErrorResult errorResult) {
                common.log("kakao - onFailure" + errorResult);
            }

            @Override
            public void onSessionClosed(ErrorResult errorResult) {
                common.log("kakao - onSessionClosed" + errorResult);
            }

            @Override
            public void onSuccess(UserProfile userProfile) {
                common.log("onSuccess" + userProfile.toString());

                long userId = userProfile.getId();
                login_success_yn = "Y";
                id = String.valueOf(userId);
                gender = "";
                email = userProfile.getEmail();
                name = userProfile.getNickname();


                String sUrl = getResources().getString(R.string.sns_callback_url)
                        + "?login_type=kakao"
                        + "&success_yn=" + login_success_yn
                        + "&id=" + id
                        + "&gender=" + gender
                        + "&name=" + name
                        + "&email=" + email
                        ;
                common.log(sUrl);
                webView.loadUrl(sUrl);
                login_success_yn = "N";
            }

            @Override
            public void onNotSignedUp() {
                common.log("kakao - onNotSignedUp");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Session.getCurrentSession().removeCallback(callback);
    }
    //카카오 로그인 끝
    ///////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //네이버로그인
    private void loginNaver() {
        mOAuthLoginModule = OAuthLogin.getInstance();
        mOAuthLoginModule.init(this, getResources().getString(R.string.naver_client_id), getResources().getString(R.string.naver_client_secret), "clientName");
        mOAuthLoginModule.startOauthLoginActivity(MainActivity.this,mOAuthLoginHandler);

    }

    private OAuthLoginHandler mOAuthLoginHandler = new OAuthLoginHandler() {

        @Override
        public void run(boolean success) {
            if (success) {
                String accessToken = mOAuthLoginModule.getAccessToken(mContext);
                String refreshToken = mOAuthLoginModule.getRefreshToken(mContext);
                long expiresAt = mOAuthLoginModule.getExpiresAt(mContext);
                String tokenType = mOAuthLoginModule.getTokenType(mContext);

                new RequestApiTask().execute(); //로그인이 성공하면  네이버에 계정값들을 가져온다.

            } else {
                String errorCode = mOAuthLoginModule.getLastErrorCode(mContext).getCode();
                String errorDesc = mOAuthLoginModule.getLastErrorDesc(mContext);
                Toast.makeText(mContext, "errorCode:" + errorCode
                        + ", errorDesc:" + errorDesc, Toast.LENGTH_SHORT).show();
            }
        };
    };

    private class RequestApiTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            String url = "https://openapi.naver.com/v1/nid/getUserProfile.xml";
            String at = mOAuthLoginModule.getAccessToken(mContext);
            Pasingversiondata(mOAuthLoginModule.requestApi(mContext, at, url));
            return null;
        }

        protected void onPostExecute(Void content) {
            if (email == null) {
                Toast.makeText(MainActivity.this,
                        "로그인 실패하였습니다.  잠시후 다시 시도해 주세요!!", Toast.LENGTH_SHORT)
                        .show();

            } else {
                String sUrl = getResources().getString(R.string.sns_callback_url)
                        + "?login_type=naver"
                        + "&success_yn=" + login_success_yn
                        + "&id=" + id
                        + "&gender=" + gender
                        + "&name=" + name
                        + "&email=" + email
                        ;
                common.log(sUrl);
                webView.loadUrl(sUrl);
                login_success_yn = "N";
            }
        }

        private void Pasingversiondata(String data) {
            // xml 파싱
            String f_array[] = new String[9];

            try {
                XmlPullParserFactory parserCreator = XmlPullParserFactory
                        .newInstance();
                XmlPullParser parser = parserCreator.newPullParser();
                InputStream input = new ByteArrayInputStream(
                        data.getBytes("UTF-8"));
                parser.setInput(input, "UTF-8");

                int parserEvent = parser.getEventType();
                String tag;
                boolean inText = false;
                boolean lastMatTag = false;

                int colIdx = 0;

                while (parserEvent != XmlPullParser.END_DOCUMENT) {
                    switch (parserEvent) {
                        case XmlPullParser.START_TAG:
                            tag = parser.getName();
                            if (tag.compareTo("xml") == 0) {
                                inText = false;
                            } else if (tag.compareTo("data") == 0) {
                                inText = false;
                            } else if (tag.compareTo("result") == 0) {
                                inText = false;
                            } else if (tag.compareTo("resultcode") == 0) {
                                inText = false;
                            } else if (tag.compareTo("message") == 0) {
                                inText = false;
                            } else if (tag.compareTo("response") == 0) {
                                inText = false;
                            } else {
                                inText = true;
                            }

                            break;

                        case XmlPullParser.TEXT:
                            tag = parser.getName();
                            if (inText) {
                                if (parser.getText() == null) {
                                    f_array[colIdx] = "";
                                } else {
                                    f_array[colIdx] = parser.getText().trim();
                                }
                                colIdx++;
                            }
                            inText = false;
                            break;

                        case XmlPullParser.END_TAG:
                            tag = parser.getName();
                            inText = false;
                            break;
                    }
                    parserEvent = parser.next();
                }
            } catch (Exception e) {
                Log.e("dd", "Error in network call", e);
            }

            id = f_array[0];
            nickname = f_array[1];
            enc_id = f_array[2];
            profile_image = f_array[3];
            age = f_array[4];
            gender = f_array[5];
            email = f_array[6];
            name = f_array[7];
            birthday = f_array[8];


            common.log("email " + email);
            common.log("profile_image " + profile_image);
            common.log("gender " + gender);
            common.log("id " + id);
            common.log("name " + name);
            common.log("birthday " + birthday);
            common.log("enc_id " + enc_id);
            common.log("nickname " + nickname);
            common.log("age " + age);

            if(!name.isEmpty()) {
                login_success_yn = "Y";
            }
        }
    }
    //네이버로그인 끝
    /////////////////////////////////////////////////////////////////

    public void sendMms(String message) {
        Uri uri = Uri.parse("smsto:");
        Intent it = new Intent(Intent.ACTION_SENDTO, uri);
        it.putExtra("sms_body", message);
        startActivity(it);
    }

    @Override
    public void onBackPressed() {
        if(webView.getUrl().endsWith(String.valueOf(R.string.default_url)))
        {
            finish();
        }
        else if (webView.canGoBack()) {
            webView.goBack();
        }
        else {
            finish();
        }
    }

    public void sendDeviceInfo(){

        String device_id;
        String device_token;
        String device_model;
        String app_version;

        device_id = common.getSP("device_id");

        if(device_id.isEmpty())
        {
            String new_device_id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            String new_device_token = FirebaseInstanceId.getInstance().getToken();
            String new_device_model = Build.BRAND + "/" + Build.MODEL + "/" + Build.ID + "/" + Build.VERSION.RELEASE;
            String new_app_version = BuildConfig.VERSION_NAME;

            common.putSP("device_id", new_device_id);
            common.putSP("device_token", new_device_token);
            common.putSP("device_model", new_device_model);
            common.putSP("app_version", new_app_version);
        }

        device_id = common.getSP("device_id");
        device_token = common.getSP("device_token");
        device_model = common.getSP("device_model");
        app_version = common.getSP("app_version");


        String data = "act=setAppDeviceInfo&device_type=Android"
                + "&device_id="+device_id
                + "&device_token="+device_token
                +"&device_model="+device_model
                +"&app_version="+app_version;

        String enc_data = Base64.encodeToString(data.getBytes(), 0);

        common.log("jsNativeToServer(enc_data)");
        webView.loadUrl("javascript:jsNativeToServer('" + enc_data + "')");

        return;

    }

    @Override
    public void onResume() {
        super.onResume();
        webView.reload();
    }
}
