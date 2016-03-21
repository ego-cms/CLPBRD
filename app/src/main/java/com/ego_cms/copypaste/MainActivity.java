package com.ego_cms.copypaste;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.ego_cms.copypaste.util.CommonUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import solid.functions.SolidFunc1;
import solid.stream.Stream;

public class MainActivity extends AppCompatActivity
	implements ClipboardManager.OnPrimaryClipChangedListener {

	private static final String TAG = "MainActivity";


	@Bind(R.id.switch_service)
	SwitchCompat switchService;

	@Bind(R.id.text_server_address)
	TextView textServerAddress;

	@Bind(R.id.text_clip_label)
	TextView textClipLabel;

	@Bind(R.id.text_clip_mime_types_count)
	TextView textClipMimeTypesCount;

	@Bind(R.id.text_clip_mime_types)
	TextView textClipMimeTypes;

	@Bind(R.id.list_clip_items)
	ListView listClipItems;


	private void updateClipDataDisplay() {
		ClipData clipData = ((ClipboardManager) getSystemService(
			CLIPBOARD_SERVICE)).getPrimaryClip();
		ClipDescription description = clipData.getDescription();

		StringBuilder sb = new StringBuilder();

		for (int i = 0, imax = description.getMimeTypeCount(); i < imax; ++i) {
			sb.append(description.getMimeType(i))
				.append(",\n");
		}
		int length = sb.length();

		if (length > 2) {
			sb.delete(length - 2, length);
		}
		textClipLabel.setText(description.getLabel());
		textClipMimeTypesCount.setText(
			getString(R.string.label_clip_mime_count, description.getMimeTypeCount()));
		textClipMimeTypes.setText(sb.toString());

		ListAdapter adapter;
		{
			final String source[] = new String[]{
				"content",
				"type"
			};
			final int target[] = new int[]{
				android.R.id.text1,
				android.R.id.text2
			};
			Iterable<ClipData.Item> clipItems = () -> new Iterator<ClipData.Item>() {

				int index;

				@Override
				public boolean hasNext() {
					return index < clipData.getItemCount();
				}

				@Override
				public ClipData.Item next() {
					return clipData.getItemAt(index++);
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
			adapter = new SimpleAdapter(this, Stream.stream(clipItems)
				.map((SolidFunc1<ClipData.Item, Map<String, ?>>) value -> {
					Map<String, Object> result = new HashMap<>();
					{
						CharSequence content;

						if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
							content = value.coerceToStyledText(this);
						}
						else {
							content = value.coerceToText(this);
						}
						result.put(source[0], content);

						String type;
						{
							if (value.getUri() != null) {
								type = getString(R.string.label_clip_item_type,
									getString(R.string.label_clip_item_type_uri) + " " + getString(
										R.string.label_clip_item_coerced));
							}
							else if (value.getIntent() != null) {
								type = getString(R.string.label_clip_item_type,
									getString(R.string.label_clip_item_type_intent) + " "
										+ getString(R.string.label_clip_item_coerced));
							}
							else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN
								&& value.getHtmlText() != null) {

								type = getString(R.string.label_clip_item_type,
									getString(R.string.label_clip_item_type_html) + " " + getString(
										R.string.label_clip_item_coerced));
							}
							else {
								type = getString(R.string.label_clip_item_type,
									getString(R.string.label_clip_item_type_text));
							}
						}
						result.put(source[1], type);
					}
					return result;
				})
				.toSolidList(), R.layout.item_clip_item, source, target);
		}
		listClipItems.setAdapter(adapter);
	}


	private static final String KEY_SERVICE_IS_RUNNING = TAG + ".keyServiceIsRunning";

	private void initializeView() {
		KeyValueStorage kvs = CopyPasteApplication.get(this)
			.getCommonKeyValueStorage();

		switchService.setChecked(
			CommonUtils.toPrimitive(kvs.load(KEY_SERVICE_IS_RUNNING, Boolean.class), false));
		switchService.setOnCheckedChangeListener((buttonView, isChecked) -> {
			if (isChecked) {
				CopyPasteService.start(this);
				textServerAddress.setVisibility(View.VISIBLE);
			}
			else {
				CopyPasteService.stop(this);
				textServerAddress.setVisibility(View.GONE);
			}
			kvs.store(KEY_SERVICE_IS_RUNNING, isChecked);
		});
		textServerAddress.setText(String.format(Locale.US, "http://%s:%d", CopyPasteService.getNetworkAddress(),
			BuildConfig.SERVER_PORT));

		updateClipDataDisplay();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);
		initializeView();

		((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)) // preserve new line
			.addPrimaryClipChangedListener(this);
	}

	@Override
	protected void onDestroy() {
		((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)) // preserve new line
			.removePrimaryClipChangedListener(this);

		ButterKnife.unbind(this);
		super.onDestroy();
	}

	@Override
	public void onPrimaryClipChanged() {
		updateClipDataDisplay();
		Log.d(TAG, ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).getPrimaryClip()
			.toString());
	}


}
