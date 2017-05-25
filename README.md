# Push manager

## 개발 IDE

 - Android Studio 1.2.1.1

## 구성

1. 외부 라이브러리


2. 소스 기본 구성


+	a. PrefUtil
	- Android SharedPreference 사용을 위한 util

+	b. WebSocketEvent
	- 본 앱에서 Web Socket 서비스를 이용하기 위한 기본 class

+	c. WebSocketService
	- Web Socket 서버와 백그라운드에서도 통신을 이어가기 위한 service class.

+	d. WebSocketReceiver
	- Web Socket 통신 응답을 Broadcasting 해주는 receiver class.

+	e. WebSocketEventListener 
	- Web Socket 통신 중에 전달 받아야 할 기능을 정의해놓은 abstract class.

3. 통신 코드표

| 코드 String                       | 코드 Int   | 상태                    |
| -------------------------------- |:---------:| ---------------------: |
| CODE_MESSAGE                     | 100       | 일반 적인 메시지 ( 채팅 )   |
| CODE_ACK                         | 101       | 메시지를 성공적으로 수신     |
| CODE_DLV_SUCCESS                 | 102       | 수신자에게 메시지 전달성공    |
| CODE_DLV_FAIL                    | 103       | 수신자에게 메시지 전달실패    |
| CODE_OK                          | 200       | 일반적인 응답             |
| CODE_ERROR                       | 400       | 일반적인 에러             |
| CODE_ERROR_AUTH                  | 401       | 인증 실패                |
| CODE_ERROR_FORMAT                | 402       | 데이터 포맷 에러           |
| CODE_PING                        | 110       | 핑 ( 서버 접속 확인 코드 )  |
| CODE_PONG                        | 111       | 퐁 ( 서버 접속 확인 코드 )  |
| CODE_GREETING                    | 1000      | 최초접속                 |
| CODE_GREETING_OK                 | 1010      | 최초접속 성공             |
| CODE_BYE                         | 1001      | 영구 접속 해제 (푸쉬 제거)  |
| CODE_BYE_OK                      | 1011      | 영구 접속 해제 성공        |
| CODE_FLASH_MESSAGE               | 104       | 일반 푸쉬 ( 체크 필요없음 ) |
| CODE_ERROR_JOIN_CHANNEL          | 403       | 채널입장 에러             |
| CODE_ERROR_MSG_PERMISSION        | 404       | 메시지 허가 에러          |
| CODE_ERROR_LEAVE_CHANNEL         | 405       | 채널 접속해제 에러         |
| CODE_INTERNAL_FORCE_CLOSE        | 2000      | 강제 접속 종료           |
| CODE_CHANNEL_BROADCAST_LEAVE     | 303       | 채널에서 유저 떠남 알림    |
| CODE_CHANNEL_BROADCAST_LEAVE_OK  | 313       | 채널에서 유저 떠남 알림    |   
| CODE_CHANNEL_JOIN                | 300       | 채널 접속               |
| CODE_CHANNEL_JOIN_OK             | 310       | 채널 접속 성공           |
| CODE_CHANNEL_LEAVE               | 301       | 채널 접속 해제           |
| CODE_CHANNEL_LEAVE_OK            | 311       | 채널 접속 해제 성공        |

4. 메시지 타입

| 타입       |           | 설명                 |
| --------- |:---------:| ------------------: |
| TYPE_USER | user      | 유저에게 개인 메시지 전송 | 
| TYPE_CH   | channel   | 채널에 전체메시지 전송    |


## 라이브러리 사용 방법

1. Add Library
	
+	a.  pushmanager 폴더를 프로젝트 최상단에 넣는다.

	- YourProject
		- app
		- pushmanager ( Add here )

+	b. YourProject/setting.gradle 을 열어 다음과 같이 수정한다.
	
+	c. YourProject/app/build.gradle 을 열어 dependencies 에 다음과 같이 
	complie project(‘:pushmanager’) 를 추가한다.

