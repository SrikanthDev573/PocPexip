package webrtc.kp.com.pocvideocall;

import java.io.Serializable;

public class TokenGen implements Serializable{


    private String status;

    private Result result;

    public TokenGen(String status, Result result) {
        this.status = status;
        this.result = result;
    }

    public String getStatus() {
        return status;
    }

    public Result getResult() {
        return result;
    }
}
