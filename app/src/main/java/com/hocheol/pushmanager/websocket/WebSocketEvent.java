package com.hocheol.pushmanager.websocket;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.hocheol.pushmanager.GCMIntentService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

public class WebSocketEvent {

    private Context context;
    private PrefUtil pref;
    private String lookupHost;

    public static final String HOST = "ws://125.209.198.188:4514/websocket";
    public static final String TYPE_USER = "user";
    public static final String TYPE_CHANNEL = "channel";

    public WebSocketEvent(Context context) {
        this.context = context;
        this.pref = new PrefUtil(context);
    }

    // Lookup 서버 정보.
    public void setLookup(String host) {
        lookupHost = host;
    }

    // Push 동의 여부 체크.
    public boolean isAgreedPush() {
        GCMRegistrar.checkDevice(context);
        GCMRegistrar.checkManifest(context);
        String deviceToken = GCMRegistrar.getRegistrationId(context);
        return deviceToken.equals("") ? false : true;
    }

    // 동의 여부 셋팅.
    public void setPushAgree() {
        GCMRegistrar.register(context, GCMIntentService.PROJECT_ID);
    }

    // 인증 여부 체크.
    public boolean isRegistered() {

        if(pref.getPrefData("name", null) == null || pref.getPrefData("screen_name", null) == null) {
            return false;
        }
        else {
            return true;
        }
    }

    // Noti 받을지 여부 셋팅.
    public void setNotification(boolean agreement) {
        if(agreement == true) {
            pref.setPrefData("getNotiClass", context.getClass().getName().trim());
        }
        pref.setPrefData("notiAgree", agreement);
    }

    public void setNotification(boolean agreement, NotificationCompat.Builder builder) {
        if(agreement == true) {
            pref.setPrefData("getNotiClass", context.getClass().getName().trim());
            WebSocketService.notiBuilder = builder;
        }
    }

    // 인증 처리.
    public void register(String name, String screenName) {
        /*
            List of data saved in device
            - name
            - screen_name
            - device_token
         */
        pref.setPrefData("name", name);
        pref.setPrefData("screen_name", screenName);
    }

    // look up 서버 연결.
    public void connect() {
        OkHttpClient client = new OkHttpClient();
        FormEncodingBuilder formBuilder = new FormEncodingBuilder();
        formBuilder.add("", "");
        Request.Builder builder = new Request.Builder();
        Request request = builder.url(lookupHost).post(formBuilder.build()).build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e("Fail", e.getMessage());
                socketConnect(HOST);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String res = response.body().string();
                Log.e("RES", res);
                socketConnect(HOST);
            }
        });
    }

    // 서비스 시작 및 웹 소켓 연결.
    private void socketConnect(String host) {
        pref.setPrefData("host", host);
        //if(!isServiceRunningCheck()) {
            Intent intent = new Intent(context, WebSocketService.class);
            intent.setAction(WebSocketService.ACTION_CONNECT);
            context.startService(intent);
        //}
    }

    // 웹 소켓 연결 끊음.
    public void disconnect() {
        Intent intent = new Intent(context, WebSocketService.class);
        intent.setAction(WebSocketService.ACTION_DISCONNECT);
        context.startService(intent);
    }

    // 그룹채팅방 입장.
    public void joinChannel(String channelName) {
        Intent intent = new Intent(context, WebSocketService.class);
        intent.setAction(WebSocketService.ACTION_CHANNEL_JOIN);
        Bundle extras = new Bundle();
        extras.putString("channel", channelName);
        extras.putString("secret", "");
        intent.putExtras(extras);
        context.startService(intent);
    }

    // 그룹채팅방 퇴장.
    public void leaveChannel(String channelName) {
        Intent intent = new Intent(context, WebSocketService.class);
        intent.setAction(WebSocketService.ACTION_CHANNEL_LEAVE);
        Bundle extras = new Bundle();
        extras.putString("secret", channelName);
        intent.putExtras(extras);
        context.startService(intent);
    }

    // 메세지 전송.
    public void sendMessage(String type, String recipient, String msg) {
        Intent intent = new Intent(context, WebSocketService.class);
        intent.setAction(WebSocketService.ACTION_SEND);
        Bundle extras = new Bundle();
        extras.putLong("TIME_STAMP", System.currentTimeMillis());
        extras.putString("RECIPIENT", recipient);
        extras.putString("MESSAGE", msg);
        extras.putString("TYPE", type);
        intent.putExtras(extras);
        context.startService(intent);
    }

    // 메세지 전송 with 커스텀 인자.
    public void sendMessage(String type, String recipient, String msg, HashMap<String, String> customParams) {
        Intent intent = new Intent(context, WebSocketService.class);
        intent.setAction(WebSocketService.ACTION_SEND);
        Bundle extras = new Bundle();
        extras.putLong("TIME_STAMP", System.currentTimeMillis());
        extras.putString("RECIPIENT", recipient);
        extras.putString("MESSAGE", msg);
        extras.putString("TYPE", type);
        if(customParams != null && customParams.size() > 0) {
            Iterator<String> it = customParams.keySet().iterator();
            while(it.hasNext()) {
                String key = it.next();
                String value = customParams.get(key);
                extras.putString(key, value);
            }
        }
        intent.putExtras(extras);
        context.startService(intent);
    }

    // 리스너 셋팅.
    public void setEventListener(WebSocketEventListener listener) {
        WebSocketEventListener.setInstance(listener);
    }

    // 서비스 가동중인지 체크.
    public boolean isServiceRunningCheck() {
        ActivityManager manager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (WebSocketService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
