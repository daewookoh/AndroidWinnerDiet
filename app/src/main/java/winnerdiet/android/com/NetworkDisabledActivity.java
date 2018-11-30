package winnerdiet.android.com;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * Created by godowondev on 2018. 5. 1..
 */

public class NetworkDisabledActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_disabled);
    }

    public void restartBtnClicked(View view) {
        Intent intent = new Intent(NetworkDisabledActivity.this, IntroActivity.class);
        startActivity(intent);
    }
}
