package org.apereo.cas.casAndroid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import java.security.cert.CertificateException;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.apereo.cas.casAndroid.util.WebSocket;
import org.json.JSONException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //Button
    private FloatingActionButton buttonScan;
    private Button buttonLogin;
    private Button buttonLogout;
    private EditText email;
    private EditText password;

    //qr code scanner object
    private IntentIntegrator qrScan;
    private SharedPreferences sharedpreferences;

    //Session
    public static final String MyPREFERENCES = "Auth";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //OKhttp policy
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //View objects
        buttonScan = (FloatingActionButton) findViewById(R.id.scanQR);
        buttonLogin = (Button) findViewById(R.id.loginbutton);
        buttonLogout = (Button) findViewById(R.id.logout);
        email = (EditText) findViewById(R.id.etemail);
        password = (EditText) findViewById(R.id.mypass);

        //intializing scan object
        qrScan = new IntentIntegrator(this);

        //attaching onclick listener
        buttonScan.setOnClickListener(this);
        buttonLogin.setOnClickListener(this);
        buttonLogout.setOnClickListener(this);
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        updateUI();

    }


    //QR CODE READER
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {

            //if qrcode has nothing in it
            if (result.getContents() == null) {
                Toast.makeText(this, "QR Code Channel Not Found", Toast.LENGTH_LONG).show();
            } else {
                //if qr contains data send message to WebSocket
                Toast.makeText(this, result.getContents(), Toast.LENGTH_LONG).show();
                WebSocket channelAuth = new WebSocket(getUnsafeOkHttpClient());
                SharedPreferences sharedpreferences = getSharedPreferences(MainActivity.MyPREFERENCES, Context.MODE_PRIVATE);
                String value = sharedpreferences.getString(String.valueOf(User.JWT), null);
                try {
                    channelAuth.send(value,result.getContents());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    //Validate Input
    boolean isEmpty(EditText text) {
        CharSequence str = text.getText().toString();
        return TextUtils.isEmpty(str);
    }

    void checkDataEntered() {

        if (isEmpty(password)) {
            Toast t = Toast.makeText(this, "Password is required!", Toast.LENGTH_SHORT);
            t.show();
        }

        if (isEmpty(email)) {
            Toast t = Toast.makeText(this, "Insert a valid email!", Toast.LENGTH_SHORT);
            t.show();
        }

    }

    //Click listener
    @Override
    public void onClick(View view) {
        //initiating the qr code scan
        switch (view.getId()) {
            case R.id.scanQR:
                qrScan.initiateScan();
                break;
            case R.id.loginbutton:

                this.checkDataEntered();
                String mEmail = email.getText().toString();
                String mPassword = password.getText().toString();
                OkHttpClient client = getUnsafeOkHttpClient();
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
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putString(String.valueOf(User.JWT), response.body().string());
                        editor.commit();
                        updateUI();
                    }
                    else{
                        Toast t = Toast.makeText(this, "Email or password is not right!", Toast.LENGTH_SHORT);
                        t.show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.logout:
                logout();
                updateUI();
                break;

        }
    }

    //Update View
    private void updateUI() {
        SharedPreferences sharedpreferences = getSharedPreferences(MainActivity.MyPREFERENCES, Context.MODE_PRIVATE);
        String value = sharedpreferences.getString(String.valueOf(User.JWT), null);
        if (value != null) {
            email.setVisibility(View.GONE);
            password.setVisibility(View.GONE);
            buttonLogin.setVisibility(View.GONE);
            buttonLogout.setVisibility(View.VISIBLE);
            buttonScan.setVisibility(View.VISIBLE);
        } else {
            email.setVisibility(View.VISIBLE);
            password.setVisibility(View.VISIBLE);
            buttonLogin.setVisibility(View.VISIBLE);
            buttonLogout.setVisibility(View.GONE);
            buttonScan.setVisibility(View.GONE);
        }
    }

    public void logout() {
        SharedPreferences sharedpreferences = getSharedPreferences(MainActivity.MyPREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.clear();
        editor.commit();
    }


    //SSL IGNORE certificate
    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            OkHttpClient okHttpClient = builder.build();
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}