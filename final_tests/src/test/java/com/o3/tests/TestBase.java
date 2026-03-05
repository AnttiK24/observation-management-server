package com.o3.tests;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.o3.tests.client.Client;
import com.o3.tests.client.StatusCode;
import com.o3.tests.client.User;

import static com.o3.tests.matchers.IsHttpConnectionError.httpConnectionError;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestBase {
	protected Client testClient = null;
	protected Logger logger = Logger.getLogger(this.getClass().getName());
	protected static final Random random = new Random();

	@BeforeEach
	void resetAuth() throws KeyManagementException, NoSuchAlgorithmException {
		testClient = new Client(
				Constants.ENVIRONMENT.getOrDefault(
						Constants.SERVER_DEFAULT_ADDRESS_KEY,
						Constants.SERVER_DEFAULT_ADDRESS_HTTPS));
	}

	@Test
	@DisplayName("Test server connection")
	@Order(1)
	void testServerConnection() throws InterruptedException {
		logger.info(String.format("Testing server connection to `%s`...", testClient.getServerAddress()));
		StatusCode result = testClient.testConnection();
		logger.info(String.format("Status code: %s", result.toString()));
		assertThat(result.getBody(), result, is(not(httpConnectionError())));
	}

	/**
	 * Helper function for comparing JSON arrays, which can contain multiple types
	 * in 2026 instance
	 *
	 * @param expected
	 * @param actual
	 * @return
	 */

	CompareResult compareJsonArray(JSONArray expected, JSONArray actual, String... ignoreKeys) {
		CompareResult result = new CompareResult();
		String firstElementClass = expected.get(0).getClass().getSimpleName();
		if (firstElementClass.equals("Double")) {
			for (int i = 0; i < expected.length(); i++) {
				double expectedValue = expected.getDouble(i);
				double actualValue = actual.getDouble(i);
				if (expectedValue != actualValue)
					result.addDifference(
							String.format("Value for JSON Array does not match: Expected: `%f` Found: `%f`",
									expected.getDouble(i), actual.getDouble(i)));
			}

		} else if (firstElementClass.equals("JSONObject")) {
			// Add default case
			CompareResult result1 = new CompareResult();
			result1.addDifference("Object not found in JSONArray");

			for (int i = 0; i < expected.length(); i++) {
				JSONObject expectedObj = expected.getJSONObject(i);
				for (int j = 0; j < actual.length(); j++) {
					result1 = compareJson(expectedObj, actual.getJSONObject(j), ignoreKeys);
					if (result1.areEqual())
						break;
				}
			}
			for (String difference : result1.getDifferences()) {
				result.addDifference(difference);
			}
		}

		return result;
	}

	/**
	 * Make sure that the returned JSONObject contains the expected keys and values
	 * Extra keys in the actual JSONObject are ignored
	 *
	 * @param expected Expected JSONObject
	 * @param actual   Actual JSONObject
	 * @return true if the actual JSONObject contains the expected keys and values,
	 *         false otherwise
	 */
	CompareResult compareJson(JSONObject expected, JSONObject actual, String... ignoreKeys) {
		CompareResult result = new CompareResult();
		Set<String> allKeys = Helpers.mergeSets(expected.keySet(), actual.keySet());

		for (String key : allKeys) {
			if (Helpers.contains(ignoreKeys, key))
				continue;

			if (!actual.has(key)) {
				result.addDifference(String.format("Key `%s` is missing from the received JSON object", key));
			} else {
				try {
					Object value = expected.get(key);
					switch (value.getClass().getSimpleName()) {
						// Fuck java and its type system
						// Get me back to C land pls
						case "Double", "Float", "BigDecimal": {
							double expectedValue = expected.getDouble(key);
							double actualValue = actual.getDouble(key);
							if (expectedValue != actualValue)
								result.addDifference(
										String.format("Value for key `%s` does not match: Expected: `%f` Found: `%f`",
												key, expected.getDouble(key), actual.getDouble(key)));
							break;
						}
						case "Long", "Integer", "BigInteger": {
							long expectedValue = expected.getLong(key);
							long actualValue = actual.getLong(key);
							if (expectedValue != actualValue)
								result.addDifference(
										String.format("Value for key `%s` does not match: Expected: `%d` Found: `%d`",
												key, expected.getInt(key), actual.getInt(key)));
							break;
						}
						case "JSONObject": {
							JSONObject actualObject = actual.optJSONObject(key, null);
							if (actualObject == null) {
								result.addDifference(String.format("Key `%s` wasn't found in response", key));
							} else {
								CompareResult recResult = compareJson(expected.getJSONObject(key), actualObject,
										ignoreKeys);
								// Update result if nested objects were not equal
								for (String difference : recResult.getDifferences()) {
									result.addDifference(difference);
								}
							}
							break;
						}
						case "JSONArray": {
							CompareResult arrResult = compareJsonArray((JSONArray) value, actual.getJSONArray(key),
									ignoreKeys);
							for (String difference : arrResult.getDifferences()) {
								result.addDifference(difference);
							}
							break;
						}
						default: {
							if (!expected.get(key).equals(actual.get(key)))
								result.addDifference(
										String.format("Value for key `%s` does not match: Expected: `%s` Found: `%s`",
												key, expected.get(key), actual.get(key)));
						}
					}
				} catch (JSONException e) {
					result.addDifference(String.format("Error comparing key `%s`: %s", key, e.getMessage()));
				}
			}
		}

		return result;
	}

	/**
	 * Register a new user using a JSON object
	 *
	 * @param user User object with username and password
	 * @return HTTP response status code
	 * @throws InterruptedException
	 */
	StatusCode registerJson(User user) throws InterruptedException {
		return testClient.register(user.toJsonString());
	}

	/**
	 * Register a new user using plain text
	 *
	 * @param user User object with username and password
	 * @return HTTP response status code
	 * @throws InterruptedException
	 */
	StatusCode registerPlain(User user) throws InterruptedException {
		return testClient.register(String.format("%s:%s", user.getUsername(), user.getPassword()));
	}

	/**
	 * Format a test failure message with the expected message, closest message and
	 * mismatches
	 *
	 * @param expected String containing formatted expected message
	 * @param actual   String containing the closest message found on the server
	 * @param result   Compareresult Differences between expected and actual message
	 */
	String formatFailureMessage(String expected, String actual, CompareResult result) {
		return String.format(
				"Expected to find message:\n%s\nClosest message returned by the server was:\n%s.\nDifferences were:\n\t%s\n",
				expected.replaceFirst(Constants.IMAGE_DATA_REGEX, Constants.IMAGE_DATA_PLACEHOLDER),
				actual.replaceFirst(Constants.IMAGE_DATA_REGEX, Constants.IMAGE_DATA_PLACEHOLDER),
				result.toString());
	}

	void checkJsonResponse(JSONObject message, String messages, ZonedDateTime sendBegin, ZonedDateTime sendEnd) {
		int mindiff, minIndex;
		minIndex = 0;
		mindiff = Integer.MAX_VALUE;
		CompareResult closestResult = new CompareResult();
		try {
			JSONArray messageArray = new JSONArray(messages);
			if (System.getProperty("output.silent") == null)
				logger.info(String.format("Messages:%n%s",
						messageArray.toString(4)/*
																		 * .replaceAll(Constants.IMAGE_DATA_REGEX, Constants.IMAGE_DATA_PLACEHOLDER)
																		 */));

			boolean found = false;
			for (int i = 0; i < messageArray.length(); i++) {
				JSONObject msg = messageArray.getJSONObject(i);
				ZonedDateTime timeReceived = ZonedDateTime.now();

				if (msg.has("metadata"))
					timeReceived = ZonedDateTime.parse(msg.getJSONObject("metadata").getString("record_time_received"),
							Constants.FORMATTER);
				CompareResult result1 = compareJson(message, msg, "record_time_received", "id", "weather");
				if (result1.getDifferences().size() < mindiff) {
					mindiff = result1.getDifferences().size();
					minIndex = i;
					closestResult = result1;
				}
				if (result1.areEqual()) {
					if (timeReceived.isAfter(sendBegin) && timeReceived.isBefore(sendEnd)) {
						found = true;
						break;
					} else {
						logger.warning(
								String.format("Message found, but timestamp is not within the expected range:%n%s",
										msg.toString(4)));
					}
				} else {
					logger.fine(result1.getDifferences().toString());
				}
			}
			String closestMessage = messageArray.getJSONObject(minIndex).toString(4);
			assertTrue(found, formatFailureMessage(message.toString(4), closestMessage, closestResult));
		} catch (JSONException e) {
			logger.severe(String.format("Error parsing JSON response:%n%s", messages));
			throw e;
		} catch (DateTimeParseException e) {
			logger.severe(String.format("Error parsing timestamp in JSON response:%n%s%n%s", e.getMessage(), messages));
			throw e;
		}
	}

	/*
	 * Check if all given messages contain all specified fields
	 *
	 * @param messages JSONArray of messages
	 *
	 * @param fields Array of strings specifying the fields that
	 * should be found
	 */
	boolean messagesContainFields(JSONArray messages, String[] fields) {
		for (int i = 0; i < messages.length(); ++i) {
			for (int j = 0; j < fields.length; ++j) {
				JSONObject message = messages.getJSONObject(i);
				if (!message.has(fields[j])) {
					if (!message.has("metadata") && !message.getJSONObject("metadata").has(fields[j])) {
						return false;
					}
				}
			}
		}

		return true;
	}

}
