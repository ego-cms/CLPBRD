package com.ego_cms.copypaste;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import com.ego_cms.copypaste.util.AndroidCommonUtils;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends ActivityBaseCompat {

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

	@Bind(R.id.button_contact_us)
	TextView buttonContactUs;


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

			CopyPasteService.startServer(this);
			transitionServiceEnabledState();
			transitionMagicHintOut();
		}
	}

	@OnClick(R.id.button_scan_qr)
	void onScanQRButtonClick() {
		new IntentIntegrator(this) // preserve new line
			.setBeepEnabled(true)
			.setCaptureActivity(AddressScannerActivity.class)
			.setOrientationLocked(true)
			.setPrompt("")
			.initiateScan(IntentIntegrator.QR_CODE_TYPES);
	}

	@OnClick(R.id.button_show_qr)
	void onShowQRButtonClick() {
		//noinspection ConstantConditions
		AddressDisplayActivity.startAsync(this, // preserve new line
			buttonShowQR, String.format("clpbrd://%s", CopyPasteService.getNetworkAddress()));
	}

	@OnClick(R.id.button_contact_us)
	void onContactUsButtonClick() {
		startActivity(new Intent(Intent.ACTION_VIEW, // preserve new line
			Uri.parse(getString(R.string.ego_cms_contact_url))));
	}


	private boolean hasCamera() {
		return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
	}


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
		if (hasCamera()) {
			AndroidCommonUtils.setBackgroundDrawable(groupServiceControls,
				R.drawable.bg_button_group_forward);

			buttonScanQR.setVisibility(View.INVISIBLE);
			buttonScanQR.setX(scanQRButtonOrigin.x);
			buttonScanQR.setY(scanQRButtonOrigin.y);
		}
		else {
			AndroidCommonUtils.setBackgroundDrawable(groupServiceControls,
				R.drawable.bg_button_group_no_camera_forward);
		}
		groupServiceControls.setX(screenCenter.x - toggleServiceButtonCenter.x);
		groupNetworkAddress.setVisibility(View.VISIBLE);
		buttonServiceToggle.setText(R.string.button_service_toggle_activated);
		buttonServiceToggle.setActivated(true);
	}

	private void transitionServiceEnabledState() {
		AnimatedVectorDrawableCompat backgroundAnimated;

		if (hasCamera()) {
			ViewCompat.animate(buttonScanQR)
				.alpha(0)
				.x(toggleServiceButtonCenter.x - buttonScanQR.getWidth() / 2.f)
				.y(toggleServiceButtonCenter.y - buttonScanQR.getHeight() / 2.f)
				.withEndAction(() -> buttonScanQR.setVisibility(View.INVISIBLE));

			backgroundAnimated = AnimatedVectorDrawableCompat.create(this,
				R.drawable.bg_button_group_animated_forward);
		}
		else {
			backgroundAnimated = AnimatedVectorDrawableCompat.create(this,
				R.drawable.bg_button_group_animated_no_camera_forward);
		}
		if (backgroundAnimated != null) {
			AndroidCommonUtils.setBackgroundDrawable(groupServiceControls, backgroundAnimated);
			backgroundAnimated.start();
		}
		else {
			AndroidCommonUtils.setBackgroundDrawable(groupServiceControls,
				R.drawable.bg_button_group_forward);
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
		if (hasCamera()) {
			AndroidCommonUtils.setBackgroundDrawable(groupServiceControls,
				R.drawable.bg_button_group_backward);

			buttonScanQR.setVisibility(View.VISIBLE);
			buttonScanQR.setX(scanQRButtonOrigin.x);
			buttonScanQR.setY(scanQRButtonOrigin.y);
		}
		else {
			AndroidCommonUtils.setBackgroundDrawable(groupServiceControls,
				R.drawable.bg_button_group_no_camera_backward);
		}
		groupServiceControls.setX(groupServiceControlsOrigin.x);
		groupNetworkAddress.setVisibility(View.INVISIBLE);
		buttonServiceToggle.setText(R.string.button_service_toggle_normal);
		buttonServiceToggle.setActivated(false);
	}

	private void transitionServiceDisabledState(Runnable onComplete) {
		ViewCompat.animate(groupServiceControls)
			.setInterpolator(new AccelerateDecelerateInterpolator())
			.x(groupServiceControlsOrigin.x)
			.withEndAction(() -> {
				AnimatedVectorDrawableCompat backgroundAnimated;

				if (hasCamera()) {
					buttonScanQR.setAlpha(0);
					buttonScanQR.setVisibility(View.VISIBLE);
					ViewCompat.animate(buttonScanQR)
						.alpha(1)
						.x(scanQRButtonOrigin.x)
						.y(scanQRButtonOrigin.y);

					backgroundAnimated
						= AnimatedVectorDrawableCompat.create(this,
						R.drawable.bg_button_group_animated_backward);
				}
				else {
					backgroundAnimated
						= AnimatedVectorDrawableCompat.create(this,
						R.drawable.bg_button_group_animated_no_camera_backward);
				}
				if (backgroundAnimated != null) {
					AndroidCommonUtils.setBackgroundDrawable(groupServiceControls,
						backgroundAnimated);
					backgroundAnimated.start();
				}
				else {
					groupServiceControls.postDelayed(
						() -> AndroidCommonUtils.setBackgroundDrawable(groupServiceControls,
							R.drawable.bg_button_group_backward),
						getResources().getInteger(android.R.integer.config_shortAnimTime));
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
		Drawable drawable = DrawableCompat.wrap(
			AndroidCommonUtils.getDrawableFrom(this, R.drawable.logo_authority))
			.mutate();

		DrawableCompat.setTint(drawable, buttonContactUs.getCurrentTextColor());
		TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(buttonContactUs, null, null,
			drawable, null);

		AndroidCommonUtils.setBackgroundDrawable(groupServiceControls,
			R.drawable.bg_button_group_backward);

		if (!hasCamera()) {
			buttonScanQR.setVisibility(View.GONE);
		}
		groupServiceControls.getViewTreeObserver()
			.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					AndroidCommonUtils.removeGlobalLayoutListener(
						groupServiceControls.getViewTreeObserver(), this);

					screenCenter = takeCenter(viewAnchor);
					groupServiceControlsOrigin = takeOrigin(groupServiceControls);
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


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case IntentIntegrator.REQUEST_CODE:
				if (resultCode == RESULT_OK) {
					IntentResult intentResult = IntentIntegrator // preserve new line
						.parseActivityResult(requestCode, resultCode, data);

					if (intentResult != null) {
						Uri uri = Uri.parse(intentResult.getContents());

						if ("clpbrd".equals(uri.getScheme())) {
							ProgressDialog progress = new ProgressDialog(this,
								R.style.ProgressDialog_Generic);
							{
								progress.setCancelable(false);
								progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
								progress.setMessage(
									getString(R.string.title_dialog_progress_connecting));
							}
							progress.show();

							CopyPasteService.registerCallbackReceiver(this,
								new BroadcastReceiver() {
									@Override
									public void onReceive(Context context, Intent intent) {
										CopyPasteService.unregisterCallbackReceiver(context, this);

										int status = intent.getIntExtra(
											CopyPasteService.CALLBACK_EXTRA_CLIENT_CONNECTED, -1);

										if (status == 200) {
											transitionServiceEnabledState();
											transitionMagicHintOut();
										}
										else {
											bringMagicHintIn();
											displayServiceDisabledState();

											new AlertDialog.Builder(MainActivity.this).setTitle(
												R.string.title_dialog_error_connection)
												.setMessage(getString(
													R.string.message_dialog_error_connection,
													getString(R.string.application_name)))
												.setPositiveButton(R.string.button_positive_generic,
													null)
												.show();
										}
										progress.dismiss();
									}
								});

							getMainHandler().post(
								() -> CopyPasteService.startClient(this, uri.getAuthority(),
									BuildConfig.SERVER_PORT));
						}
						else {
							new AlertDialog.Builder(this).setTitle(
								R.string.title_dialog_error_bad_qr)
								.setMessage(getString(R.string.message_dialog_error_bad_qr,
									getString(R.string.application_name)))
								.setPositiveButton(R.string.button_positive_generic, null)
								.show();
						}
					}
				}
				break;
		}
	}

	private PointF takeCenter(View v) {
		return new PointF(v.getX() + v.getWidth() / 2.f, v.getY() + v.getHeight() / 2.f);
	}

	private PointF takeOrigin(View v) {
		return new PointF(v.getX(), v.getY());
	}
}
