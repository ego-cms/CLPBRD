package com.ego_cms.copypaste.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import org.jetbrains.annotations.NotNull;

public class QueueThread extends HandlerThread {
	public QueueThread(String name) {
		super(name);
	}

	public QueueThread(String name, int priority) {
		super(name, priority);
	}


	@NotNull
	protected Handler onCreateHandler(@NotNull Looper looper) {
		return new Handler(looper);
	}


	private volatile Handler handler;

	public final Handler getHandler() {
		Handler handlerLocal = handler;

		if (handlerLocal == null) {
			synchronized (this) {
				handlerLocal = handler;

				if (handlerLocal == null) {
					if (!isAlive())
						start();

					final Looper myLooper = getLooper();

					assert myLooper != null;
					handlerLocal = handler = onCreateHandler(myLooper);

					if (!myLooper.equals(handlerLocal.getLooper()))
						throw new Error("You are not allowed to use any Looper except the provided one!");
				}
			}
		}
		return handlerLocal;
	}

	private void dropHandler() {
		handler = null;
	}


	@Override
	public boolean quit() {
		dropHandler();
		return super.quit();
	}

	@Override
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public boolean quitSafely() {
		dropHandler();
		return super.quitSafely();
	}


	protected void onLooperQuit() {
		/* onLooperQuit method stub */
	}

	@Override
	public void run() {
		super.run();

		onLooperQuit();
	}
}
