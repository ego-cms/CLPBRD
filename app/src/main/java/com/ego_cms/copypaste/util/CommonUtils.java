package com.ego_cms.copypaste.util;

import java.util.Enumeration;
import java.util.Iterator;

public class CommonUtils {

	public static boolean toPrimitive(Boolean bool, boolean fallback) {
		return bool != null ? bool : fallback;
	}

	public static <E> Iterable<E> asIterable(Enumeration<E> enumeration) {
		return () -> new Iterator<E>() {
			@Override
			public boolean hasNext() {
				return enumeration.hasMoreElements();
			}

			@Override
			public E next() {
				return enumeration.nextElement();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}


	private CommonUtils() {
		/* Nothing to do */
	}
}
