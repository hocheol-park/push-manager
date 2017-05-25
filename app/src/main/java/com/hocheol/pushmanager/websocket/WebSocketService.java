package com.hocheol.pushmanager.websocket;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;

public class WebSocketService extends Service {

    public static final String TAG = WebSocketService.class.getSimpleName();
    public static final String OS = "android";

    public static final String PACKAGE_NAME = WebSocketService.class.getPackage().getName();
    public static final String ACTION_CONNECT = PACKAGE_NAME+".action.connect";
    public static final String ACTION_DISCONNECT = PACKAGE_NAME+".action.disconnect";
    public static final String ACTION_RECONNECT = PACKAGE_NAME+".action.reconnect";
    public static final String ACTION_RECEIVE = PACKAGE_NAME+".action.receive";
    public static final String ACTION_SEND = PACKAGE_NAME+".action.send";
    public static final String ACTION_CHANNEL_JOIN = PACKAGE_NAME+".action.channeljoin";
    public static final String ACTION_CHANNEL_LEAVE = PACKAGE_NAME+".action.channelleave";

    public static final int CODE_MESSAGE = 100; //일반적인 메시지
    public static final int CODE_ACK = 101; //메시지를 성공적으로 수신했을 경우
    public static final int CODE_DLV_SUCCESS = 102; //수신자에게 전달 성공
    public static final int CODE_DLV_FAIL = 103; //수신자에게 전달 실패


    public static final int CODE_PING = 110; // PING
    public static final int CODE_PONG = 111; // PONG


    public static final int CODE_OK = 200;
    public static final int CODE_ERROR = 400;
    public static final int CODE_ERROR_AUTH = 401; //인증 에러
    public static final int CODE_ERROR_FORMAT = 402; //Message Format Error
    public static final int CODE_ERROR_CHANNEL_JOIN = 403;
    public static final int CODE_ERROR_CHANNEL_MSG = 404;
    public static final int CODE_ERROR_CHANNEL_LEAVE = 405;

    public static final int CODE_CHANNEL_JOIN = 300;
    public static final int CODE_CHANNEL_JOIN_OK = 310;
    public static final int CODE_CHANNEL_LEAVE = 301;
    public static final int CODE_CHANNEL_LEAVE_OK = 311;
    public static final int CODE_CHANNEL_BROADCAST_JOIN = 302;
    public static final int CODE_CHANNEL_BROADCAST_LEAVE = 303;

    public static final int CODE_GREETING = 1000; //최초 접속
    public static final int CODE_GREETING_OK = 1010; //최초 접속 OK

    public static final int CODE_BYE = 1001; //영구 접속 해제 (푸시까지 제거)
    public static final int CODE_BYE_OK = 1011; //영구 접속 OK

    private URI uri;
    private static final String TYPE = "user";
    private int gseq;
    private PrefUtil pref;

    private WebSocketClient mWebSocketClient;

    private Queue waitingList = new LinkedList();
    private boolean isGreeted = false;
    private Hashtable<String, String> msgHashTable = new Hashtable<String, String>();

