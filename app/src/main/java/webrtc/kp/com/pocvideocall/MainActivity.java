package webrtc.kp.com.pocvideocall;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private String baseUrl;
    private String pin;
    private TokenGen tokenGen;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EditText urlET = findViewById(R.id.url);
        baseUrl = urlET.getText().toString().trim();
        EditText pinET = findViewById(R.id.pin);
        pin = pinET.getText().toString();
        Button button = findViewById(R.id.videoCall);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callAuthAPI();


            }
        });

    }
    private void callAuthAPI() {
        String url = "https://csc2cvn00000017.cloud.kp.org/api/client/v2/conferences/meet.testroom/request_token?display_name=meet.testroom";
        JSONObject body = new JSONObject();
        /*try {
            body.put("display_name", "meet.testroom");
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("Token", response.toString());
                String status = null;
                String UUID = null, token = null, display_name = null;
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
                Result result = new Result(UUID,duration,display_name,token);
                tokenGen = new TokenGen(status, result);
                refreshToken();
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
                param.put("pin", "1234");
                param.put("Content-Type", "application/json");
                return param;
            }

        };
        VollyReq.getInstance(this).addToRequestQueue(request);
    }

    private void refreshToken(){
        String url = "https://csc2cvn00000017.cloud.kp.org/api/client/v2/conferences/meet.testroom/refresh_token?"+"pin="
                +tokenGen.getResult().getValidity()+"&token="+tokenGen.getResult().getToken();

        Log.d("RefreshURL", url);
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

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("RefreshToken", error.toString());
            }
        });
        VollyReq.getInstance(this).addToRequestQueue(request);


    }
}
