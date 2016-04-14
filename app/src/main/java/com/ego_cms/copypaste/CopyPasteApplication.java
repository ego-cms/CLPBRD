package com.ego_cms.copypaste;

import android.app.Application;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDex;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.widget.Toast;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class CopyPasteApplication extends Application {

	@NonNull
	public static CopyPasteApplication get(Context context) {
		return (CopyPasteApplication) context.getApplicationContext();
	}


	private static final int NOTIFICATION_ID = 1;

	@Override
	public void onCreate() {
		super.onCreate();

		CalligraphyConfig.initDefault(
			new CalligraphyConfig.Builder().setDefaultFontPath("fonts/Ubuntu-R.ttf")
				.setFontAttrId(R.attr.fontPath)
				.build());

		CopyPasteService.initialize(this);
		CopyPasteService.registerCallback(new CopyPasteService.Callback() {

			PendingIntent pendingIntent = PendingIntent.getActivity(CopyPasteApplication.this, 0,
				new Intent(CopyPasteApplication.this, MainActivity.class),
				PendingIntent.FLAG_UPDATE_CURRENT);

			@Override
			public void onStart(@CopyPasteService.RoleDef int role, String ipAddress) {
				switch (role) {
					case CopyPasteService.ROLE_CLIENT:
						NotificationManagerCompat.from(CopyPasteApplication.this)
							.notify(NOTIFICATION_ID, buildNotification(
								getString(R.string.title_notification_is_running_as_client,
									getString(R.string.application_name)),
								getString(R.string.content_notification_is_running_as_client,
									ipAddress), pendingIntent, true, false).setSmallIcon(
								R.mipmap.ic_notification)
								.build());
						break;

					case CopyPasteService.ROLE_SERVER:
						NotificationManagerCompat.from(CopyPasteApplication.this)
							.notify(NOTIFICATION_ID, buildNotification(
								getString(R.string.title_notification_is_running_as_service,
									getString(R.string.application_name)),
								getString(R.string.content_notification_is_running_as_service,
									ipAddress), pendingIntent, true, false).setSmallIcon(
								R.mipmap.ic_notification)
								.build());
						break;
				}
			}

			@Override
			public void onStop() {
				NotificationManagerCompat.from(CopyPasteApplication.this)
					.cancel(NOTIFICATION_ID);
			}

			@Override
			public void onError() {
				NotificationManagerCompat.from(CopyPasteApplication.this)
					.cancel(NOTIFICATION_ID);
			}

			@Override
			public void onClipChanged(ClipData clipData) {
				Toast.makeText(CopyPasteApplication.this, R.string.label_clip_updated,
					Toast.LENGTH_LONG)
					.show();
			}
		});

		// TODO: initialize additional components here.
		initSharedPreferences();
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);

		MultiDex.install(this);
	}


	private SharedPreferences commonPreferences;

	private void initSharedPreferences() {
		commonPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	}


	@NonNull
	public SharedPreferences getCommonPreferences() {
		return commonPreferences;
	}

	@NonNull
	public KeyValueStorage getCommonKeyValueStorage() {
		return new SharedKeyValueStorage(commonPreferences);
	}


	public static final int NOTIFICATION_PRIORITY_UNDEFINED = Integer.MIN_VALUE;

	public NotificationCompat.Builder buildNotification(@Nullable String title,
		@Nullable String content, @Nullable PendingIntent contentIntent, boolean ongoing,
		boolean autoCancel, int priority) {

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setSmallIcon(
			R.mipmap.ic_launcher);

		if (TextUtils.isEmpty(title)) {
			title = this.getString(R.string.application_name);
		}
		builder.setContentTitle(title);

		if (!TextUtils.isEmpty(content)) {
			builder.setContentText(content);
		}
		if (contentIntent != null) {
			builder.setContentIntent(contentIntent);
		}
		builder.setOngoing(contentIntent != null && ongoing)
			.setAutoCancel(contentIntent == null || autoCancel);

		if (priority != NOTIFICATION_PRIORITY_UNDEFINED) {
			builder.setPriority(priority);
		}
		return builder;
	}

	public NotificationCompat.Builder buildNotification(@Nullable String title,
		@Nullable String content, @Nullable PendingIntent contentIntent, boolean ongoing,
		boolean autoCancel) {

		return buildNotification(title, content, contentIntent, ongoing, autoCancel,
			NOTIFICATION_PRIORITY_UNDEFINED);
	}
}
