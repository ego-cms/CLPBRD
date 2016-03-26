package com.ego_cms.copypaste;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.view.View;
import android.widget.TextView;

import com.ego_cms.copypaste.util.AndroidCommonUtils;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends ActivityBaseCompat {

	private static final String TAG = "MainActivity";


	@Bind(R.id.group_service_controls)
	View groupServiceControls;

	@Bind(R.id.button_service_toggle)
	View buttonServiceToggle;

	@Bind(R.id.button_scan_qr)
	View buttonScanQR;

	@Bind(R.id.group_network_address)
	View groupNetworkAddress;

	@Bind(R.id.button_show_qr)
	View buttonShowQR;

	@Bind(R.id.text_network_address)
	TextView textNetworkAddress;


	@OnClick(R.id.button_service_toggle)
	void onServiceToggleButtonClick() {
		AnimatedVectorDrawableCompat backgroundAnimated = AnimatedVectorDrawableCompat.create(this,
			R.drawable.bg_button_group_animated_forward);

		if (backgroundAnimated != null) {
			AndroidCommonUtils.setBackgroundDrawable(groupServiceControls, backgroundAnimated);
			backgroundAnimated.start();
		}
	}

	@OnClick(R.id.button_scan_qr)
	void onScanQRButtonClick() {
		/* Nothing to do */
	}

	@OnClick(R.id.button_show_qr)
	void onShowQRButtonClick() {
		/* Nothing to do */
	}

	@OnClick(R.id.button_contact_us)
	void onContactUsButtonClick() {
		startActivity(new Intent(Intent.ACTION_VIEW, // preserve new line
			Uri.parse(getString(R.string.ego_cms_contact_url))));
	}


	private void displayServiceEnabledState() {
		groupNetworkAddress.setVisibility(View.VISIBLE);
		groupServiceControls.setVisibility(View.INVISIBLE);
	}

	private void displayServiceDisabledState() {
		groupNetworkAddress.setVisibility(View.INVISIBLE);
		groupServiceControls.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);

		displayServiceDisabledState();
	}

	@Override
	protected void onDestroy() {
		ButterKnife.unbind(this);
		super.onDestroy();
	}
}
