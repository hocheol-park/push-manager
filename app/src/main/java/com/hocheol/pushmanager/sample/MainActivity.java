package com.hocheol.pushmanager.sample;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hocheol.pushmanager.R;
import com.hocheol.pushmanager.websocket.WebSocketEvent;
import com.hocheol.pushmanager.websocket.WebSocketEventListener;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity implements OnClickListener {

    TextView msgText;
    protected Button btnSend, btnBye, btnJoin, btnLeave;
    EditText msgInput, recipient;
    WebSocketEvent ws;

    String name, screenName, deviceToken;

    public static final String LOOKUP_HOST = "";	// Have to set Lookup host here
    public String type = "user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {

        name = "testname";
        screenName = "testscreen";
        deviceToken = "";

        ws = new WebSocketEvent(this);

        // 리스너 등록.
        ws.setEventListener(new WebSocketEventListener() {
            @Override
            public void onPushAgreed() {
                Log.d("Main", "onPushAgreed");
                connect();
            }

            @Override
            public void onSocketConnected() {
                msgText.setText(msgText.getText().toString() + "\n\n" + "Socket Connected");
            }

            @Override
            public void onSocketDisconnected() {
                msgText.setText(msgText.getText().toString() + "\n\n" + "Socket Closed");
            }

            @Override
            public void onGreeting(JSONObject json) {
                msgText.setText(msgText.getText().toString() + "\n\n" + json.toString());
            }

            @Override
            public void onBye(JSONObject json) {
                msgText.setText(msgText.getText().toString() + "\n\n" + json.toString());
            }

            @Override
            public void onSendSuccess(JSONObject json) {
                msgText.setText(msgText.getText().toString() + "\n\n" + json.toString());
            }

            @Override
            public void onSendFail(JSONObject json) {
                msgText.setText(msgText.getText().toString() + "\n\n" + json.toString());
            }

            @Override
            public void onReceiveSuccess(JSONObject json) {
                msgText.setText(msgText.getText().toString() + "\n\n" + json.toString());
                Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibe.vibrate(500);
            }

            @Override
            public void onChannelJoin(JSONObject json) {
                msgText.setText(msgText.getText().toString() + "\n\n" + json.toString());
                type = WebSocketEvent.TYPE_CHANNEL;
                String message = null;
                try {
                    message = json.getString("message");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String[] messageAry = message.split(":");
                if(messageAry.length > 1) {
                    message = messageAry[1];
                }
                recipient.setText(message);
            }

            @Override
            public void onChannelLeave(JSONObject json) {
                msgText.setText(msgText.getText().toString() + "\n\n" + json.toString());
                type = WebSocketEvent.TYPE_USER;
            }

            @Override
            public void onChannelError(JSONObject json) {
                msgText.setText(msgText.getText().toString() + "\n\n" + json.toString());
            }

            @Override
            public void onChannelMessage(JSONObject json) {
                msgText.setText(msgText.getText().toString() + "\n\n" + json.toString());
            }
        });

        // Lookup 서버 호스트 셋팅.
        ws.setLookup(LOOKUP_HOST);

        // 푸시 동의 여부 체크.
        if(!ws.isAgreedPush()) {
            // 푸시 동의 처리.
            ws.setPushAgree();
        }
        // 푸시 동의 하였다면.
        else {
            connect();
        }

        msgText = (TextView) findViewById(R.id.message_text);
        btnSend = (Button) findViewById(R.id.btn_send);
        btnBye = (Button) findViewById(R.id.btn_bye);
        btnJoin = (Button) findViewById(R.id.btn_join);
        btnLeave = (Button) findViewById(R.id.btn_leave);
        msgInput = (EditText) findViewById(R.id.message);
        recipient = (EditText) findViewById(R.id.recipient);

        recipient.setText(screenName);

        btnSend.setOnClickListener(this);
        btnBye.setOnClickListener(this);
        btnJoin.setOnClickListener(this);
        btnLeave.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v == btnSend) {
            // 메세지 전송.
            ws.sendMessage(type, recipient.getText().toString().trim(), msgInput.getText().toString());
            // 커스텀 인자 있으면 ..
            //ws.sendMessage(type, recipient.getText().toString().trim(), msgInput.getText().toString(), new HashMap<String, String>());
            msgInput.setText("");
        }
        else if(v == btnBye) {
            ws.disconnect();
        }
        else if(v == btnJoin) {
            ws.joinChannel("test");
        }
        else if(v == btnLeave) {
            ws.leaveChannel("test");
        }
    }

    public void connect() {
        // 사용자 인증 여부 체크.
        if (ws.isRegistered()) {
            ws.register(name, screenName);
        } else {
            ws.register(name, screenName);
        }

        // 메시지 Notification 받을지 여부.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("title")
                .setContentText("text")
                .setSmallIcon(android.R.drawable.ic_notification_overlay)
                .setTicker("ticker");
        ws.setNotification(true, builder);
        //ws.setNotification(true);

        // 서비스 시작.
        ws.connect();
    }
}
