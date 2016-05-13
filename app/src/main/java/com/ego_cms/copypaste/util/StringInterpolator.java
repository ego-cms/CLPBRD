package com.ego_cms.copypaste.util;

import android.text.TextUtils;

import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class StringInterpolator {

	public interface Rule {
		String apply(MatchResult match);
	}


	private final Map<String, Rule> rules;
	private final Pattern           pattern;

	protected StringInterpolator(Pattern pattern, Map<String, Rule> rules) {
		this.rules = rules;
		this.pattern = pattern;
	}


	protected abstract String getRuleKey(MatchResult matchResult);


	public CharSequence interpolate(CharSequence text) {
		if (!TextUtils.isEmpty(text)) {
			Matcher matcher = pattern.matcher(text);

			StringBuilder sb = new StringBuilder();

			int index = 0;
			while (matcher.find()) {
				MatchResult match = matcher.toMatchResult();
				Rule rule = rules.get(getRuleKey(match));

				String interpolatedValue = rule != null ? // preserve new line
					rule.apply(match) : match.group();

				sb.append(text.subSequence(index, match.start()));
				sb.append(interpolatedValue);

				index = match.end();
			}
			if (sb.length() > 0) {
				if (index < sb.length()) {
					sb.append(text.subSequence(index, text.length()));
				}
				return sb.toString();
			}
		}
		return text;
	}
}
