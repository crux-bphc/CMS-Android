package crux.bphc.cms.helper;

import com.google.gson.annotations.SerializedName;

public class UserDetail {
    public String errorcode;
    private String username, firstname, lastname;
    @SerializedName("userpictureurl")
    private String userPictureUrl;
    private int userid;
    private String token;
    private String password;

    public UserDetail(String username, String firstname, String lastname, String userPictureUrl, String errorcode, int userid) {
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.userPictureUrl = userPictureUrl;
        this.errorcode = errorcode;
        this.userid = userid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getUserPictureUrl() {
        return userPictureUrl;
    }

    public int getUserid() {
        return userid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}