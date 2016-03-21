package com.ego_cms.copypaste.util;

import org.intellij.lang.annotations.MagicConstant;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Lazy<T> implements LazyInitializer<T> {

	public static final int LAZY_TRIVIAL = 0;
	public static final int LAZY_SYNCHRONIZED = 1;
	public static final int LAZY_SYNCHRONIZED_LOCK_FREE = 2;


	protected abstract T initialize();


	public T get() {
		return lazy.get();
	}

	public boolean isEmpty() {
		return lazy.isEmpty();
	}


	private final LazyImpl<T> lazy;

	public Lazy(@MagicConstant(valuesFromClass = Lazy.class) int type) {
		switch (type) {
			case LAZY_TRIVIAL:
				lazy = new LazyTrivialImpl();
				break;

			case LAZY_SYNCHRONIZED:
				lazy = new LazySynchronizedImpl();
				break;

			case LAZY_SYNCHRONIZED_LOCK_FREE:
				lazy = new LazySynchronizedLockFreeImpl();
				break;

			default:
				throw new IllegalArgumentException("Unknown Lazy type.");
		}
	}

	public Lazy() {
		this(LAZY_SYNCHRONIZED);
	}


	private interface LazyImpl<T> {
		T get();

		boolean isEmpty();
	}

	private class LazyTrivialImpl implements LazyImpl<T> {

		private T instance;

		public T get() {
			if (Lazy.this.isEmpty(instance)) {
				instance = initialize();
			}
			return instance;
		}

		@Override
		public boolean isEmpty() {
			return Lazy.this.isEmpty(instance);
		}
	}

	private class LazySynchronizedImpl implements LazyImpl<T> {

		private volatile T instance;

		public T get() {
			T instanceLocal = instance;

			if (Lazy.this.isEmpty(instanceLocal)) {
				synchronized (this) {
					instanceLocal = instance;

					if (Lazy.this.isEmpty(instanceLocal)) {
						instance = instanceLocal = initialize();
					}
				}
			}
			return instanceLocal;
		}

		@Override
		public boolean isEmpty() {
			return Lazy.this.isEmpty(instance);
		}
	}

	private class LazySynchronizedLockFreeImpl implements LazyImpl<T> {

		private volatile T instance;

		private final AtomicBoolean isInitialized = new AtomicBoolean(false);

		public T get() {
			while (Lazy.this.isEmpty(instance)) {
				if (isInitialized.compareAndSet(false, true)) {
					instance = initialize();
				}
			}
			return instance;
		}

		@Override
		public boolean isEmpty() {
			return Lazy.this.isEmpty(instance);
		}
	}

	boolean isEmpty(T value) {
		return value == null;
	}
}