+	d. Tools > Android > Sync Project with Gradle Files 을 클릭하여 sync 한다.

2. GCMIntentService.java 추가

+	a. 프로젝트 패키지 경로 상단에 GCMIntentService.java 파일을 추가한다.
	(Push 서비스 이용을 위해)

+	b. onMessage() 에 푸시 받았을 때의 기능을 구현한다.

+	c. 유의 사항
	  - GCM 푸시는 Web socket 이 close 된 상태일때만 서버에서 보내준다.
	  - onMessage() 이외의 메소드를 수정할시 push agreement 에 오류가 발생할 수 있다.


3. AndroidManifest.xml 추가

+ a. GCM 을 이용하기 위한 퍼미션을 아래와 같이 등록한다. YourAppPackagePath 에는 프로젝트 패키지 경로를 삽입한다.

<permission
        android:name="YourAppPackagePath.permission.C2D_MESSAGE"
        android:protectionLevel="signature" />

    <uses-permission android:name="YourAppPackagePath.permission.C2D_MESSAGE" />
    <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
+ b. GCM 서비스와 Broadcast Receiver 를 아래와 같이 등록해준다.
<receiver
            android:name="com.google.android.gcm.GCMBroadcastReceiver"
            android:permission="com.google.android.c2dm.permission.SEND" >
            <intent-filter>
                <!-- Receives the actual messages. -->
                <action android:name="com.google.android.c2dm.intent.RECEIVE" />
                <!-- Receives the registration id. -->
                <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
            </intent-filter>
        </receiver>

        <service android:name="YourAppPackagePath.GCMIntentService" />


4. WebSocketEvent 

| 메서드       | 설명                 |
| ---------  | ------------------: |
| void setEventListener(WebSocketEventListener listener) | WebSocket 통신 작업중 값 전달을 위해 필요한 listener 를 등록합니다. listener : WebSocketEventListener 에 대한 설명은 5) WebSocketEventListener 클래스 설명 을 참. | 
| boolean isAgreedPush() | 푸쉬 설정 동의 여부를 반환합니다 | 
| void setPushAgree() | 푸쉬 동의 설정을 합니다. GCM 서버와 통신하며 이에 대한 성공, 실패 여부는 WebSocketEventListener의onPushAgreed, onPushAgreeError 로 구분합니다 | 
| boolean isRegistered() | 사용자 등록 여부를 반환합니다. | 
| void register(String name, String screenName) | 사용자 등록을 처리합니다. name : 유저 네임 / screenName : 유저 스크린 네임 | 
| void setLookup(String lookupHost) | Lookup 서버 호스트를 설정합니다. lookupHost : 웹소켓 서버에 접속하기 위해서 서버 정보를 받아오는 lookup 서버 호스트 | 
| void connect() | 웹소켓 서버에 접속합니다. 접속 시도에 대한 성공 실패 여부는 WebSocketEventListener의 onSocketConnected, onSocketDisconnected 로 구분합니다. lookup 서버에 Http 통신하여 받아온 리스트에 랜덤으로 접속합니다. | 
| void disconnect() | 웹소켓 서버와 연결을 해지합니다. | 
| void reconnect() | 웹소켓 서버에 재접속합니다. 이미 접속 상태이면 접속 중인 소켓을 끊고 새로운 접속을 시도하며 접속 상태가 아니면 바로 새로운 접속을 시도합니다. | 
| void sendMessage(String type, String recipient, String msg) | 메세지를 전송합니다. 메세지 전송의 성공 여부는 WebSocketEventListener의 onSendMessage, onSendFail 로 구분합니다. type : 받는 사람 타입 (user or channel) / recipient : 받는 사람 / msg : 전송할 메세지 | 

5. WebSocketEventListener

| 타입       |           | 설명                 |
| --------- |:---------:| ------------------: |
| TYPE_USER | user      | 유저에게 개인 메시지 전송 | 
