package com.ego_cms.copypaste;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface KeyValueStorage {

	void clear(@NotNull String key);

	void store(@NotNull String key, @NotNull Object object);

	@Nullable
	<T> T load(@NotNull String key, @NotNull Class<T> clazz);

}
