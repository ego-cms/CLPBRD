package com.ego_cms.copypaste;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SharedKeyValueStorage implements KeyValueStorage {

	private final SharedPreferences storage;
	private final Gson              gson;

	public SharedKeyValueStorage(SharedPreferences storage) {
		this.storage = storage;
		this.gson = new GsonBuilder()
			.create();
	}


	@Override
	@SuppressLint("CommitPrefEdits")
	public void clear(@NotNull String key) {
		storage.edit()
			.remove(key)
			.commit();
	}

	@Override
	@SuppressLint("CommitPrefEdits")
	public void store(@NotNull String key, @NotNull Object object) {
		storage.edit()
			.putString(key, gson.toJson(object))
			.commit();
	}

	@Override
	@Nullable
	public <T> T load(@NotNull String key, @NotNull Class<T> clazz) {
		String objectJson = storage.getString(key, null);

		if (objectJson != null) {
			return gson.fromJson(objectJson, clazz);
		}
		return null;
	}
}
