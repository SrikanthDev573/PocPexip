package webrtc.kp.com.pocvideocall;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TokenRequest {

    public static void getTokenRequest(String base_url, final String pin, Context context){
        String url;
        url = "https://csc2cvn00000017.cloud.kp.org"+"/api/client/v2/conferences/meet.testroom/request_token";

        JSONObject body = new JSONObject();
        try {
            body.put("display_name","meet.testroom");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url,body, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("Token",response.toString());
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
                new TokenGen(status, result);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("token", error.toString());

            }
        }){
           @Override
           public Map<String, String> getHeaders(){
               Map<String, String> param = new HashMap<>();
               param.put("pin",pin);
               return param;
           }

        };
        VollyReq.getInstance(context).addToRequestQueue(request);
    }
}
