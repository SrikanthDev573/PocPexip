package webrtc.kp.com.pocvideocall;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.pexip.android.wrapper.PexView;


public class VideoCallActivity extends AppCompatActivity {

    private PexView pexView;
    private static final String ONSETUP = "onSetup";
    private static final String ONCONNECT = "onConnect";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pexView = findViewById(R.id.pexView);
        initViewCallBack();
    }
    private void initViewCallBack() {
        pexView.setEvent(ONSETUP, pexView.new PexEvent() {
            @Override
            public void onEvent(String[] strings) {
                pexView.setSelfViewVideo(strings[0]);
                pexView.evaluateFunction("Connect");
            }
        });

        pexView.setEvent(ONCONNECT, pexView.new PexEvent() {
            @Override
            public void onEvent(String[] strings) {
                if (strings.length > 0 &&
                        strings[0] != null) {
                    pexView.setVideo(strings[0]);
                }
            }
        });

        pexView.addPageLoadedCallback(pexView.new PexCallback() {
            @Override
            public void callback(String s) {

            }
        });
        pexView.load();
    }
}
