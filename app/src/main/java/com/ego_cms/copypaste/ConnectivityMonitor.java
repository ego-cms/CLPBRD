package com.ego_cms.copypaste;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

import java.util.Arrays;

public class ConnectivityMonitor extends BroadcastReceiver {

	public static boolean isLocalNetworkAvailable(@NonNull Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
			Context.CONNECTIVITY_SERVICE);

		NetworkInfo info = cm.getActiveNetworkInfo();

		return info != null && Arrays.asList(ConnectivityManager.TYPE_WIFI,
			ConnectivityManager.TYPE_ETHERNET)
			.contains(info.getType());
	}


	@Override
	public void onReceive(Context context, Intent intent) {

	}
}
