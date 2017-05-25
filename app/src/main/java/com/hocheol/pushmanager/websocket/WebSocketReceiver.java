package com.hocheol.pushmanager.websocket;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class WebSocketReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        WebSocketEventListener mListener = WebSocketEventListener.getInstance();

        if(intent.getAction().equals(WebSocketService.ACTION_CONNECT)) {
            if(mListener != null) {
                mListener.onSocketConnected();
            }
        }
        else if(intent.getAction().equals(WebSocketService.ACTION_DISCONNECT)) {
            if(mListener != null) {
                mListener.onSocketDisconnected();
            }
        }
        else if(intent.getAction().equals(WebSocketService.ACTION_RECEIVE)) {
            JSONObject json = null;
            try {
                json = new JSONObject(intent.getStringExtra("data"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if(json != null) {
                try {
                    int code = json.getInt("code");

                    // listener 셋팅 되었다면.
                    if(mListener != null) {
                        // 최초 접속.
                        if (code == WebSocketService.CODE_GREETING_OK) {
                            Log.e("Receiver", "GREETING_OK");
                            mListener.onGreeting(json);
                        }
                        // 영구 접속 해지.
                        else if (code == WebSocketService.CODE_BYE_OK) {
                            mListener.onBye(json);
                        }
                        // 일반적인 메세지.
                        else if (code == WebSocketService.CODE_MESSAGE) {
                            mListener.onReceiveSuccess(json);
                            //sendNotification(context, json);
                        }
                        // 메세지 서버 전송 성공.
                        else if (code == WebSocketService.CODE_ACK) {
                            mListener.onReceiveSuccess(json);
                        }
                        // 메세지 수신자 전송 성공.
                        else if (code == WebSocketService.CODE_DLV_SUCCESS) {
                            mListener.onSendSuccess(json);
                        }
                        // 메세지 수신자 전송 실패.
                        else if (code == WebSocketService.CODE_DLV_FAIL) {
                            mListener.onSendFail(json);
                        }
                        // 핑
                        else if (code == WebSocketService.CODE_PING) {
                            mListener.onReceiveSuccess(json);
                        }
                        // 그룹채팅방 입장
                        else if (code == WebSocketService.CODE_CHANNEL_JOIN_OK) {
                            mListener.onChannelJoin(json);
                        }
                        // 그룹채팅방 퇴장
                        else if (code == WebSocketService.CODE_CHANNEL_LEAVE_OK) {
                            mListener.onChannelLeave(json);
                        }
                        // 채널 알림 메시지.
                        else if( code == WebSocketService.CODE_CHANNEL_BROADCAST_JOIN
                                || code == WebSocketService.CODE_CHANNEL_BROADCAST_LEAVE) {
                            mListener.onChannelMessage(json);
                        }
                        // 채널 관련 에러.
                        else if( code == WebSocketService.CODE_ERROR_CHANNEL_JOIN
                                || code == WebSocketService.CODE_ERROR_CHANNEL_MSG
                                || code == WebSocketService.CODE_ERROR_CHANNEL_LEAVE) {
                            mListener.onChannelError(json);
                        }
                        else {
                            mListener.onReceiveSuccess(json);
                        }
                    }
                    // 메세지 받으면 노티 날려줌.
                    if (code == WebSocketService.CODE_MESSAGE) {
                        sendNotification(context, json);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        else if(intent.getAction().equals(WebSocketService.ACTION_RECONNECT)) {
            Log.e("BR", "ACTION_RECONNECT");
            Intent i = new Intent(context, WebSocketService.class);
            i.setAction(WebSocketService.ACTION_CONNECT);
            context.startService(i);
        }
    }

    private boolean isActivityRunning(final Context context, final String packageName, final String className) {
        boolean isRunning = false;
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> info;
        info = activityManager.getRunningTasks(1);
        final ComponentName topActivity = info.get(0).topActivity;
        if (topActivity.getPackageName().equals(packageName) && topActivity.getClassName().equals(className)) {
            isRunning = true;
        }
        return isRunning;
    }

    private void sendNotification(Context context, JSONObject json) {

        PrefUtil pref = new PrefUtil(context);

        Class toClass = null;
        // 유저가 저장한 노티 받을 클래스 명.
        String strClass = pref.getPrefData("getNotiClass", "");
        try {
            toClass = Class.forName(strClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        // 클래스가 존재하면.
        if(!strClass.equals("") && toClass != null) {
            // 노티 동의 하였으면.
            if (pref.getPrefData("notiAgree", false) && !isActivityRunning(context, context.getPackageName(), toClass.getName())) {

                NotificationCompat.Builder builder;
                if (WebSocketService.notiBuilder == null) {
                    builder = new NotificationCompat.Builder(context)
                            .setSmallIcon(android.R.drawable.btn_star)
                            .setContentTitle("New message")
                            .setContentText(json.toString())
                            .setTicker(json.toString())
                            .setAutoCancel(true)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
                } else {
                    builder = WebSocketService.notiBuilder;
                }

                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, toClass), PendingIntent.FLAG_UPDATE_CURRENT);
                Notification noti = builder.setContentIntent(pendingIntent).build();
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(2000, noti);
            }
        }
    }
}
