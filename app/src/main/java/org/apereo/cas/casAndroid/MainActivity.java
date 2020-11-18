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

import java.security.cert.CertificateException;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.apereo.cas.casAndroid.model.User;
import org.apereo.cas.casAndroid.util.LoginRest;
import org.apereo.cas.casAndroid.util.WebSocket;
import org.json.JSONException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

import static org.apereo.cas.casAndroid.R.string.QR_Code_Channel_Not_Found;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //Button
    private FloatingActionButton buttonScan;
    private Button buttonLogin;
    private Button buttonLogout;
    private EditText user_id;
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
        user_id = (EditText) findViewById(R.id.userid);
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
                Toast.makeText(this, QR_Code_Channel_Not_Found, Toast.LENGTH_LONG).show();
            } else {
                //if qr contains data send message to WebSocket
                Toast.makeText(this, result.getContents(), Toast.LENGTH_LONG).show();
                WebSocket channelAuth = new WebSocket(getUnsafeOkHttpClient());
                SharedPreferences sharedpreferences = getSharedPreferences(MainActivity.MyPREFERENCES, Context.MODE_PRIVATE);
                String value = sharedpreferences.getString(String.valueOf(User.JWT), null);
                try {
                    channelAuth.send(value, result.getContents());
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

    public boolean checkDataEntered() {

        if (isEmpty(user_id)) {
            Toast t = Toast.makeText(this, R.string.Insert_id_user, Toast.LENGTH_LONG);
            t.show();
            return false;
        }

        if (isEmpty(password)) {
            Toast t = Toast.makeText(this, R.string.Password_is_required, Toast.LENGTH_LONG);
            t.show();
            return false;
        }

        return true;
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
                if (this.checkDataEntered()) {
                    String mUser_id = user_id.getText().toString();
                    String mPassword = password.getText().toString();
                    OkHttpClient client = getUnsafeOkHttpClient();
                    LoginRest loginRest = new LoginRest(mUser_id, mPassword, client);
                    String result = loginRest.login();
                    switch (result) {
                        case "credential_not_correct":
                            Toast credential = Toast.makeText(this, R.string.User_id_or_password_is_not_right, Toast.LENGTH_LONG);
                            credential.show();
                            break;
                        case "server_not_found":
                            Toast serverNotFound = Toast.makeText(this, R.string.Server_not_respond, Toast.LENGTH_LONG);
                            serverNotFound.show();
                            break;
                        case "something_went_wrong":
                            Toast something_went_wrong = Toast.makeText(this, R.string.Somethings_went_wrong, Toast.LENGTH_LONG);
                            something_went_wrong.show();
                            break;
                        default:
                            SharedPreferences.Editor editor = sharedpreferences.edit();
                            editor.putString(String.valueOf(User.JWT), result);
                            editor.commit();
                            updateUI();
                    }
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
            user_id.setVisibility(View.GONE);
            password.setVisibility(View.GONE);
            buttonLogin.setVisibility(View.GONE);
            buttonLogout.setVisibility(View.VISIBLE);
            buttonScan.setVisibility(View.VISIBLE);
        } else {
            user_id.setVisibility(View.VISIBLE);
            password.setVisibility(View.VISIBLE);
            buttonLogin.setVisibility(View.VISIBLE);
            buttonLogout.setVisibility(View.GONE);
            buttonScan.setVisibility(View.GONE);
        }
    }

    //remove the session in the App
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