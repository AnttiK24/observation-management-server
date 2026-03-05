package com.o3.tests.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.o3.tests.client.StatusCode;

public class IsHttpSuccess extends TypeSafeMatcher<StatusCode> {
	@Override
	protected boolean matchesSafely(StatusCode statusCode) {
		return statusCode.getCode() >= 200 && statusCode.getCode() < 300;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("a status code between 200 and 299");
	}

	@Override
	public void describeMismatchSafely(StatusCode statusCode, Description mismatchDescription) {
		if (statusCode.getCode() == 404)
			mismatchDescription
					.appendText("The server responded with a 404 error. Make sure the context path is set correctly.");
		else
			mismatchDescription.appendText("was ").appendValue(statusCode);
	}

	public static Matcher<StatusCode> httpSuccess() {
		return new IsHttpSuccess();
	}
}
