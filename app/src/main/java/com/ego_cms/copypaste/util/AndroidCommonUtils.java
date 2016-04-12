package com.ego_cms.copypaste.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AndroidCommonUtils {

	public static String makeIntentActionName(@NotNull Class<?> cls, @NotNull String actionName) {
		return String.format("%s.action.%s", cls.getName(), actionName.replaceAll("[^\\w\\d]", "_")
			.toUpperCase());
	}

	public static String makeIntentExtraName(@NotNull Class<?> cls, @NotNull String extraName) {
		return String.format("%s.EXTRA_%s", cls.getName(), extraName.replaceAll("[^\\w\\d]", "_")
			.toUpperCase());
	}


	// Context helper methods

	public static int getColorFrom(@NonNull Context context, @ColorRes int colorResId) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return context.getResources()
				.getColor(colorResId);
		}
		return context.getResources()
			.getColor(colorResId, context.getTheme());
	}

	@SuppressWarnings("deprecation")
	public static Drawable getDrawableFrom(@NotNull Context context,
		@DrawableRes int drawableResId) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			TypedValue value = new TypedValue();
			{
				Resources resources = context.getResources();

				resources.getValue(drawableResId, value, true);
				return resources.getDrawable(value.resourceId);
			}
		}
		else {
			return context.getDrawable(drawableResId);
		}
	}

	private static boolean hasNavigationBar(Resources resources) {
		//Emulator
		if (!Build.FINGERPRINT.startsWith("generic")) {
			int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");

			return id != 0 && resources.getBoolean(id);
		}
		return true;
	}

	public static boolean hasNavigationBar(@NotNull Context context) {
		return hasNavigationBar(context.getResources());
	}


	private static boolean hasTranslucentDecor(Resources resources) {
		int id = resources.getIdentifier("config_enableTranslucentDecor", "bool", "android");

		return id != 0 && resources.getBoolean(id);
	}

	public static boolean hasTranslucentDecor(@NotNull Context context) {
		return hasTranslucentDecor(context.getResources());
	}


	public static int getNavigationBarHeight(@NotNull Context context, int defaultHeight) {
		Resources resources = context.getResources();

		int id = resources.getIdentifier(
			resources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
				? "navigation_bar_height"
				: "navigation_bar_height_landscape", "dimen", "android");
		if (id != 0) {
			return hasNavigationBar(resources) ? resources.getDimensionPixelSize(id) : 0;
		}
		return defaultHeight;
	}

	public static int getStatusBarHeight(@NotNull Context context, int defaultHeight) {
		Resources resources = context.getResources();

		int id = resources.getIdentifier("status_bar_height", "dimen", "android");
		if (id != 0) {
			return resources.getDimensionPixelSize(id);
		}
		return defaultHeight;
	}


	// View helper methods

	public static void removeGlobalLayoutListener(@NotNull ViewTreeObserver vto,
		@Nullable ViewTreeObserver.OnGlobalLayoutListener listener) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
			vto.removeOnGlobalLayoutListener(listener);
		}
		else {
			vto.removeGlobalOnLayoutListener(listener);
		}
	}

	public static void setBackgroundDrawable(@NotNull View view, @Nullable Drawable drawable) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			view.setBackgroundDrawable(drawable);
		}
		else {
			view.setBackground(drawable);
		}
	}

	public static void setBackgroundDrawable(@NotNull View view, @DrawableRes int drawableResId) {
		setBackgroundDrawable(view, getDrawableFrom(view.getContext(), drawableResId));
	}

	public static boolean removeViewFromParent(@NotNull View v) {
		ViewParent parent = v.getParent();

		if (parent instanceof ViewGroup) {
			((ViewGroup) parent).removeView(v);

			return true;
		}
		return false;
	}

	public static boolean removeViewFromParentInLayout(@NotNull View v) {
		ViewParent parent = v.getParent();

		if (parent instanceof ViewGroup) {
			((ViewGroup) parent).removeViewInLayout(v);

			return true;
		}
		return false;
	}


	public static void startTransition(@NotNull View v, int duration) {
		TransitionDrawable transition = extractTransitionDrawable(v);

		if (transition != null) {
			if (duration > 0) {
				transition.startTransition(duration);
			}
			else if (duration < 0) {
				transition.reverseTransition(-duration);
			}
		}
	}


	@Nullable
	public static TransitionDrawable extractTransitionDrawable(@NotNull View v) {
		Drawable background = v.getBackground();

		if (background != null) {
			Drawable backgroundPrevious;

			do {
				backgroundPrevious = background;

				if (background instanceof TransitionDrawable) {
					return (TransitionDrawable) background;
				}
				background = background.getCurrent();
			} while (background != backgroundPrevious);
		}
		return null;
	}


	private AndroidCommonUtils() {
		/* Prevent instantiating */
	}
}
