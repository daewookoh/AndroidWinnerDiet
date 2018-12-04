package winnerdiet.android.com;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.kakao.kakaolink.v2.KakaoLinkResponse;
import com.kakao.kakaolink.v2.KakaoLinkService;
import com.kakao.network.ErrorResult;
import com.kakao.network.callback.ResponseCallback;
import com.kakao.util.helper.log.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

//sns공유
public class ContentShare extends Activity {

    private Context mContext;
    private ShareDialog mShareDialog;
    Common common = new Common(MainActivity.mContext);

    public void start(Context context, String data, ShareDialog shareDialog) throws JSONException {
        mContext = context;
        mShareDialog = shareDialog;

        if (data == null) {
            return;
        }

        JSONObject jObject = null;
        jObject = new JSONObject(data);

        String share_type = jObject.getString("share_type");
        String img_url = jObject.getString("img_url");
        String link_url = jObject.getString("link_url");
        String title = jObject.getString("title");
        String content = jObject.getString("content");

        switch(share_type)
        {

            case "KAKAO" :
                shareKakaoTalk(title, content, link_url, img_url);
                break;

            case "FACEBOOK" :
                shareFB(link_url);
                break;

            case "KAKAOSTORY" :
                shareKakaoStory(title, content, link_url);
                break;

            case "BAND" :
                shareBand(title, content, link_url);
                break;

            case "LINE" :
                shareLine(title, content, link_url);
                break;

            case "MMS" :
                shareMms(title, content, link_url);
                break;

        }

    }

    //카카오톡 공유
    private void shareKakaoTalk(String title, String content, String link_url, String img_url) {
        common.log("shareKakaoTalk()");

        String templateId = "13622";
        Map<String, String> templateArgs = new HashMap<>();
        templateArgs.put("${title}", title.replace("\n","      "));
        templateArgs.put("${description}", content);
        templateArgs.put("${image_url}", img_url);

        KakaoLinkService.getInstance().sendCustom(mContext, templateId, templateArgs, new ResponseCallback<KakaoLinkResponse>(){
            @Override
            public void onFailure(ErrorResult errorResult) {
                Logger.e(errorResult.toString());
            }

            @Override
            public void onSuccess(KakaoLinkResponse result) {

            }
        });
    }

    //카카오스토리 공유
    private void shareKakaoStory(String title, String content, String link_url) {
        common.log("shareKakaoStory()");

        String kakaostoryPakageName = "com.kakao.story";

        if (getLaunchIntentForPackage(kakaostoryPakageName) != null) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            share.putExtra(Intent.EXTRA_TEXT, "\n\n"
                    + title +"\n\n"
                    + content + "\n\n"
                    + link_url);
            share.setPackage(kakaostoryPakageName);
            mContext.startActivity(share);
        }
    }

    //Fabebook 공유
    private void shareFB(String link_url) {
        common.log("shareFB()");

        if (ShareDialog.canShow(ShareLinkContent.class)) {
            ShareLinkContent linkContent = new ShareLinkContent.Builder()
                    .setContentUrl(Uri.parse(link_url))
                    .build();
            mShareDialog.show(linkContent);
        } else {
            common.log("--> Facebook ShareDialog can not show !");
        }
    }

    //Band 공유
    private void shareBand(String title, String content, String link_url) {
        common.log("shareBand");

        String bandPakageName = "com.nhn.android.band";

        Intent intent = getLaunchIntentForPackage(bandPakageName);

        if (intent != null) {
            //String shareText = mContentShareData.getTitle() + mContentShareData.getLinkUrl();
            //shareText = shareText.replace("\n", "");
            String shareText = title + "\n\n" + content + "\n\n" + link_url;

            try {
                shareText = URLEncoder.encode(shareText,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("bandapp://create/post?text=" + shareText));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mContext.startActivity(intent);
        }
    }

    //Line 공유
    private void shareLine(String title, String content, String link_url) {
        common.log("shareLine()");

        String linePakageName = "jp.naver.line.android";

        Intent intent = getLaunchIntentForPackage(linePakageName);

        if (intent != null) {
            String shareText = title + "\n\n" + content + "\n\n" + link_url;

            try {
                shareText = URLEncoder.encode(shareText,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //String shareText = mContentShareData.getTitle() + mContentShareData.getLinkUrl();
            //shareText = shareText.replace("\n", "");
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("line://msg/text/" + shareText));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mContext.startActivity(intent);
        }
    }


    //MMS 공유
    private void shareMms(String title, String content, String link_url) {
        common.log("shareMms()");

        String smsText = title + "\n\n" + content + "\n\n" + link_url;

        ((MainActivity)MainActivity.mContext).sendMms(smsText);
    }


    //해당 패키지명의 앱을 실행하기 위한 Intent 를 얻는다
    //설치되어있지 않다면 구글 플레이스토어에서 검색
    private Intent getLaunchIntentForPackage(String pakageName) {
        PackageManager packageManager = mContext.getPackageManager();
        Intent i = packageManager.getLaunchIntentForPackage(pakageName);

        if (i == null) {
            // 설치되어있지 않다면 구글 플레이스토어에서 검색
            Uri uri = Uri.parse("market://details?id=" + pakageName);
            i = new Intent(Intent.ACTION_VIEW, uri);

            try {
                mContext.startActivity(i);
            } catch (ActivityNotFoundException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        return i;
    }

}

