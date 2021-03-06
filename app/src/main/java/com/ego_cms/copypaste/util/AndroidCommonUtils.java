package com.ego_cms.copypaste.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public final class AndroidCommonUtils {

	public static String makeIntentActionName(@NotNull Class<?> cls, @NotNull String actionName) {
		return String.format("%s.action.%s", cls.getName(), actionName.replaceAll("[^\\w\\d]", "_")
			.toUpperCase());
	}

	public static String makeIntentExtraName(@NotNull Class<?> cls, @NotNull String extraName) {
		return String.format("%s.EXTRA_%s", cls.getName(), extraName.replaceAll("[^\\w\\d]", "_")
			.toUpperCase());
	}


	public static CharSequence interpolateTextFromResourcesToHTML(CharSequence text,
		Context context) {
		Map<String, StringInterpolator.Rule> rules = new HashMap<>();
		{
			NumberFormat decimalFormat = NumberFormat.getInstance(Locale.US);
			rules.put("color", new AndroidResourcesInterpolatorRule(context) {
				@Override
				protected String getResourceValueText(int resourceId, String type) {
					final int color = AndroidCommonUtils.getColorFrom(context, resourceId);

					final int r = Color.red(color);
					final int g = Color.green(color);
					final int b = Color.blue(color);
					final int a = Color.alpha(color);

					return String.format(Locale.US, "rgba(%d, %d, %d, %1.3f)",
						r, g, b, Math.min(a / 255.f, 1));
				}
			});
			rules.put("string", new AndroidResourcesInterpolatorRule(context) {
				@Override
				protected String getResourceValueText(int resourceId, String type) {
					return context.getString(resourceId);
				}
			});
		}
		return new StringInterpolator(Pattern.compile("@(?:(\\w+):)?(\\w+)\\/(\\w+)"), rules) {

			@Override
			protected String getRuleKey(MatchResult matchResult) {
				return matchResult.group(2);
			}
		}.interpolate(text);
	}

	private static abstract class AndroidResourcesInterpolatorRule
		implements StringInterpolator.Rule {

		protected final Context context;

		public AndroidResourcesInterpolatorRule(Context context) {
			this.context = context;
		}


		protected abstract String getResourceValueText(int resourceId, String type);


		@Override
		public String apply(MatchResult match) {
			String namespace = match.group(1);

			if (TextUtils.isEmpty(namespace)) {
				namespace = context.getPackageName();
			}
			String type = match.group(2);

			return getResourceValueText(context.getResources()
				.getIdentifier(match.group(3), type, namespace), type);
		}
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
