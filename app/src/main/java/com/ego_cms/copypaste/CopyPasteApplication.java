package com.ego_cms.copypaste;

import android.app.Application;
import android.content.ClipData;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.widget.Toast;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class CopyPasteApplication extends Application {

	@NonNull
	public static CopyPasteApplication get(Context context) {
		return (CopyPasteApplication) context.getApplicationContext();
	}


	@Override
	public void onCreate() {
		super.onCreate();

		CalligraphyConfig.initDefault(
			new CalligraphyConfig.Builder().setDefaultFontPath("fonts/Ubuntu-R.ttf")
				.setFontAttrId(R.attr.fontPath)
				.build());

		CopyPasteService.initialize(this);
		CopyPasteService.registerCallback(new CopyPasteService.Callback() {
			@Override
			public void onStart(@CopyPasteService.RoleDef int role) {
				/* Nothing to do */
			}

			@Override
			public void onStop() {
				/* Nothing to do */
			}

			@Override
			public void onError() {
				/* Nothing to do */
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
}
