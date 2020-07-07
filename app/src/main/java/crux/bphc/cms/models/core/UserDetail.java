package crux.bphc.cms.models.core;

import com.google.gson.annotations.SerializedName;

import crux.bphc.cms.network.MoodleServices;

/**
 * Model class to represent the response from
 * {@link MoodleServices#fetchUserDetail}
 *
 * @author Abhijeet Viswa
 */
public class UserDetail {
    @SerializedName("username") private String username;
    @SerializedName("firstname") private String firstName;
    @SerializedName("lastname") private String lastName;
    @SerializedName("userpictureurl") private String userPictureUrl;
    @SerializedName("userid") private int userId;

    private String token;

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUserPictureUrl() {
        return userPictureUrl;
    }

    public int getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}