package com.hocheol.pushmanager.websocket;

import org.json.JSONObject;

public abstract class WebSocketEventListener {

    public static WebSocketEventListener mInstance;
    public WebSocketEventListener() {}

    public static WebSocketEventListener getInstance() {
        return mInstance;
    }
    public static void setInstance(WebSocketEventListener instance) {
        mInstance = instance;
    }

    public abstract void onPushAgreed();
    public abstract void onSocketConnected();
    public abstract void onSocketDisconnected();
    public abstract void onGreeting(JSONObject json);
    public abstract void onBye(JSONObject json);
    public abstract void onSendSuccess(JSONObject json);
    public abstract void onSendFail(JSONObject json);
    public abstract void onReceiveSuccess(JSONObject json);
    public abstract void onChannelJoin(JSONObject json);
    public abstract void onChannelLeave(JSONObject json);
    public abstract void onChannelError(JSONObject json);
    public abstract void onChannelMessage(JSONObject json);
}
