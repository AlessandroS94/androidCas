package org.apereo.cas.casAndroid.util;


import android.widget.SimpleAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONStringer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.disposables.CompositeDisposable;

import okhttp3.OkHttpClient;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompCommand;
import ua.naiksoftware.stomp.dto.StompHeader;
import ua.naiksoftware.stomp.dto.StompMessage;

public class WebSocket {

    public static final  String TAG = "WEBSOCKET";
    public static final String QR_AUTHENTICATION_CHANNEL_ID = "QR_AUTHENTICATION_CHANNEL_ID";
    public static final String payload = "payload";
    String address = "wss://10.0.2.2:8443/cas/qr-websocket/websocket";
    private SimpleAdapter mAdapter;
    private List<String> mDataSet = new ArrayList<>();
    private StompClient mStompClient;
    private final SimpleDateFormat mTimeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private Gson mGson = new GsonBuilder().create();
    private CompositeDisposable compositeDisposable;

    public WebSocket(OkHttpClient httpClient) {
        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, address, null, httpClient);
        resetSubscriptions();
    }



    private void resetSubscriptions() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
        compositeDisposable = new CompositeDisposable();
    }

    public void send(String jwt, String channel) throws JSONException {

        resetSubscriptions();
        JSONStringer jsonWebToken = new JSONStringer().object().key("token").value(jwt).endObject();
        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader(QR_AUTHENTICATION_CHANNEL_ID, channel));
        headers.add(new StompHeader(StompHeader.DESTINATION, "/qr/accept"));
        mStompClient.connect();
        StompMessage stompMessage = new StompMessage(StompCommand.SEND, headers, jsonWebToken.toString());
        mStompClient.send(stompMessage).subscribe();
    }
}
