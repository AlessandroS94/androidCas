package org.apereo.cas.casAndroid.util;

import java.io.IOException;
import java.net.ConnectException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginRest {

    String user_id;
    String password;
    OkHttpClient client;

    public LoginRest(String user_id, String password, OkHttpClient client) {
        this.user_id = user_id;
        this.password = password;
        this.client = client;
    }

    public OkHttpClient getClient() {
        return client;
    }

    public void setClient(OkHttpClient client) {
        this.client = client;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String login(){

        String mEmail = this.user_id;
        String mPassword = this.password;
        OkHttpClient client = this.client;
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "token=true&username=" + mEmail + "&password=" + mPassword);
        Request request = new Request.Builder()
                .url("https://10.0.2.2:8443/cas/v1/tickets?token=true&username=" + mEmail + "&password=" + mPassword)
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.code() == 201)
            {
                return response.body().string();
            }
            else{
                return "credential_not_correct";
            }
        }
        catch (ConnectException e){
            return "server_not_found";
        }
        catch (IOException e) {
            e.printStackTrace();
            return "something_went_wrong";
        }
    }
}
