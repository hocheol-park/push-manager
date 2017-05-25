package com.hocheol.pushmanager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.hocheol.pushmanager.websocket.PrefUtil;
import com.hocheol.pushmanager.websocket.WebSocketEventListener;

public class GCMIntentService extends GCMBaseIntentService {

	public static final String TAG = GCMIntentService.class.getSimpleName();
	public static final String PROJECT_ID = "778195886968";
	public static String token = null;

	public GCMIntentService() {
		super(GCMIntentService.PROJECT_ID);
	}

	@Override
	protected void onDeletedMessages(final Context context, final int total) {
		Log.e(TAG, "onDeletedMessages");
	}

	@Override
	public void onError(final Context context, final String errorId) {
		Log.e(TAG, "onError ... : "+errorId);
	}

	@Override
	protected void onMessage(final Context context, final Intent intent) {
		Log.e(TAG, "onMessage");

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

			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			Log.e(TAG, "intent.getExtras() = > " + intent.getExtras());
			String message = intent.getExtras().getString("message");
			String uuid = intent.getExtras().getString("message");
			String title = message;

			String[] messages = message.split(":");
			String sender = messages[0];
			String msg = "";
			if (messages.length > 1) {
				msg = messages[1];
			}

			Intent notificationIntent = new Intent(context, toClass);

			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			final PendingIntent pending_intent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
			Notification notification = builder.setContentIntent(pending_intent)
					.setSmallIcon(android.R.drawable.btn_star)
					.setContentTitle(title)
					.setContentText(msg)
					.setTicker(sender + " : " + msg)
					.setAutoCancel(true)
					.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE).build();

			notificationManager.notify(2000, notification);
		}

	}

	@Override
	protected boolean onRecoverableError(final Context context, final String errorId) {
		Log.e(TAG, "onRecoverableError ... "+errorId);
		return super.onRecoverableError(context, errorId);
	}

	@Override
	protected void onRegistered(final Context context, final String registrationId) {
		Log.e(TAG, "onRegistered ... " + registrationId);
		PrefUtil pref = new PrefUtil(this);
		pref.setPrefData("device_token", registrationId);
		WebSocketEventListener.getInstance().onPushAgreed();
	}

	@Override
	protected void onUnregistered(final Context context, final String registrationId) {
		Log.e(TAG, "onUnregistered");
	}

}
