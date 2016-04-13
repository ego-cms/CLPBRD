package com.ego_cms.copypaste.util;

public interface Delegate<T> {
	void invoke(T target);
}
