package winnerdiet.android.com;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
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
import android.icu.util.TimeZone;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.kakao.auth.AuthType;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;
import com.kakao.util.exception.KakaoException;
import com.mocoplex.adlib.AdlibConfig;
import com.mocoplex.adlib.AdlibManager;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;
import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogRecord;

import static android.content.ContentValues.TAG;


public class MainActivity extends Activity implements RewardedVideoAdListener, SensorEventListener, IUnityAdsListener {

    WebView webView;
    ProgressBar progressBar;
    SwipeRefreshLayout refreshLayout;
    public static Context mContext;
    Common common = new Common(this);

    private InterstitialAd frontAd;
    private RewardedVideoAd rewardAd;

    Intent bluetoothService;

    //GPS
    private final int PERMISSIONS_ACCESS_FINE_LOCATION = 1000;
    private final int PERMISSIONS_ACCESS_COARSE_LOCATION = 1001;

    //카메라(이미지)업로드
    private final static int FCR = 1;
    private String mCM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;

    //만보기
    BroadcastReceiver receiver;
    String serviceData;
    Intent manboService;
    String step_device;

    Sensor stepCountSensor;
    SensorManager sensorManager;


    //구글피트니스
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 0x1001;

    //네이버로그인
    public static OAuthLogin mOAuthLoginModule;
    private static OAuthLogin mOAuthLoginInstance;

    //카카오로그인
    SessionCallback callback;

    private int last_increase_step;

    //sns로그인공용
    String email = "";
    String nickname = "";
    String enc_id = "";
    String profile_image = "";
    String age = "";
    String gender = "";
    String id = "";
    String name = "";
    String birthday = "";

    AdlibManager adlibManager;

    //sns공유
    private ContentShare mContentShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        MobileAds.initialize(this,
                getResources().getString(R.string.admob_id));

        UnityAds.initialize(this, getResources().getString(R.string.unity_id), this);

        adlibManager = new AdlibManager(getResources().getString(R.string.adlib_id));
        adlibManager.onCreate(this);
        // 테스트 광고 노출로, 상용일 경우 꼭 제거해야 합니다.
        //adlibManager.setAdlibTestMode(true);

        // 미디에이션 스케쥴 관련 설정
        bindPlatform();

        //common.putSP("step_device", "app");
        setGPS();
        startManboService();

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

        //Intent intent = new Intent(this, ScanBleActivity.class);
        //startActivity(intent);


