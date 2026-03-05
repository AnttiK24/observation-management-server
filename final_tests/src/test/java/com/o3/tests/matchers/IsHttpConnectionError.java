package com.o3.tests.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.o3.tests.client.StatusCode;

public class IsHttpConnectionError extends TypeSafeMatcher<StatusCode> {
	@Override
	protected boolean matchesSafely(StatusCode statusCode) {
		return statusCode.getCode() == -1;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("a status code of -1 (Connection error)");
	}

	@Override
	public void describeMismatchSafely(StatusCode statusCode, Description mismatchDescription) {
		mismatchDescription.appendText("was ").appendValue(statusCode);
	}

	public static Matcher<StatusCode> httpConnectionError() {
		return new IsHttpConnectionError();
	}

}
