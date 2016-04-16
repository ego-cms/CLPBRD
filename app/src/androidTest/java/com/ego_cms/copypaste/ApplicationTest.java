package com.ego_cms.copypaste;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.ego_cms.copypaste.util.AndroidCommonUtils;

import junit.framework.Assert;

import java.util.regex.Pattern;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
	public ApplicationTest() {
		super(Application.class);
	}


	@Override
	protected void setUp() throws Exception {
		super.setUp();

		createApplication();
	}


	public void testTextInterpolationFromResourcesToHTML() throws Exception {
		String text = "Color #1 is @color/debugRed; Phrase #1 is @string/debug_text_phrase1\n"
			+ "Color #2 is @color/debugGreen; Phrase #2 is @string/debug_text_phrase2\n"
			+ "Color #3 is @color/debugBlue; Phrase #2 is @string/debug_text_phrase3\n"
			+ "Android Color Transparent: @android:color/transparent\n"
			+ "Android Color While: @android:color/white";

		String expected = "Color #1 is rgba(255, 0, 0, 1); Phrase #1 is phrase1\n"
			+ "Color #2 is rgba(0, 255, 0, 0.059); Phrase #2 is phrase2\n"
			+ "Color #3 is rgba(0, 0, 255, 1); Phrase #2 is phrase3\n"
			+ "Android Color Transparent: rgba(0, 0, 0, 0)\n"
			+ "Android Color While: rgba(255, 255, 255, 1)";

		CharSequence result = AndroidCommonUtils.interpolateTextFromResourcesToHTML(text, getContext());
		Assert.assertFalse(Pattern.compile("(?:(\\w+):)?(\\w+)\\/(\\w+)").matcher(result).find());
		Assert.assertEquals(result, expected);
	}
}