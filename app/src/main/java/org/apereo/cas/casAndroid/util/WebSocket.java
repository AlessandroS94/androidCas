package org.apereo.cas.casAndroid.util;

import android.util.Log;
import android.widget.SimpleAdapter;

import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;

public class WebSocket {

    public static final  String TAG = "WEBSOCKET";
    public static final String QR_AUTHENTICATION_CHANNEL_ID = "QR_AUTHENTICATION_CHANNEL_ID";
    public static final String payload = "payload";
    String Channel;
    String address = "wss://10.0.2.2:8443/cas/qr-websocket/websocket";
    private SimpleAdapter mAdapter;
    private List<String> mDataSet = new ArrayList<>();
    private StompClient mStompClient;
    private Disposable mRestPingDisposable;
    private final SimpleDateFormat mTimeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private RecyclerView mRecyclerView;
    private Gson mGson = new GsonBuilder().create();
    private CompositeDisposable compositeDisposable;

    public WebSocket(String channel, OkHttpClient httpClient) {
        Channel = channel;
        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, address, null, httpClient);
        resetSubscriptions();
    }

    public String getChannel() {
        return Channel;
    }

    public void setChannel(String channel) {
        Channel = channel;
    }

    private void resetSubscriptions() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
        compositeDisposable = new CompositeDisposable();
    }

    public void Connection() {
        mStompClient.withClientHeartbeat(1000).withServerHeartbeat(1000);

        resetSubscriptions();

        Disposable dispLifecycle = mStompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lifecycleEvent -> {
                    switch (lifecycleEvent.getType()) {
                        case OPENED:
                            Log.i("INFO SOCKET","Stomp connection opened");
                            break;
                        case ERROR:
                            Log.e(TAG, "Stomp connection error", lifecycleEvent.getException());
                            Log.i("INFO SOCKET","Stomp connection error");
                            break;
                        case CLOSED:
                            Log.i("INFO SOCKET","Stomp connection closed");
                            resetSubscriptions();
                            break;
                        case FAILED_SERVER_HEARTBEAT:
                            Log.i("INFO SOCKET","Stomp failed server heartbeat");
                            break;
                    }
                });

        compositeDisposable.add(dispLifecycle);

        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader(QR_AUTHENTICATION_CHANNEL_ID, "a7c475f3-92ef-43ec-8534-3992ba3a5f41"));
        headers.add(new StompHeader(payload, "payload"));
        // Receive greetings
        Disposable dispTopic = mStompClient.topic("/qr/accept",headers)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(topicMessage -> {
                    Log.d(TAG, "Received " + topicMessage.getPayload());
                }, throwable -> {
                    Log.e(TAG, "Error on subscribe topic", throwable);
                });

        compositeDisposable.add(dispTopic);

        mStompClient.connect();

    }
}
