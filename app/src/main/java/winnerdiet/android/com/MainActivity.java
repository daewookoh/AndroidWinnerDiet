package winnerdiet.android.com;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;

import com.google.android.gms.fitness.Fitness;

public class MainActivity extends Activity {

    WebView webView;
    ProgressBar progressBar;
    SwipeRefreshLayout refreshLayout;
    public static Context mContext;
    Common common = new Common(this);

    private InterstitialAd frontAd;

    //카메라(이미지)업로드
    private final static int FCR = 1;
    private String mCM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;

    /*
    //만보기
    BroadcastReceiver receiver;
    String serviceData;
    Intent manboService;
    String step_record_date;
    */

    //구글피트니스
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 0x1001;

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

        MobileAds.initialize(this,
                getResources().getString(R.string.admob_id));

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

    /*
    //만보기
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
    */

    public void setWebview(final WebView webView)
    {
        WebSettings set = webView.getSettings();
        set.setJavaScriptEnabled(true);
        set.setLoadWithOverviewMode(true); // 한페이지에 전체화면이 다 들어가도록
        set.setJavaScriptCanOpenWindowsAutomatically(true);
        set.setSupportMultipleWindows(true); // <a>태그에서 target="_blank" 일 경우 외부 브라우저를 띄움
        set.setUserAgentString(webView.getSettings().getUserAgentString() + getResources().getString(R.string.user_agent));
        set.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW); // KCP 결제

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

                if(url.endsWith(getResources().getString(R.string.default_url)))
                {
                    refreshLayout.setEnabled(false);
                }
                else
                {
                    refreshLayout.setEnabled(true);
                }

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

                case "FRONT_AD" :

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (frontAd.isLoaded()) {
                                frontAd.show();
                            } else {
                                common.log("The front_ad wasn't loaded yet.");
                            }
                        }
                    });
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

        //카메라(이미지)업로드
        if (resultCode == Activity.RESULT_OK && requestCode == FCR)
        {
            Uri[] results = null;

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
            nickname = f_array[3];
            enc_id = f_array[6];
            profile_image = f_array[1];
            age = f_array[4];
            gender = f_array[5];
            email = f_array[2];
            name = f_array[3];
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

    /////////////////////////////////////////////////////////////////
    //구글피트니스 시작

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

        // 1일단위
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
                                        //common.log(sel_date);

                                        for (Field field : dp.getDataType().getFields()) {
                                            steps = dp.getValue(field).asInt();
                                            //common.log("STEP : " + String.valueOf(steps));

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


        /*
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
                        List<DataSet> dataSets = response.getDataSets();
                        for (DataSet dataSet : dataSets) {
                            for (DataPoint dp : dataSet.getDataPoints()) {
                                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                                String sel_date = df.format(dp.getStartTime(TimeUnit.MILLISECONDS));

                                for (Field field : dp.getDataType().getFields()) {
                                    steps = dp.getValue(field).asInt();
                                    common.log("STEP : " + sel_date + " : " + String.valueOf(steps));

                                    try {
                                        json.put(sel_date, String.valueOf(steps));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }

                        common.log(String.valueOf(json));

                    }
                });
        //기록단위 끝
        */

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
        //webView.reload(); //카메라 이미지 업로드시 리로드되는 상황이 발생하여 리로드처리하지 않음
    }

    public void loadFrontAd() {
        frontAd = new InterstitialAd(this);
        frontAd.setAdUnitId(getResources().getString(R.string.admob_front_ad));
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("301293198DDC43393B39932591A099C8")
                .build();
        frontAd.loadAd(adRequest);


        frontAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                common.log("front_ad_loaded");
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
}
