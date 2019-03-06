package webrtc.kp.com.pocvideocall;

public class Result {

    private String uuID;
    private int validity;
    private String displayNmae;
    private String token;

    public Result(String uuID, int validity, String displayNmae, String token) {
        this.uuID = uuID;
        this.validity = validity;
        this.displayNmae = displayNmae;
        this.token = token;
    }

    public String getUuID() {
        return uuID;
    }

    public int getValidity() {
        return validity;
    }

    public String getDisplayNmae() {
        return displayNmae;
    }

    public String getToken() {
        return token;
    }

    public void setValidity(int validity) {
        this.validity = validity;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
