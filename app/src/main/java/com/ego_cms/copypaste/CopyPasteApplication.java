package com.ego_cms.copypaste;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;

public class CopyPasteApplication extends Application {

	@NonNull
	public static CopyPasteApplication get(Context context) {
		return (CopyPasteApplication)context.getApplicationContext();
	}


	@Override
	public void onCreate() {
		super.onCreate();

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
	public KeyValueStorage getCommonKeyValueStorage() {
		return new SharedKeyValueStorage(commonPreferences);
	}
}