        // Sensor
        if (stepCountSensor == null) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            sensorManager.registerListener(this, stepCountSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void bindPlatform() {
        // 광고 스케줄링 설정 - 전면, 띠 배너 동일
        // AdlibManager 생성 및 onCreate() 이후
        // 광고 요청 이전에 해당 스케쥴 관련 타 플랫폼 정보 등록
        // 첫번 째 AdlibManager 생성 시에 호출
        // 광고 subview 의 패키지 경로를 설정 (실제로 작성된 패키지 경로로 변경)

        // 쓰지 않을 광고 플랫폼은 삭제해주세요.
        /*
        AdlibConfig.getInstance().bindPlatform("ADMIXER", "winnerdiet.android.com.ads.SubAdlibAdViewAdmixer");
        AdlibConfig.getInstance().bindPlatform("ADAM", "winnerdiet.android.com.ads.SubAdlibAdViewAdam");
        AdlibConfig.getInstance().bindPlatform("ADMOB", "winnerdiet.android.com.ads.SubAdlibAdViewAdmob");
        AdlibConfig.getInstance().bindPlatform("AMAZON", "winnerdiet.android.com.ads.SubAdlibAdViewAmazon");
        AdlibConfig.getInstance().bindPlatform("MOBCLIX", "winnerdiet.android.com.ads.SubAdlibAdViewMobclix");
        AdlibConfig.getInstance().bindPlatform("CAULY", "winnerdiet.android.com.ads.SubAdlibAdViewCauly");
        AdlibConfig.getInstance().bindPlatform("FACEBOOK", "winnerdiet.android.com.ads.SubAdlibAdViewFacebook");
        AdlibConfig.getInstance().bindPlatform("INMOBI", "winnerdiet.android.com.ads.SubAdlibAdViewInmobi");
        AdlibConfig.getInstance().bindPlatform("MEZZO", "winnerdiet.android.com.ads.SubAdlibAdViewMezzo");
        AdlibConfig.getInstance().bindPlatform("MMEDIA", "winnerdiet.android.com.ads.SubAdlibAdViewMMedia");
        AdlibConfig.getInstance().bindPlatform("MOBFOX", "winnerdiet.android.com.ads.SubAdlibAdViewMobfox");
        AdlibConfig.getInstance().bindPlatform("MOPUB", "winnerdiet.android.com.ads.SubAdlibAdViewMopub");
        AdlibConfig.getInstance().bindPlatform("SHALLWEAD", "winnerdiet.android.com.ads.SubAdlibAdViewShallWeAd");
        AdlibConfig.getInstance().bindPlatform("TAD", "winnerdiet.android.com.ads.SubAdlibAdViewTAD");
        AdlibConfig.getInstance().bindPlatform("TNK", "winnerdiet.android.com.ads.SubAdlibAdViewTNK");
        */
    }

    //만보기
    public void startManboService(){

        step_device = common.getSP("step_device");
        if(TextUtils.isEmpty(step_device))
        {
            common.putSP("step_device", "app");
            step_device = "app";
        }

        if(step_device.equals("app")) {

            if(manboService==null) {
                IntentFilter mainFilter = new IntentFilter("manbo");
                manboService = new Intent(this, StepCheckService.class);
                registerReceiver(receiver, mainFilter);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(manboService);
            }else{
                startService(manboService);
            }

        }
    }

    public void stopManboService(){
        if(manboService != null){
            stopService(manboService);
        }
    }
    //만보기 끝


    public void setGPS() {

        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();

                //common.log("latitude: " + lat + ", longitude: " + lng);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                //common.log("onStatusChanged");
            }

            public void onProviderEnabled(String provider) {
                //common.log("onProviderEnabled");
            }

            public void onProviderDisabled(String provider) {
                //common.log("onProviderDisabled");
            }
        };

        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_ACCESS_FINE_LOCATION);

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED){

                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS_ACCESS_COARSE_LOCATION);
            }
        }
        else {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            // 수동으로 위치 구하기
            String locationProvider = LocationManager.GPS_PROVIDER;
            Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
            if (lastKnownLocation != null) {
                double lng = lastKnownLocation.getLongitude();
                double lat = lastKnownLocation.getLatitude();
                common.log("longtitude=" + lng + ", latitude=" + lat);
                common.putSP("longtitude", Double.toString(lng));
                common.putSP("latitude", Double.toString(lat));
            }
        }
    }

    public void setWebview(final WebView webView)
    {
        WebSettings set = webView.getSettings();
        set.setJavaScriptEnabled(true);
        set.setLoadWithOverviewMode(true); // 한페이지에 전체화면이 다 들어가도록
        set.setJavaScriptCanOpenWindowsAutomatically(true);
        set.setSupportMultipleWindows(true); // <a>태그에서 target="_blank" 일 경우 외부 브라우저를 띄움
        set.setUserAgentString(webView.getSettings().getUserAgentString() + getResources().getString(R.string.user_agent));
        set.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW); // KCP 결제
        set.setTextZoom(100);

        //KCP 결제용 쿠키처리
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {

            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                //kcp 결제 처리
                if (url != null && (url.startsWith("vguardend:") )){
                    return false;
                }

                if (url != null && (url.startsWith("intent:") || (url.startsWith("ahnlabv3mobileplus:")))) {
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
                refreshLayout.setEnabled(true);


                /*
                adlibManager.loadFullInterstitialAd(mContext, new Handler(){
                    public void handleMessage(Message message) {
                        try {
                            switch (message.what) {
                                case AdlibManager.DID_SUCCEED:
                                    Log.d("ADLIBr", "[Interstitial] onReceiveAd " + (String) message.obj);
                                    break;

                                // 전면배너 스케줄링 사용시, 각각의 플랫폼의 수신 실패 이벤트를 받습니다.
                                case AdlibManager.DID_ERROR:
                                    Log.d("ADLIBr", "[Interstitial] onFailedToReceiveAd " + (String) message.obj);
                                    break;

                                // 전면배너 스케줄로 설정되어있는 모든 플랫폼의 수신이 실패했을 경우 이벤트를 받습니다.
                                case AdlibManager.INTERSTITIAL_FAILED:
                                    Log.d("ADLIBr", "[Interstitial] All Failed.");
                                    break;

                                case AdlibManager.INTERSTITIAL_CLOSED:
                                    Log.d("ADLIBr", "[Interstitial] onClosedAd " + (String) message.obj);
                                    break;
                            }

                        } catch (Exception e) {
                        }
                    }
                });
*/

                /*
                adlibManager.requestInterstitial();
                common.log(String.valueOf(adlibManager.isLoadedInterstitial()));
                adlibManager.loadFullInterstitialAd(mContext);
*/

                /*
                if(url.endsWith(getResources().getString(R.string.default_url)))
                {
                    refreshLayout.setEnabled(false);
                }
                else
                {
                    refreshLayout.setEnabled(true);
                }
                */

                /*
                if(url.endsWith("/step.php"))
                {
                    checkGoogleFit();
                }
                else if(url.endsWith("/challenge.php"))
                {
                    loadFrontAd();
                }
                else if(url.endsWith("/index2.php"))
                {
                    common.log("index2");
                    loadFrontAd();
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

            //<a>태그에서 target="_blank" 일 경우 외부 브라우저를 띄우기 위해 필요한override
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg)
            {
                WebView newWebView = new WebView(MainActivity.this);
                WebSettings webSettings = newWebView.getSettings();
                webSettings.setJavaScriptEnabled(true);

                ((WebView.WebViewTransport)resultMsg.obj).setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }

            //For Android 5.0+
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams) {


                grantFileUploadPermission();

                if (mUMA != null) {
                    mUMA.onReceiveValue(null);
                }

                mUMA = filePathCallback;
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {

                    File photoFile = null;

                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCM);
                    } catch (IOException ex) {
                        Log.e(TAG, "Image file creation failed", ex);
                    }
                    if (photoFile != null) {
                        mCM = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");
                Intent[] intentArray;

                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }


                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, FCR);

                return true;
            }
        });

        webView.addJavascriptInterface(new JavaScriptInterface(), getResources().getString(R.string.js_name));

    }


    // Create an image file
    private File createImageFile() throws IOException {

        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar cal = Calendar.getInstance();
            Date now = new Date();
            cal.setTime(now);
            String today = sdf.format(cal.getTime());

            if(step_device.equals("app")) {
                common.log("updateStepWithDate" + String.valueOf(StepCheckService.count));
                webView.loadUrl("javascript:updateStepWithDate('" + StepCheckService.count + "','" + today + "');");
            }
            else {
                int cur_step = (int) event.values[0];

                if (last_increase_step == 0) {
                    last_increase_step = (int) event.values[0];
                }

                int amount = (int) event.values[0] - last_increase_step;

                //common.log(String.valueOf(amount));
                webView.loadUrl("javascript:increaseStepWithDate(" + amount + ",'" + today + "');");
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private class JavaScriptInterface {

        @JavascriptInterface
        public void appLogin(final String data) {

            common.log("appLogin() -> " + data);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (data) {

                        case "NAVER":
                            loginNaver();
                            break;

                        case "KAKAO":
                            loginKako();
                            break;

                        case "STEP_DATA" :

                            step_device = common.getSP("step_device");
                            if(TextUtils.isEmpty(step_device))
                            {
                                common.putSP("step_device", "app");
                                step_device = "app";
                            }

                            last_increase_step = 0;

                            if(common.getSP("step_device").equals("app"))
                            {
                                try {
                                    getStepsCountFromApp(21);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            else
                            {
                                checkGoogleFit();
                            }
                            break;

                        case "STEP_DATA_TODAY" :
                            if(common.getSP("step_device").equals("app"))
                            {
                                try {
                                    getStepsCountFromApp(1);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            else
                            {
                                checkGoogleFit();
                            }
                            break;

                        case "STEP_DEVICE_APP" :
                            common.putSP("step_device","app");
                            step_device = "app";
                            startManboService();
                            break;

                        case "STEP_DEVICE_GOOGLEFIT" :
                            common.putSP("step_device","googlefit");
                            step_device = "googlefit";
                            stopManboService();
                            break;

                        case "TEST_RING" :
                            playRing();
                            break;

                        case "REFRESH_UNABLE" :
                            refreshLayout.setEnabled(false);
                            break;

                        case "CHECK_GOOGLE_FIT_INSTALL" :
                            if(isInstallApp("com.google.android.apps.fitness")==false){
                                webView.post(new Runnable() {
                                    public void run() {
                                        webView.loadUrl("javascript:openGoogleInstall();");
                                    }
                                });
                            }
                            break;

                        case "CHECK_ADLIB_REWARD_LOADED" :

                            adlibManager.requestInterstitial(new Handler(){
                                public void handleMessage(Message message) {
                                    common.log(String.valueOf(message));

                                    try {
                                        switch (message.what) {
                                            case AdlibManager.DID_SUCCEED:
                                                Log.d("TTTADLIBr", "[Interstitial] onReceiveAd " + (String) message.obj);
                                                webView.loadUrl("javascript:rewardLoaded()");
                                                break;

                                            // 전면배너 스케줄링 사용시, 각각의 플랫폼의 수신 실패 이벤트를 받습니다.
                                            case AdlibManager.DID_ERROR:
                                                Log.d("TTTADLIBr", "[Interstitial] onFailedToReceiveAd " + (String) message.obj);
                                                break;

                                            // 전면배너 스케줄로 설정되어있는 모든 플랫폼의 수신이 실패했을 경우 이벤트를 받습니다.
                                            case AdlibManager.INTERSTITIAL_FAILED:
                                                Log.d("TTTADLIBr", "[Interstitial] All Failed.");
                                                break;

                                            case AdlibManager.INTERSTITIAL_CLOSED:
                                                Log.d("TTTADLIBr", "[Interstitial] onClosedAd " + (String) message.obj);
                                                break;
                                        }

                                    } catch (Exception e) {
                                    }

                                }
                            });
                            break;

                        case "SHOW_ADLIB_REWARD_AD" :
                            adlibManager.loadFullInterstitialAd(mContext, new Handler(){
                                public void handleMessage(Message message) {
                                    common.log(String.valueOf(message));

                                    try {
                                        switch (message.what) {
                                            case 2:
                                                webView.loadUrl("javascript:rewardComplete()");
                                                break;

                                            case AdlibManager.DID_SUCCEED:
                                                Log.d("TTTADLIBr", "[Interstitial] onReceiveAd " + (String) message.obj);
                                                webView.loadUrl("javascript:rewardLoaded()");
                                                break;

                                            // 전면배너 스케줄링 사용시, 각각의 플랫폼의 수신 실패 이벤트를 받습니다.
                                            case AdlibManager.DID_ERROR:
                                                Log.d("TTTADLIBr", "[Interstitial] onFailedToReceiveAd " + (String) message.obj);
                                                break;

                                            // 전면배너 스케줄로 설정되어있는 모든 플랫폼의 수신이 실패했을 경우 이벤트를 받습니다.
                                            case AdlibManager.INTERSTITIAL_FAILED:
                                                Log.d("TTTADLIBr", "[Interstitial] All Failed.");
                                                break;

                                            case AdlibManager.INTERSTITIAL_CLOSED:
                                                webView.loadUrl("javascript:rewardClosed()");
                                                Log.d("TTTADLIBr", "[Interstitial] onClosedAd " + (String) message.obj);
                                                break;
                                        }

                                    } catch (Exception e) {
                                    }

                                }
                            });
                            break;

                        case "SHOW_ADLIB_FRONT_AD" :
                            adlibManager.loadFullInterstitialAd(mContext);
                            break;

                        case "CHECK_UNITY_REWARD_LOADED" :
                            if(UnityAds.isReady("rewardedVideo")) {
                                webView.loadUrl("javascript:rewardLoaded()");
                            }
                            break;

                        case "SHOW_UNITY_REWARD_AD" :
                            if(UnityAds.isReady("rewardedVideo")) {
                                UnityAds.show(MainActivity.this, "rewardedVideo");
                            }
                            break;

                        case "SHOW_UNITY_FRONT_AD" :
                            if(UnityAds.isReady("Interstitial")) {
                                UnityAds.show(MainActivity.this, "Interstitial");
                            }
                            break;

                        case "CHECK_TIMEZONE" :
                            startManboService();
                            sendTimezoneInfo();
                            break;

                        case "RESET_TIMEZONE" :
                            common.putSP("timezone","");
                            sendTimezoneInfo();
                            break;

                        case "LOAD_FRONT_AD" :
                            loadFrontAd();
                            break;

                        case "LOAD_REWARD_AD" :
                            loadRewardAd();
                            break;

                        case "SHOW_REWARD_AD" :
                            if (rewardAd.isLoaded()) {
                                rewardAd.show();
                            }
                            break;

                        case "SHOW_FRONT_AD" :
                            if (frontAd.isLoaded()) {
                                frontAd.show();
                            } else {
                                common.log("The front_ad wasn't loaded yet.");
                            }
                            break;

                        default :
                            break;


                    }
                }
            });

        }

        public void playRing(){
            MediaPlayer mp;
            mp = MediaPlayer.create(MainActivity.this,R.raw.ring);
            mp.start();
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

    private boolean grantFileUploadPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }else{
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
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

        super.onActivityResult(requestCode, resultCode, data);

        //카카오
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            return;
        }

        //구글피트
        if (resultCode == Activity.RESULT_OK && requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            getStepsCount();
            return;
        }

        //카메라(이미지)업로드
        if (requestCode == FCR)
        {
            Uri[] results = null;

            if (resultCode == Activity.RESULT_OK) {
                if (null == mUMA) {
                    return;
                }
                if (data == null) {
                    //Capture Photo if no image available
                    if (mCM != null) {
                        results = new Uri[]{Uri.parse(mCM)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            mUMA.onReceiveValue(results);
            mUMA = null;
        }

    }

    private class SessionCallback implements ISessionCallback {

        @Override
        public void onSessionOpened() {
            requestMe();
        }

        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            if(exception != null) {
            }
        }
    }

    private void requestMe() {

        UserManagement.getInstance().me(new MeV2ResponseCallback() {
            @Override
            public void onSessionClosed(ErrorResult errorResult) {
                common.log("b");
            }

            @Override
            public void onSuccess(MeV2Response result) {
                common.log("onSuccess" + result.toString());

                long userId = result.getId();
                id = String.valueOf(userId);
                gender = "";
                email = "";//result.getKakaoAccount().getEmail();
                name = result.getNickname();
                profile_image = result.getProfileImagePath();


                String sUrl = getResources().getString(R.string.sns_callback_url)
                        + "?login_type=kakao"
                        + "&success_yn=Y"
                        + "&id=" + id
                        + "&gender=" + gender
                        + "&name=" + name
                        + "&email=" + email
                        + "&profile_image=" + profile_image
                        ;
                common.log(sUrl);
                webView.loadUrl(sUrl);
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
                final String accessToken = mOAuthLoginModule.getAccessToken(mContext);
                ProfileTask task = new ProfileTask();
                task.execute(accessToken);

            } else {
                String errorCode = mOAuthLoginModule.getLastErrorCode(mContext).getCode();
                String errorDesc = mOAuthLoginModule.getLastErrorDesc(mContext);
                Toast.makeText(mContext, "errorCode:" + errorCode
                        + ", errorDesc:" + errorDesc, Toast.LENGTH_SHORT).show();
            }
        };
    };

    class ProfileTask extends AsyncTask<String, Void, String> {
        String result;
        @Override
        protected String doInBackground(String... strings) {
            String token = strings[0];// 네이버 로그인 접근 토큰;
            String header = "Bearer " + token; // Bearer 다음에 공백 추가
            try {
                String apiURL = "https://openapi.naver.com/v1/nid/me";
                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("Authorization", header);
                int responseCode = con.getResponseCode();
                BufferedReader br;
                if(responseCode==200) { // 정상 호출
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else {  // 에러 발생
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                }
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                result = response.toString();
                br.close();
                System.out.println(response.toString());
            } catch (Exception e) {
                System.out.println(e);
            }
            //result 값은 JSONObject 형태로 넘어옵니다.
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                //넘어온 result 값을 JSONObject 로 변환해주고, 값을 가져오면 되는데요.
                // result 를 Log에 찍어보면 어떻게 가져와야할 지 감이 오실거에요.
                JSONObject object = new JSONObject(result);
                if(object.getString("resultcode").equals("00")) {
                    JSONObject jsonObject = new JSONObject(object.getString("response"));
                    //Log.d("jsonObject", jsonObject.toString());

                    String sUrl = getResources().getString(R.string.sns_callback_url)
                            + "?login_type=naver"
                            + "&success_yn=Y"
                            + "&id=" + jsonObject.getString("id");

                    if(jsonObject.has("name"))
                    {
                        sUrl += "&name=" + jsonObject.getString("name");
                    }

                    if(jsonObject.has("email"))
                    {
                        sUrl += "&email=" + jsonObject.getString("email");
                    }

                    if(jsonObject.has("profile_image")) {
                        sUrl += "&profile_image=" + jsonObject.getString("profile_image");
                    }

                    common.log(sUrl);
                    webView.loadUrl(sUrl);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //네이버로그인 끝
    /////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////
    //구글피트니스 시작
    private boolean isInstallApp(String pakageName){
        Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(pakageName);

        if(intent==null){
            //미설치
            return false;
        }else{
            //설치
            return true;
        }
    }

    public void checkGoogleFit() {

        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_ACTIVITY_SUMMARY, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                .build();

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions);
        } else {
            getStepsCount();
        }
    }

    public void getStepsCountFromApp(int days) throws JSONException {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        String today = sdf.format(cal.getTime());

        Calendar endCalendar = new GregorianCalendar();
        endCalendar.setTime(cal.getTime());

        //cal.add(Calendar.WEEK_OF_YEAR, -3);
        cal.add(Calendar.DAY_OF_YEAR, -days);
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(cal.getTime());

        JSONObject json = new JSONObject();

        while (calendar.before(endCalendar)) {

            calendar.add(Calendar.DATE, 1);
            Date cur = calendar.getTime();

            String curday = sdf.format(cur);

            if(curday.equals(today) && curday.equals(common.getSP("step_record_date")))
            {
                int steps_sum = StepCheckService.count;
                json.put(curday, String.valueOf(steps_sum));
            }
            else {
                String steps_sum = common.getSP(curday);
                if(TextUtils.isEmpty(steps_sum)) {
                    steps_sum = "0";
                }
                json.put(curday, steps_sum);
            }

        }

        common.log(String.valueOf(json));
        String data = "act=setStepInfo&step_data="+json.toString();
        String enc_data = Base64.encodeToString(data.getBytes(), 0);
        common.log("jsNativeToServer(enc_data) : step_data");
        webView.loadUrl("javascript:jsNativeToServer('" + enc_data + "')");
    }

    public void getStepsCount() {

        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);

        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        long endTime = cal.getTimeInMillis();

        cal.add(Calendar.WEEK_OF_YEAR, -3);
        //cal.add(Calendar.DAY_OF_YEAR, -2);
        long startTime = cal.getTimeInMillis();

/*
        // 1일단위  //오레오 이상 버젼에서 날짜라 하루씩 미뤄지는 현상 발견되어 사용안함
        Fitness.getHistoryClient(this,

                GoogleSignIn.getLastSignedInAccount(this))
                .readData(new DataReadRequest.Builder()
                        .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.TYPE_STEP_COUNT_DELTA)
                        .bucketByTime(1, TimeUnit.DAYS)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build())
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse response) {

                        JSONObject json = new JSONObject();
                        int steps = 0;
                        if (response.getBuckets().size() > 0) {
                            for (Bucket bucket : response.getBuckets()) {
                                List<DataSet> dataSets = bucket.getDataSets();
                                for (DataSet dataSet : dataSets) {
                                    for (DataPoint dp : dataSet.getDataPoints()) {
                                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                                        String sel_date = df.format(dp.getStartTime(TimeUnit.MILLISECONDS));
                                        common.log(sel_date);

                                        for (Field field : dp.getDataType().getFields()) {
                                            steps = dp.getValue(field).asInt();
                                            common.log("STEP : " + String.valueOf(steps));

                                            try {
                                                json.put(sel_date, String.valueOf(steps));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        common.log(String.valueOf(json));

                        String data = "act=setStepInfo&step_data="+json.toString();
                        String enc_data = Base64.encodeToString(data.getBytes(), 0);
                        common.log("jsNativeToServer(enc_data) : step_data");
                        webView.loadUrl("javascript:jsNativeToServer('" + enc_data + "')");

                    }
                });
        // 1일단위 끝
*/

        /*
        // 오늘하루만
        Fitness.getHistoryClient(this,
                GoogleSignIn.getLastSignedInAccount(this))
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(
                        new OnSuccessListener<DataSet>() {
                            @Override
                            public void onSuccess(DataSet dataSet) {
                                long total =
                                        dataSet.isEmpty()
                                                ? 0
                                                : dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                                common.log("Total steps: " + total);
                            }
                        });
        // 오늘하루만 끝
        */



/*
        // 1일단위(구글 피트니스 앱과 같은 데이터 가져오기-데이터 매칭 안되서 사용안함)
        DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder()
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED)
                .setStreamName("estimated_steps")
                .setAppPackageName("com.google.android.gms")
                .build();

        Fitness.getHistoryClient(this,

                GoogleSignIn.getLastSignedInAccount(this))
                .readData(new DataReadRequest.Builder()
                        .aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA)
                        .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                        .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                        .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                        .bucketByTime(1, TimeUnit.DAYS)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build())
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse response) {

                        JSONObject json = new JSONObject();
                        int steps = 0;
                        if (response.getBuckets().size() > 0) {
                            for (Bucket bucket : response.getBuckets()) {
                                List<DataSet> dataSets = bucket.getDataSets();
                                for (DataSet dataSet : dataSets) {
                                    for (DataPoint dp : dataSet.getDataPoints()) {
                                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                                        String sel_date = df.format(dp.getStartTime(TimeUnit.MILLISECONDS));
                                        //common.log(sel_date);

                                        for (Field field : dp.getDataType().getFields()) {
                                            //steps = dp.getValue(field).asInt();
                                            common.log(String.valueOf(field) + ":" + String.valueOf(dp.getValue(field)));

                                            try {
                                                json.put(sel_date, String.valueOf(steps));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        common.log(String.valueOf(json));

                        String data = "act=setStepInfo&step_data="+json.toString();
                        String enc_data = Base64.encodeToString(data.getBytes(), 0);
                        common.log("jsNativeToServer(enc_data) : step_data");
                        webView.loadUrl("javascript:jsNativeToServer('" + enc_data + "')");

                    }
                });
        // 1일단위(구글 피트니스 앱과 같은 데이터 가져오기) 끝
*/



        // 기록단위
        Fitness.getHistoryClient(this,
                GoogleSignIn.getLastSignedInAccount(this))
                .readData(new DataReadRequest.Builder()
                        .read(DataType.TYPE_STEP_COUNT_DELTA)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build())
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse response) {

                        JSONObject json = new JSONObject();

                        int steps = 0;
                        int steps_sum = 0;
                        String sel_date = "";
                        String last_date = "";

                        List<DataSet> dataSets = response.getDataSets();
                        for (DataSet dataSet : dataSets) {

                            for (DataPoint dp : dataSet.getDataPoints()) {
                                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                                sel_date = df.format(dp.getStartTime(TimeUnit.MILLISECONDS));

                                for (Field field : dp.getDataType().getFields()) {

                                    if(!String.valueOf(dp.getOriginalDataSource().getStreamName()).equals("user_input")) {

                                        if (!sel_date.equals(last_date)) {
                                            steps_sum = 0;
                                        }

                                        steps = dp.getValue(field).asInt();
                                        steps_sum += steps;
                                        //common.log("STEP : " + sel_date + last_date + " : " + String.valueOf(steps_sum));

                                        try {
                                            json.put(sel_date, String.valueOf(steps_sum));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        last_date = sel_date;
                                    }
                                }
                            }
                        }

                        common.log(String.valueOf(json));
                        String data = "act=setStepInfo&step_data="+json.toString();
                        String enc_data = Base64.encodeToString(data.getBytes(), 0);
                        common.log("jsNativeToServer(enc_data) : step_data");
                        webView.loadUrl("javascript:jsNativeToServer('" + enc_data + "')");

                    }
                });
        //기록단위 끝


    }
    //구글피트니스 끝
    /////////////////////////////////////////////////////////////////

    public void sendMms(String message) {
        Uri uri = Uri.parse("smsto:");
        Intent it = new Intent(Intent.ACTION_SENDTO, uri);
        it.putExtra("sms_body", message);
        startActivity(it);
    }

    @Override
    public void onBackPressed() {
        if(webView.getUrl().endsWith(getResources().getString(R.string.default_url)))
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
        String longtitude;
        String latitude;

        device_id = common.getSP("device_id");

        if(TextUtils.isEmpty(device_id))
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

        //앱버젼 변경시 업데이트
        String new_app_version = BuildConfig.VERSION_NAME;
        if(new_app_version!=app_version) {
            app_version = new_app_version;
            common.putSP("app_version", new_app_version);
        }

        setGPS();
        longtitude = common.getSP("longtitude");
        latitude = common.getSP("latitude");

        String data = "act=setAppDeviceInfo&device_type=Android"
                + "&device_id="+device_id
                + "&device_token="+device_token
                +"&device_model="+device_model
                +"&app_version="+app_version
                +"&longtitude="+longtitude
                +"&latitude="+latitude
                ;

        String enc_data = Base64.encodeToString(data.getBytes(), 0);

        common.log("jsNativeToServer(enc_data)");
        webView.loadUrl("javascript:jsNativeToServer('" + enc_data + "')");

        return;

    }

    public void sendTimezoneInfo(){

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            TimeZone tz = null;
            tz = TimeZone.getDefault();
            String timezone = tz.getID();

            String myTimezone = common.getSP("timezone");

            if(!TextUtils.isEmpty(timezone)) {

                if (TextUtils.isEmpty(myTimezone) || !myTimezone.trim().equals(timezone.trim())) {

                    common.putSP("timezone", timezone);

                    String country = "";

                    LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                    Geocoder geocoder = new Geocoder(getApplicationContext());
                    for(String provider: lm.getAllProviders()) {
                        @SuppressWarnings("ResourceType") Location location = lm.getLastKnownLocation(provider);
                        if(location!=null) {
                            try {
                                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                if(addresses != null && addresses.size() > 0) {
                                    country = addresses.get(0).getCountryName();
                                    break;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if(TextUtils.isEmpty(country)) {
                        country = timezone;
                    }

                    String data = "act=setTimezoneInfo"
                            + "&timezone=" + timezone
                            + "&country=" + country;

                    String enc_data = Base64.encodeToString(data.getBytes(), 0);

                    common.log("jsNativeToServer(enc_data)");
                    webView.loadUrl("javascript:jsNativeToServer('" + enc_data + "')");

                    /*
                    String data2 = "act=debug"
                            + "&memo=" + memo;
                    String enc_data2 = Base64.encodeToString(data2.getBytes(), 0);

                    common.log("jsNativeToServer(enc_data)");
                    webView.loadUrl("javascript:jsNativeToServer('" + enc_data2 + "')");
                    */
                }
            }
        }
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        //webView.reload(); //카메라 이미지 업로드시 리로드되는 상황이 발생하여 리로드처리하지 않음

        // Sensor
        if (stepCountSensor == null) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            sensorManager.registerListener(this, stepCountSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }else {
            sensorManager.registerListener(this, stepCountSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        // URL 세팅
        String sUrl = getIntent().getStringExtra("sUrl");
        if(sUrl!=null) {
            webView.loadUrl(sUrl);
        }

    }

    public void loadFrontAd() {
        frontAd = new InterstitialAd(this);
        frontAd.setAdUnitId(getResources().getString(R.string.admob_front_ad));
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("301293198DDC43393B39932591A099C8")
                .addTestDevice("4A98D900E8A438000FD4534B96A825EA")
                .build();
        frontAd.loadAd(adRequest);


        frontAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                common.log("front_ad_loaded");
                if (frontAd.isLoaded()) {
                    frontAd.show();
                }
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when the ad is displayed.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the interstitial ad is closed.
            }
        });
    }

    //애드몹(리워드광고)
    public void loadRewardAd() {
        common.log("loadRewardAd");
        rewardAd = MobileAds.getRewardedVideoAdInstance(this);
        rewardAd.setUserId(getResources().getString(R.string.admob_id));
        rewardAd.setRewardedVideoAdListener(this);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("301293198DDC43393B39932591A099C8")
                .addTestDevice("4A98D900E8A438000FD4534B96A825EA")
                .build();
        rewardAd.loadAd(getResources().getString(R.string.admob_reward_ad), adRequest);
    }

    @Override
    public void onRewardedVideoAdLoaded() {
        common.log("rewardAdLoaded");
        webView.loadUrl("javascript:rewardLoaded()");
    }

    @Override
    public void onRewardedVideoAdOpened() {
    }

    @Override
    public void onRewardedVideoStarted() {
    }

    @Override
    public void onRewardedVideoAdClosed() {

    }

    @Override
    public void onRewarded(RewardItem rewardItem) {
        common.log("onRewarded");
        webView.loadUrl("javascript:rewardComplete()");
    }

    @Override
    public void onRewardedVideoAdLeftApplication() {

    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int i) {

    }

    @Override
    public void onRewardedVideoCompleted() {

    }
    //애드몹(리워드광고 끝)


    //Unity Ads
    @Override
    public void onUnityAdsReady(String s) {
        common.log("onUnityAdsReady"+s);

    }

    @Override
    public void onUnityAdsStart(String s) {
        common.log("onUnityAdsStart"+s);
    }

    @Override
    public void onUnityAdsFinish(String s, UnityAds.FinishState finishState) {
        common.log("onUnityAdsFinish"+s);
        if(s.equals("rewardedVideo"))
        {
            webView.loadUrl("javascript:rewardComplete()");
        }
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String s) {
        common.log("onUnityAdsError"+s);
    }
    //Unity Ads 끝
}
