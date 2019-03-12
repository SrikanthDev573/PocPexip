package webrtc.kp.com.pocvideocall;

import android.content.Context;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private String baseUrl;
    private String pin;
    private TokenGen tokenGen;
    private final static String SDP = "sdp";
    private SessionDescription mSDP;

    private static final String SIGNALING_URI = "https://csc2cvn00000017.cloud.kp.org";
    private static final String VIDEO_TRACK_ID = "PEXIPv0";
    private static final String AUDIO_TRACK_ID = "PEXIPa0";
    private static final String LOCAL_STREAM_ID = "stream1";
    private static final String SDP_MID = "sdpMid";
    private static final String SDP_M_LINE_INDEX = "sdpMLineIndex";
    private static final String CREATEOFFER = "createoffer";
    private static final String OFFER = "offer";
    private static final String ANSWER = "answer";
    private static final String CANDIDATE = "candidate";

    private PeerConnectionFactory peerConnectionFactory;
    private VideoSource localVideoSource;
    private PeerConnection peerConnection;
    private MediaStream localMediaStream;
    private VideoRenderer otherPeerRenderer;
    private boolean createOffer = false;
    private MediaConstraints mRtcConstraint;
    private String UUID;
    private String token;
    private String display_name;
    private String CALL_UUID;
    private Handler handler;
    private Runnable runnable;
    private boolean isReleaseToken = false;
    private SessionDescription mSDPAnswer;
    private GLSurfaceView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final EditText urlET = findViewById(R.id.url);
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                Log.d("Refresh Token", "Refreshing User tokens...");
                refreshToken();
            }
        };

        final EditText pinET = findViewById(R.id.pin);
        Button button = findViewById(R.id.videoCall);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                baseUrl = urlET.getText().toString().trim();
                pin = pinET.getText().toString();
                Log.d("url", baseUrl);
                isReleaseToken = true;
                initPeerConnection();
                callAuthAPI();

            }
        });

        findViewById(R.id.disconnectCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

        initPeerConnection();
    }

    private void initPeerConnection() {
        if (peerConnectionFactory != null) {
            return;
        }
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);

        PeerConnectionFactory.initializeAndroidGlobals(
                this,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true); // Render EGL Context

        peerConnectionFactory = new PeerConnectionFactory();

        VideoCapturerAndroid vc = VideoCapturerAndroid.create(CameraEnumerationAndroid.getNameOfFrontFacingDevice(), null);

        localVideoSource = peerConnectionFactory.createVideoSource(vc, new MediaConstraints());
        VideoTrack localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource);
        localVideoTrack.setEnabled(true);

        AudioSource audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        localAudioTrack.setEnabled(true);

        localMediaStream = peerConnectionFactory.createLocalMediaStream("PEXIP");
        localMediaStream.addTrack(localVideoTrack);
        localMediaStream.addTrack(localAudioTrack);

        videoView = (GLSurfaceView) findViewById(R.id.glview_call);

        try {
            VideoRendererGui.setView(videoView, null);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            ;
        }
        try {
            otherPeerRenderer = VideoRendererGui.createGui(0, 0, 100, 80, RendererCommon.ScalingType.SCALE_ASPECT_FILL, false);
            VideoRenderer renderer = VideoRendererGui.createGui(50, 80, 50, 20, RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);
            localVideoTrack.addRenderer(renderer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (peerConnection != null)
            return;

        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
//        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

        mRtcConstraint = new MediaConstraints();
        mRtcConstraint.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mRtcConstraint.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        peerConnection = peerConnectionFactory.createPeerConnection(
                iceServers,
                new MediaConstraints(),
                peerConnectionObserver);

        peerConnection.addStream(localMediaStream);
    }

    private void callAuthAPI() {
        String url = baseUrl + "/api/client/v2/conferences/meet.testroom/request_token?display_name=meet.testroom";
        Log.d("url", url);
        JSONObject body = new JSONObject();
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("Token", response.toString());
                String status = null;
                int duration = 0;
                try {
                    status = response.getString("status");
                    UUID = response.getJSONObject("result").getString("participant_uuid");
                    token = response.getJSONObject("result").getString("token");
                    display_name = response.getJSONObject("result").getString("display_name");
                    duration = response.getJSONObject("result").getInt("expires");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Result result = new Result(UUID, duration, display_name, token);
                tokenGen = new TokenGen(status, result);
                peerConnection.createOffer(sdpObserver, mRtcConstraint);
//                makeCall(tokenGen.getResult().getUuID(), mSDP.description);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Token", error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> param = new HashMap<>();
                param.put("pin", pin);
                param.put("Content-Type", "application/json");
                return param;
            }

        };
        VollyReq.getInstance(this).addToRequestQueue(request);
    }

    private void refreshToken() {
        String url = baseUrl + "/api/client/v2/conferences/meet.testroom/refresh_token?" + "pin="
                + pin + "&token=" + tokenGen.getResult().getToken();

        Log.d("url", url);
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("RefreshToken", response.toString());
                try {
                    tokenGen.getResult().setToken(response.getJSONObject("result").getString("token"));
                    tokenGen.getResult().setValidity(response.getJSONObject("result").getInt("expires"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
               //Multiply by 1000 to convert sec to millisec
               if (!isReleaseToken) {
                   int refreshTimer = tokenGen.getResult().getValidity() * 1000;
                   handler.postDelayed(runnable, refreshTimer);
               }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("RefreshToken", error.toString());
            }
        });
        VollyReq.getInstance(this).addToRequestQueue(request);
    }

    private void makeCall(String uuid, String description) {
        String url = baseUrl + "/api/client/v2/conferences/meet.testroom/participants/" + uuid + "/calls";
        Log.d("url", url);
        JSONObject body = new JSONObject();
        try {
            body.put("call_type", "WEBRTC");
            body.put("sdp", description);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("call", response.toString());
                try {
                    CALL_UUID = response.getJSONObject("result").getString("call_uuid");
                    String sdp = response.getJSONObject("result").getString("sdp");

                    mSDPAnswer = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
                    Log.e(TAG, "S Type in make call:" + mSDPAnswer.type.toString());
                    peerConnection.setRemoteDescription(sdpObserver, mSDPAnswer);
//                    peerConnection.createAnswer(sdpObserver,mRtcConstraint);eee
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //TODO: parse response once Event calls are done.

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("call", error.toString());
                Toast.makeText(getBaseContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> param = new HashMap<>();
                param.put("token", tokenGen.getResult().getToken());
                param.put("Content-Type", "application/json");
                return param;
            }

        };
        VollyReq.getInstance(this).addToRequestQueue(request);
    }

    private void doAck(String uuid, String callUuid) {
        String url = baseUrl + "/api/client/v2/conferences/meet.testroom/participants/" + uuid + "/calls/" + callUuid + "/ack";
        Log.d("url", url);
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("Ack", response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Ack", error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> param = new HashMap<>();
                param.put("token", tokenGen.getResult().getToken());
                param.put("Content-Type", "application/json");
                return param;
            }

        };
        VollyReq.getInstance(this).addToRequestQueue(request);
    }

    private void disconnect(String uuid, String callUuid) {
        String url = baseUrl + "/api/client/v2/conferences/meet.testroom/participants/" + uuid + "/disconnect";
        Log.d("url", url);
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("disconnect", response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("disconnect", error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> param = new HashMap<>();
                param.put("token", tokenGen.getResult().getToken());
                param.put("Content-Type", "application/json");
                return param;
            }
        };
        VollyReq.getInstance(this).addToRequestQueue(request);
    }

    private void releaseToken() {
        String url = baseUrl + "/api/client/v2/conferences/meet.testroom/release_token?" + "pin="
                + pin + "&token=" + tokenGen.getResult().getToken();

        Log.d("releaseUrl", url);
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                isReleaseToken = true;
                Log.d("releaseToken", response.toString());
                tokenGen = new TokenGen(null, null);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("releaseToken", error.toString());
            }
        });
        VollyReq.getInstance(this).addToRequestQueue(request);
    }


    private String TAG = "RTCAPP";
    SdpObserver sdpObserver = new SdpObserver() {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            mSDP = sessionDescription;
            Log.e(TAG, "S Type in observer:" + mSDP.type.toString());
            peerConnection.setLocalDescription(sdpObserver, sessionDescription);
//            peerConnection.createAnswer(sdpObserver,mRtcConstraint);
            makeCall(tokenGen.getResult().getUuID(), sessionDescription.description);
        }

        @Override
        public void onSetSuccess() {
            Log.e(TAG, "onSetSuccess: ");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.e(TAG, "onCreateFailure: " + s);
        }

        @Override
        public void onSetFailure(String s) {
//           makeCall(tokenGen.getResult().getUuID(),mSDP.description);
            peerConnection.createOffer(sdpObserver, mRtcConstraint);
            Log.e(TAG, "onSetFailure: " + s);
        }
    };

    PeerConnection.Observer peerConnectionObserver = new PeerConnection.Observer() {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d(TAG, "onSignalingChange:" + signalingState.toString());
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.e(TAG, "onIceConnectionChange:" + iceConnectionState.toString());
            switch (iceConnectionState) {
                case COMPLETED:
                    doAck(tokenGen.getResult().getUuID(), CALL_UUID);
                    break;
            }

        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.e(TAG, "onIceConnectionReceChange:" + b);
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.e(TAG, "onIceGathChange:" + iceGatheringState.toString());
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.e(TAG, "onIceCandidate:");
            peerConnection.addIceCandidate(iceCandidate);
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.e(TAG, "onAddStream:");
            mediaStream.videoTracks.getFirst().addRenderer(otherPeerRenderer);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.e(TAG, "onRemoveStream:");
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.e(TAG, "onDataChannel:");
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.e(TAG, "onRengo:");
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disconnect();
    }

    private void disconnect() {
        if (tokenGen.getResult() == null) {
            return;
        }
        try {
            peerConnection.removeStream(localMediaStream);
            localMediaStream = null;
            localVideoSource = null;
            peerConnection.close();
            peerConnection = null;
            otherPeerRenderer = null;
            peerConnectionFactory = null;
            mRtcConstraint = null;
            mSDP = null;
            mSDPAnswer = null;
            videoView = null;
            disconnect(UUID, CALL_UUID);
            releaseToken();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