    public static NotificationCompat.Builder notiBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        registerRestartAlarm(true);
        pref = new PrefUtil(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        if(intent != null) {
            // intent 가 있으므로 알람서비스 종료.
            registerRestartAlarm(false);
            Log.e(TAG, "onStartCommand intent not null");

            // Socket connect 요청.
            if(ACTION_CONNECT.equals(intent.getAction())) {
                if(isRegistered()) {
                    openSocket();
                }
            }
            // Socket disconnect 요청.
            else if(ACTION_DISCONNECT.equals(intent.getAction())) {
                // client 가 null 이거나 socket close 상태가 아니면 close 처리.
                if(mWebSocketClient != null && !mWebSocketClient.getConnection().isClosed()) {
                    //sendByeMessage();
                    mWebSocketClient.close();
                }
                // 이미 close 상태이면.
                else {
                    Intent i = new Intent(WebSocketService.this, WebSocketReceiver.class);
                    i.setAction(ACTION_DISCONNECT);
                    sendBroadcast(i);
                }
            }
            // Message send 요청.
            else if(ACTION_SEND.equals(intent.getAction())) {

                // 소켓이 열려 있다면 전송.
                if(mWebSocketClient != null && mWebSocketClient.getConnection().isOpen()) {
                    sendNormalMessage(intent);
                }
                // 소켓이 닫혀 있을때 처리
                else {
                    Intent i = new Intent(WebSocketService.this, WebSocketReceiver.class);
                    i.setAction(ACTION_DISCONNECT);
                    sendBroadcast(i);
                }
            }
            else if(ACTION_CHANNEL_JOIN.equals(intent.getAction())) {
                if(isGreeted == true) {
                    sendChannelJoin(intent);
                }
                else {
                    Intent i = new Intent(WebSocketService.this, WebSocketReceiver.class);
                    i.setAction(ACTION_DISCONNECT);
                    sendBroadcast(i);
                }
            }
            else if(ACTION_CHANNEL_LEAVE.equals(intent.getAction())) {
                if(isGreeted == true) {
                    sendChannelLeave(intent);
                }
                else {
                    Intent i = new Intent(WebSocketService.this, WebSocketReceiver.class);
                    i.setAction(ACTION_DISCONNECT);
                    sendBroadcast(i);
                }
            }
        }
        else {
            // intent 가 없으므로 새로 살아난 서비스. 알람서비스 등록.
            registerRestartAlarm(true);
            Log.e(TAG, "onStartCommand intent is null");
        }

        return START_STICKY;
    }

