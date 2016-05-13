package com.ego_cms.copypaste;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.ego_cms.copypaste.extension.CommonActivityExtension;

import org.intellij.lang.annotations.MagicConstant;

public class AppCompatActivityBase extends AppCompatActivity
	implements CommonActivityExtension.ExtensionProvider {

	private CommonActivityExtension extension = new CommonActivityExtension(this);


	@Override
	public Handler getMainHandler() {
		return extension.getMainHandler();
	}

	@Override
	public boolean scheduleTask(@MagicConstant(valuesFromClass = ActivityStates.class) int when,
		@NonNull Runnable task) {
		return extension.scheduleTask(when, task);
	}

	@Override
	public boolean isInState(@MagicConstant(valuesFromClass = ActivityStates.class) int state) {
		return extension.isInState(state);
	}

	@Override
	public void setOnBackPressedListener(OnBackPressedListener listener) {
		extension.setOnBackPressedListener(listener);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		extension.onCreate(savedInstanceState);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		extension.onPostCreate(savedInstanceState);
	}

	@Override
	protected void onStart() {
		super.onStart();
		extension.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		extension.onResume();
	}

	@Override
	protected void onPause() {
		extension.onPause();
		super.onPause();
	}

	@Override
	protected void onStop() {
		extension.onStop();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		extension.onDestroy();
		super.onDestroy();
	}


	@Override
	public void onBackPressed() {
		if (!extension.onBackPressed()) {
			super.onBackPressed();
		}
	}

}
