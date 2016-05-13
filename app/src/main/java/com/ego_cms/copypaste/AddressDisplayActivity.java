package com.ego_cms.copypaste;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import butterknife.Bind;
import butterknife.ButterKnife;

public class AddressDisplayActivity extends Activity {

	private static final String EXTRA_QR_CODE = "AddressDisplayActivity.extraQRCode";

	public static void startAsync(@NonNull Activity context, View transitionView,
		@NonNull String address) {

		new AsyncTask<String, Void, Bitmap>() {

			@Override
			protected Bitmap doInBackground(String... params) {
				try {
					int size;
					{
						DisplayMetrics dm = context.getResources()
							.getDisplayMetrics();

						size = Math.round(0.16f * Math.min(dm.widthPixels, dm.heightPixels));
					}
					BitMatrix code = new QRCodeWriter().encode(params[0], // preserve new line
						BarcodeFormat.QR_CODE, size, size);
					Bitmap result = Bitmap.createBitmap(code.getWidth(), code.getHeight(),
						Bitmap.Config.RGB_565);

					for (int x = 0, xmax = code.getWidth(); x < xmax; ++x) {
						for (int y = 0, ymax = code.getHeight(); y < ymax; ++y) {
							result.setPixel(x, y, code.get(x, y) ? Color.BLACK : Color.WHITE);
						}
					}
					return result;
				}
				catch (WriterException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			protected void onPostExecute(Bitmap bitmap) {
				if (!context.isFinishing()) {
					Intent result = new Intent(context, AddressDisplayActivity.class);
					{
						result.putExtra(EXTRA_QR_CODE, bitmap);
					}
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						ActivityOptionsCompat options = ActivityOptionsCompat.
							makeSceneTransitionAnimation(context, transitionView, "qr");

						context.startActivity(result, options.toBundle());
					}
					else {
						context.startActivity(result);
					}
				}
			}
		}.execute(address);
	}


	@Bind(R.id.image_qr_code)
	ImageView imageQRCode;

	@Bind(R.id.text_qr_hint)
	TextView textQRHint;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestTranslucentBarsFor(this);
		setContentView(R.layout.activity_address_display);
		ButterKnife.bind(this);

		textQRHint.setText(String.format(textQRHint.getText()
			.toString(), getString(R.string.application_name)));
		imageQRCode.setImageBitmap(getIntent() // preserve new line
			.getParcelableExtra(EXTRA_QR_CODE));
	}

	private static boolean requestTranslucentBarsFor(Activity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			Window window = activity.getWindow();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				Resources resources = activity.getResources();

				int id = resources.getIdentifier("config_enableTranslucentDecor", "bool",
					"android");
				if (id != 0 && resources.getBoolean(id)) {
					window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
				}
			}
			window.getDecorView()
				.setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

			return true;
		}
		return false;
	}
}