    public void openSocket() {
        if(mWebSocketClient == null) {
            if(uri == null) {
                try {
                    uri = new URI(pref.getPrefData("host", ""));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            mWebSocketClient = new WebSocketClient(uri) {

                // Socket Open
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.d(TAG, "onOpen websocket...");
                    // 소켓 오픈 되었음을 알림.
                    Intent i = new Intent(WebSocketService.this, WebSocketReceiver.class);
                    i.setAction(ACTION_CONNECT);
                    sendBroadcast(i);

                    // 최초 접속 인증 전송.
                    sendGreetingMessage();
                }

                // Message Receive
                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "onMessage : " + message);

                    try {
                        JSONObject data = new JSONObject(message);
                        int code = data.getInt("code");

                        // 일반적인 메세지 수신시.
                        if (code == CODE_MESSAGE) {

                            // 메시지 중복 체크해서 중복 아닐때만 받은 메세지 정보를 Broadcasting
                            if(!isMessageDuplicate(message)) {

                                Intent i = new Intent(WebSocketService.this, WebSocketReceiver.class);
                                i.setAction(ACTION_RECEIVE);
                                i.putExtra("data", message);
                                sendBroadcast(i);
                            }

                            // 메세지 수신시 ACK 보냄.
                            sendMessage(CODE_ACK, data);
                        }
                        // 일반적인 메세지 아닌 메세지 수신시.
                        else {

                            // 받은 메세지 정보를 Broadcasting
                            Intent i = new Intent(WebSocketService.this, WebSocketReceiver.class);
                            i.setAction(ACTION_RECEIVE);
                            i.putExtra("data", message);
                            sendBroadcast(i);

                            // 핑 수신시 퐁 보냄.
                            if (code == CODE_PING) {
                                sendMessage(CODE_PONG, data);
                            }
                            // 인증 상태 업데이트. queue 에 담긴 메세지 들이 있으면 모두 전송.
                            else if (code == CODE_GREETING_OK) {
                                isGreeted = true;
                                while (waitingList.peek() != null) {
                                    String msg = waitingList.poll().toString();
                                    mWebSocketClient.send(msg);
                                }
                            } else if (code == CODE_OK) { // CODE_BYE_OK
                                mWebSocketClient.close();
                                pref.removePref("name");
                                pref.removePref("screen_name");
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                // Socket Close
                @Override
                public void onClose(int code, String reason, boolean remote){
                    Log.i(TAG, "onClose : " + code);
                    // 소켓 클로즈 되었음을 알림.
                    Intent i = new Intent(WebSocketService.this, WebSocketReceiver.class);
                    i.setAction(ACTION_DISCONNECT);
                    sendBroadcast(i);
                    mWebSocketClient = null;
                    isGreeted = false;

                    // 사용자의 의도로 close 한게 아니면 재 접속 시킨다.
                    if(code != CloseFrame.NORMAL) {
                        reOpenSocket();
                    } else {
                        //reOpenSocket();
                    }
                }

                // Socket connect error
                @Override
                public void onError(Exception ex) {
                    Log.i(TAG, "onError : " + ex.getMessage());
                }
            };
            mWebSocketClient.connect();
        }
    }

    public void reOpenSocket() {
        if(!mWebSocketClient.getConnection().isConnecting() && !mWebSocketClient.getConnection().isOpen()) {
            mWebSocketClient = null;
            openSocket();
        }
    }

    // 최초 접속 코드 서버에 전송.
    public void sendGreetingMessage() {

        JSONObject sendJson = new JSONObject();
        JSONObject dataJson = new JSONObject();

        /*
            CODE_GREETING data format : {
                "os", "device_token", "screen_name", "type", "name"
            }
         */
        try {
            dataJson.put("os", OS);
            dataJson.put("device_token", pref.getPrefData("device_token", ""));
            dataJson.put("screen_name", pref.getPrefData("screen_name", ""));
            dataJson.put("type", TYPE);
            dataJson.put("name", pref.getPrefData("name", ""));

            sendJson.put("code", CODE_GREETING);
            sendJson.put("message", "");
            sendJson.put("data", dataJson);

        } catch(JSONException e) {
            e.printStackTrace();
        }

        mWebSocketClient.send(sendJson.toString());
        Log.d(TAG, "send : " + sendJson.toString());
    }

    // 그룹채팅방 입장.
    public void sendChannelJoin(Intent intent) {

        JSONObject sendJson = new JSONObject();
        JSONObject dataJson = new JSONObject();

        /*
            CODE_CHANNEL_JOIN data format : {
                "channel", "secret"
            }
         */
        try {
            dataJson.put("channel", intent.getStringExtra("channel"));
            dataJson.put("secret", intent.getStringExtra("secret"));

            sendJson.put("code", CODE_CHANNEL_JOIN);
            sendJson.put("message", "");
            sendJson.put("data", dataJson);

        } catch(JSONException e) {
            e.printStackTrace();
        }

        mWebSocketClient.send(sendJson.toString());
        Log.d(TAG, "send : " + sendJson.toString());
    }

    // 그룹채팅방 퇴장.
    public void sendChannelLeave(Intent intent) {

        JSONObject sendJson = new JSONObject();
        JSONObject dataJson = new JSONObject();

        /*
            CODE_CHANNEL_JOIN data format : {
                "channel"
            }
         */
        try {
            dataJson.put("channel", intent.getStringExtra("channel"));

            sendJson.put("code", CODE_CHANNEL_LEAVE);
            sendJson.put("message", "");
            sendJson.put("data", dataJson);

        } catch(JSONException e) {
            e.printStackTrace();
        }

        mWebSocketClient.send(sendJson.toString());
        Log.d(TAG, "send : " + sendJson.toString());
    }

    // 이전 메시지와 비교하여 중복 처리.
    public boolean isMessageDuplicate(String msg) {
        String key = makeSha256Key(msg);
        String value = msg;

        // 해시 테이블에 같은 키값을 가진 메시지가 있으면 중복되었다 판단.
        if(msgHashTable.get(key) != null) {
            return true;
        }
        // 해시 테이블에 저장.
        else {
            if(msgHashTable.size() > 100) {
                Enumeration en = msgHashTable.keys();
                msgHashTable.remove(en.nextElement());
            }
            msgHashTable.put(key, value);
            Log.e("HashTable", "size : " + msgHashTable.size());
            return false;
        }
    }

    // 영구 접속 해지 코드 서버에 전송.
    public void sendByeMessage() {

        JSONObject sendJson = new JSONObject();
        JSONObject dataJson = new JSONObject();

        /*
            CODE_BYE data format : {
                "os", "device_token", "screen_name", "name"
            }
         */
        try {
            dataJson.put("os", OS);
            dataJson.put("device_token", pref.getPrefData("device_token", ""));
            dataJson.put("screen_name", pref.getPrefData("screen_name", ""));
            dataJson.put("name", pref.getPrefData("name", ""));

            sendJson.put("code", CODE_BYE);
            sendJson.put("message", "");
            sendJson.put("data", dataJson);

        } catch(JSONException e) {
            e.printStackTrace();
        }

        mWebSocketClient.send(sendJson.toString());
        Log.d(TAG, "send : " + sendJson.toString());
    }

    // 일반적인 메세지(100) 전송
    public void sendNormalMessage(Intent intent) {

        JSONObject sendJson = new JSONObject();
        JSONObject dataJson = new JSONObject();

        Long timestamp = intent.getLongExtra("TIME_STAMP", System.currentTimeMillis());
        String recipient = intent.getStringExtra("RECIPIENT");
        String message = intent.getStringExtra("MESSAGE");
        String type = intent.getStringExtra("TYPE");

        /*
            CODE_MESSAGE data format : {
                "uuid", "client_seq", "timestamp", "type", "recipient", "screen_name", "name"
            }
         */
        try {
            dataJson.put("uuid", "");
            dataJson.put("client_seq", ++gseq + "");
            dataJson.put("timestamp", timestamp);
            dataJson.put("type", type);
            dataJson.put("recipient", recipient);
            dataJson.put("screen_name", pref.getPrefData("screen_name", ""));
            dataJson.put("name", pref.getPrefData("name", ""));

            sendJson.put("code", CODE_MESSAGE);
            sendJson.put("message", message);
            sendJson.put("data", dataJson);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 서버 인증처리 되었으면 메세지 전송. 아니면 queueing
        if(isGreeted == true) {
            mWebSocketClient.send(sendJson.toString());
        }
        else {
            waitingList.offer(sendJson.toString());
            Log.d("ADD", message);
        }
        Log.d(TAG, "send : " + sendJson.toString());
    }

    // 받은 메세지 별로 서버에 던져주는 메세지.
    public void sendMessage(int code, JSONObject data) {
        JSONObject sendJson = new JSONObject();
        JSONObject dataJson = new JSONObject();

        /*
            CODE_ACK data format : {
                "uuid", "client_seq", "timestamp"
            }
         */
        if(code == CODE_ACK) {
            try {

                dataJson.put("client_seq", data.getJSONObject("data").getString("client_seq"));
                dataJson.put("timestamp", data.getJSONObject("data").getLong("timestamp"));
                dataJson.put("uuid", data.getJSONObject("data").getString("uuid"));

                sendJson.put("data", dataJson);
                sendJson.put("message", "");
                sendJson.put("code", code);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        /*
            CODE_PONG data format : { }
         */
        else if(code == CODE_PONG) {
            try {
                sendJson.put("data", dataJson);
                sendJson.put("message", data.getString("message"));
                sendJson.put("code", code);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        mWebSocketClient.send(sendJson.toString());
        Log.d(TAG, "send : "+sendJson.toString());
    }

    // Service Restart 를 위한 AlarmManager 생성 .. 5초 후부터 20초 간격으로 실행.
    public void registerRestartAlarm(boolean isOn){
        Intent intent = new Intent(this, WebSocketReceiver.class);
        intent.setAction(ACTION_RECONNECT);
        PendingIntent sender = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if(isOn){
            Log.e(TAG, "AlarmManager Start");
            am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5000, 20000, sender);
        } else {
            am.cancel(sender);
            Log.e(TAG, "AlarmManager Cancel");
        }
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

    public String makeSha256Key(String str) { // 암호화 지정

        byte[] defaultBytes = str.getBytes();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("SHA-256");
            algorithm.reset();
            algorithm.update(defaultBytes);
            byte messageDigest[] = algorithm.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            str = hexString + "";
        } catch (NoSuchAlgorithmException nsae) {

        }
        return str;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        registerRestartAlarm(false);
        if(mWebSocketClient != null && mWebSocketClient.getConnection().isClosed()) {
            mWebSocketClient.close();
        }
        mWebSocketClient = null;
    }
}
