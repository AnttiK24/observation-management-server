package com.o3.tests.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.o3.tests.client.StatusCode;

public class IsHttpClientError extends TypeSafeMatcher<StatusCode> {
	@Override
	protected boolean matchesSafely(StatusCode statusCode) {
		return statusCode.getCode() >= 400 && statusCode.getCode() < 500;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("a status code between 400 and 499");
	}

	@Override
	public void describeMismatchSafely(StatusCode statusCode, Description mismatchDescription) {
		mismatchDescription.appendText("was ").appendValue(statusCode);
	}

	public static Matcher<StatusCode> httpClientError() {
		return new IsHttpClientError();
	}
}
