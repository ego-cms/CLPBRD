package com.ego_cms.copypaste;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import com.ego_cms.copypaste.util.AndroidCommonUtils;

import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends ActivityBaseCompat {

	private static final String TAG = "MainActivity";


	@Bind(R.id.__view_anchor)
	View viewAnchor;

	@Bind(R.id.group_service_controls)
	View groupServiceControls;

	@Bind(R.id.button_service_toggle)
	TextView buttonServiceToggle;

	@Bind(R.id.button_scan_qr)
	View buttonScanQR;

	@Bind(R.id.__text_magic)
	View magicText;

	@Bind(R.id.__image_magic)
	View magicImage;

	@Bind(R.id.group_network_address)
	View groupNetworkAddress;

	@Bind(R.id.button_show_qr)
	View buttonShowQR;

	@Bind(R.id.text_network_address)
	TextView textNetworkAddress;


	@OnClick(R.id.button_service_toggle)
	void onServiceToggleButtonClick() {
		if (CopyPasteService.isRunning(this)) {
			CopyPasteService.stop(this);
			transitionServiceDisabledState(this::transitionMagicHintIn);
		}
		else {
			textNetworkAddress.setText(
				String.format(Locale.US, "http://%s:%d", CopyPasteService.getNetworkAddress(),
					BuildConfig.SERVER_PORT));

			CopyPasteService.start(this);
			transitionServiceEnabledState();
			transitionMagicHintOut();
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


	private PointF magicTextOrigin;
	private PointF magicImageOrigin;

	private void bringMagicHintIn() {
		magicImage.setX(magicImageOrigin.x);
		magicImage.setY(magicImageOrigin.y);
		magicText.setVisibility(View.VISIBLE);
		magicImage.setVisibility(View.VISIBLE);
	}

	private void transitionMagicHintIn() {
		magicText.setAlpha(0);
		magicText.setScaleX(3);
		magicText.setScaleY(3);
		magicText.setVisibility(View.VISIBLE);

		magicImage.setX(magicImageOrigin.x);
		magicImage.setY(magicImageOrigin.y);

		Resources resources = getResources();
		ViewCompat.animate(magicText)
			.setDuration(resources.getInteger(android.R.integer.config_mediumAnimTime))
			.alpha(1)
			.scaleX(1)
			.scaleY(1);

		magicImage.setAlpha(0);
		magicImage.setRotation(-30);
		magicImage.setX(-magicImage.getWidth());
		magicImage.setVisibility(View.VISIBLE);

		ViewCompat.animate(magicImage)
			.setDuration(resources.getInteger(android.R.integer.config_shortAnimTime))
			.setStartDelay(resources.getInteger(android.R.integer.config_shortAnimTime) / 2)
			.alpha(1)
			.x(magicImageOrigin.x)
			.rotation(0);
	}

	private void bringMagicHintOut() {
		magicText.setVisibility(View.INVISIBLE);
		magicImage.setVisibility(View.INVISIBLE);
	}

	private void transitionMagicHintOut() {
		Resources resources = getResources();
		ViewCompat.animate(magicText)
			.alpha(0)
			.withEndAction(() -> magicText.setVisibility(View.INVISIBLE));
		ViewCompat.animate(magicImage)
			.setDuration(resources.getInteger(android.R.integer.config_shortAnimTime))
			.alpha(0)
			.rotation(-30)
			.x(magicImage.getX() - magicImage.getWidth() / 2)
			.y(magicImage.getY() + magicImage.getHeight())
			.withEndAction(() -> magicText.setVisibility(View.INVISIBLE));
	}


	private PointF screenCenter;
	private PointF groupServiceControlsOrigin;
	private PointF scanQRButtonOrigin;
	private PointF toggleServiceButtonCenter;

	private void displayServiceEnabledState() {
		AndroidCommonUtils.setBackgroundDrawable(groupServiceControls,
			R.drawable.bg_button_group_collapsed);

		groupServiceControls.setX(screenCenter.x - toggleServiceButtonCenter.x);
		groupNetworkAddress.setVisibility(View.VISIBLE);
		buttonServiceToggle.setText(R.string.button_service_toggle_activated);
		buttonServiceToggle.setActivated(true);
		buttonScanQR.setVisibility(View.INVISIBLE);
		buttonScanQR.setX(scanQRButtonOrigin.x);
		buttonScanQR.setY(scanQRButtonOrigin.y);
	}

	private void transitionServiceEnabledState() {
		ViewCompat.animate(buttonScanQR)
			.alpha(0)
			.x(toggleServiceButtonCenter.x - buttonScanQR.getWidth() / 2.f)
			.y(toggleServiceButtonCenter.y - buttonScanQR.getHeight() / 2.f)
			.withEndAction(() -> buttonScanQR.setVisibility(View.INVISIBLE));

		AnimatedVectorDrawableCompat backgroundAnimated = AnimatedVectorDrawableCompat.create(this,
			R.drawable.bg_button_group_animated_forward);

		if (backgroundAnimated != null) {
			AndroidCommonUtils.setBackgroundDrawable(groupServiceControls, backgroundAnimated);
			backgroundAnimated.start();
		}
		groupServiceControls.postDelayed(() -> {
			ViewCompat.animate(groupServiceControls)
				.setInterpolator(new LinearOutSlowInInterpolator())
				.x(screenCenter.x - toggleServiceButtonCenter.x)
				.withEndAction(() -> buttonServiceToggle.setClickable(true));

			buttonServiceToggle.setText(R.string.button_service_toggle_activated);
		}, getResources().getInteger(android.R.integer.config_mediumAnimTime));

		buttonServiceToggle.setActivated(true);
		buttonServiceToggle.setClickable(false);

		groupNetworkAddress.setAlpha(0);
		groupNetworkAddress.setVisibility(View.VISIBLE);
		ViewCompat.animate(groupNetworkAddress)
			.alpha(1);
	}

	private void displayServiceDisabledState() {
		buttonScanQR.setVisibility(View.VISIBLE);
		buttonScanQR.setX(scanQRButtonOrigin.x);
		buttonScanQR.setY(scanQRButtonOrigin.y);
		buttonServiceToggle.setActivated(false);
		buttonServiceToggle.setText(R.string.button_service_toggle_normal);
		groupServiceControls.setX(groupServiceControlsOrigin.x);
		groupNetworkAddress.setVisibility(View.INVISIBLE);
		groupServiceControls.setVisibility(View.VISIBLE);
	}

	private void transitionServiceDisabledState(Runnable onComplete) {
		ViewCompat.animate(groupServiceControls)
			.setInterpolator(new AccelerateDecelerateInterpolator())
			.x(groupServiceControlsOrigin.x)
			.withEndAction(() -> {
				buttonScanQR.setAlpha(0);
				buttonScanQR.setVisibility(View.VISIBLE);
				ViewCompat.animate(buttonScanQR)
					.alpha(1)
					.x(scanQRButtonOrigin.x)
					.y(scanQRButtonOrigin.y);

				AnimatedVectorDrawableCompat backgroundAnimated
					= AnimatedVectorDrawableCompat.create(this,
					R.drawable.bg_button_group_animated_backward);

				if (backgroundAnimated != null) {
					AndroidCommonUtils.setBackgroundDrawable(groupServiceControls,
						backgroundAnimated);
					backgroundAnimated.start();
				}
				buttonServiceToggle.setText(R.string.button_service_toggle_normal);
				buttonServiceToggle.setActivated(false);

				ViewCompat.animate(groupNetworkAddress)
					.alpha(0)
					.withEndAction(() -> groupNetworkAddress.setVisibility(View.INVISIBLE));

				onComplete.run();
			});
	}

	private void initializeView(Runnable onComplete) {
		groupServiceControls.getViewTreeObserver()
			.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					AndroidCommonUtils.removeGlobalLayoutListener(
						groupServiceControls.getViewTreeObserver(), this);

					screenCenter = takeCenter(viewAnchor);
					groupServiceControlsOrigin = takeOrigin(groupServiceControls);
					magicTextOrigin = takeOrigin(magicText);
					magicImageOrigin = takeOrigin(magicImage);
					scanQRButtonOrigin = takeOrigin(buttonScanQR);
					toggleServiceButtonCenter = takeCenter(buttonServiceToggle);

					onComplete.run();
				}
			});
	}


	private void onViewInitialized(boolean restored) {
		boolean isRunning = CopyPasteService.isRunning(this);

		if (isRunning) {
			displayServiceEnabledState();
			bringMagicHintOut();
		}
		else {
			if (restored) {
				bringMagicHintIn();
			}
			else {
				groupServiceControls.postDelayed(this::transitionMagicHintIn, 550);
			}
			displayServiceDisabledState();
		}
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);
		initializeView(() -> onViewInitialized(savedInstanceState != null));
	}

	@Override
	protected void onDestroy() {
		ButterKnife.unbind(this);
		super.onDestroy();
	}


	private PointF takeCenter(View v) {
		return new PointF(v.getX() + v.getWidth() / 2.f, v.getY() + v.getHeight() / 2.f);
	}

	private PointF takeOrigin(View v) {
		return new PointF(v.getX(), v.getY());
	}
}
