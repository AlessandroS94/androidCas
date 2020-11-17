package org.apereo.cas.casAndroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.net.URISyntaxException;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //WebSocket
    //private WebSocketClient mWebSocketClient;

    //Button
    private FloatingActionButton buttonScan;
    private Button buttonLogin;
    private Button buttonWebSocket;

    //qr code scanner object
    private IntentIntegrator qrScan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //View objects
        buttonScan = (FloatingActionButton) findViewById(R.id.qrbutton);
        buttonLogin = (Button) findViewById(R.id.loginbutton);
        //buttonWebSocket = (Button) findViewById(R.id.websocket);

        //intializing scan object
        qrScan = new IntentIntegrator(this);

        //attaching onclick listener
       buttonScan.setOnClickListener(this);
       buttonLogin.setOnClickListener(this);
//        buttonWebSocket.setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {

            //if qrcode has nothing in it
            if (result.getContents() == null) {
                Toast.makeText(this, "Result Not Found", Toast.LENGTH_LONG).show();
            } else {
                //if qr contains data
                    Toast.makeText(this, result.getContents(), Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onClick(View view) {
        //initiating the qr code scan
        switch (view.getId())
        {
            case R.id.qrbutton:
                qrScan.initiateScan();
                break;
            case R.id.loginbutton:
                Toast.makeText(this, "Login", Toast.LENGTH_LONG).show();
                break;
            //case  R.id.websocket:
                //Intent Websocket = new Intent(this, MainSocket.class);
                //startActivity(Websocket);
               // break;
        }
    }
}