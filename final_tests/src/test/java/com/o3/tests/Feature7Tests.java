package com.o3.tests;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static com.o3.tests.matchers.IsHttpClientError.httpClientError;
import static com.o3.tests.matchers.IsHttpSuccess.httpSuccess;

import com.o3.tests.client.StatusCode;
import com.o3.tests.client.User;
import com.o3.tests.messages.MessageHorizons;
import com.o3.tests.messages.Observatory;
import com.o3.tests.messages.OrbitalElements;
import com.o3.tests.messages.StateVector;

class Feature7Tests extends TestBase {
	@Test
	@DisplayName("Test datarecord get returns ID")
	@Order(1)
	void testDatarecordHasId() throws InterruptedException {
		logger.info("Testing if ID field is present in datarecord");
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());

		// Send a message to be retrieved later
		MessageHorizons message = new MessageHorizons(
			Helpers.generateRandomString(1),
			Helpers.generateRandomString(1),
			ZonedDateTime.now(),
			Helpers.createStateVector(),
			Helpers.generateRandomString(8) // Add payload
		);

		StatusCode result = testClient.sendMessage(message.toJsonString());
		assertThat(result.getBody(), result, is(httpSuccess()));

		StatusCode response = testClient.getMessages();
		assertThat(response.getBody(), response, is(httpSuccess()));
		String messages = response.getBody();

		JSONArray messageArray = new JSONArray(messages);

		// Specify "id" as a mandatory field for this feature
		String[] fields = { "id" };
		assertTrue(messagesContainFields(messageArray, fields));
	}

	@Test
	@DisplayName("Test messages should be able to be updated")
	@Order(2)
	void testUpdateMessages() throws InterruptedException {
		logger.info("Testing if messages are able to be updated");
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());

		// Send a message to be retrieved later
		MessageHorizons message = new MessageHorizons(
			Helpers.generateRandomString(1),
			Helpers.generateRandomString(1),
			ZonedDateTime.now(),
			Helpers.createStateVector(),
			Helpers.generateRandomString(8) // Add payload
		);

		StatusCode result = testClient.sendMessage(message.toJsonString());
		assertThat(result.getBody(), result, is(httpSuccess()));

		StatusCode response = testClient.getMessages();
		assertThat(response.getBody(), response, is(httpSuccess()));
		String messages = response.getBody();
		JSONArray messageArray = new JSONArray(messages);

		// Get the final record returned
		JSONObject messageToUpdate = messageArray.getJSONObject(messageArray.length() - 1);
		int messageId = messageToUpdate.getJSONObject("metadata").getInt("id");

		//Add observatory to update
		Observatory observatory = new Observatory(
				Helpers.generateRandomString(4),
				random.nextDouble(0, 90),
				random.nextDouble(0, 180));
		ArrayList<Observatory> observatories = new ArrayList<>() {
			{
				add(observatory);
			}
		};

		// Update some fields
		messageToUpdate.put("target_body_name", "Tatooine");
		messageToUpdate.put("center_body_name", "Tatoo I & II");
		messageToUpdate.put("state_vector", new JSONObject(Helpers.createStateVector()));
		messageToUpdate.getJSONObject("metadata").put("observatory", new JSONArray(observatories));

		// Remove ID from the content
		messageToUpdate.getJSONObject("metadata").remove("id");

		// Test update with incorrect/missing id field in query
		StatusCode updateResult = testClient.updateMessage(messageToUpdate.toString(), "incorrectID=" + messageId);
		assertThat(updateResult, is(httpClientError()));

		// Test update with incorrect number of query parameters
		updateResult = testClient.updateMessage(messageToUpdate.toString(), "id=" + messageId + "&anotherParameter=value");
		assertThat(updateResult, is(httpClientError()));

		// Update with correct id field
		updateResult = testClient.updateMessage(messageToUpdate.toString(), "id=" + messageId);
		assertThat(updateResult, is(httpSuccess()));

		// Test that returned message has been updated
		response = testClient.getMessages();
		String messagesAfterUpdate = response.getBody();
		assertThat(response.getBody(), response, is(httpSuccess()));

		JSONArray updatedMessageArray = new JSONArray(messagesAfterUpdate);
		int indexOfUpdatedMessage = searchForMessage(updatedMessageArray, messageId);
		assertTrue(indexOfUpdatedMessage >= 0, "Server did not return updated message");
		JSONObject messageAfterUpdate = updatedMessageArray.getJSONObject(indexOfUpdatedMessage);

		CompareResult comparisonResult = compareJson(messageToUpdate, messageAfterUpdate, "id", "edited", "update_reason");

		assertTrue(comparisonResult.areEqual());
	}

	@Test
	@DisplayName("Test update reason and time is properly updated")
	@Order(3)
	void testUpdateReasonAndTime() throws InterruptedException {
		logger.info("Testing if messages are able to be updated");
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());

		// Send a message to be retrieved later
		StateVector vector = Helpers.createStateVector();
		MessageHorizons message = new MessageHorizons(
			Helpers.generateRandomString(1),
			Helpers.generateRandomString(1),
			ZonedDateTime.now(),
			new OrbitalElements(vector),
			Helpers.generateRandomString(8) // Add payload
		);

		StatusCode result = testClient.sendMessage(message.toJsonString());
		assertThat(result.getBody(), result, is(httpSuccess()));

		StatusCode response = testClient.getMessages();
		assertThat(response.getBody(), response, is(httpSuccess()));

		String messages = response.getBody();
		JSONArray messageArray = new JSONArray(messages);

		JSONObject messageToUpdate = messageArray.getJSONObject(messageArray.length() - 1);
		int messageId = messageToUpdate.getJSONObject("metadata").getInt("id");

		// Update some fields
		messageToUpdate.put("center_body_name", "Orb");
		messageToUpdate.getJSONObject("orbital_elements").put("inclination_deg", 12.443);
		String updateReason = "Testing: fixes";
		messageToUpdate.getJSONObject("metadata").put("update_reason", updateReason);
		// Remove locationID from the content
		messageToUpdate.remove("id");

		ZonedDateTime sendBegin = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		StatusCode updateResult = testClient.updateMessage(messageToUpdate.toString(), "id=" + messageId);
		ZonedDateTime sendEnd = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		assertThat(updateResult, is(httpSuccess()));

		response = testClient.getMessages();
		assertThat(response.getBody(), response, is(httpSuccess()));
		String allMessagesAfterUpdate = response.getBody();

		JSONArray updatedMessageArray = new JSONArray(allMessagesAfterUpdate);
		int indexOfUpdatedMessage = searchForMessage(updatedMessageArray, messageId);
		assertTrue(indexOfUpdatedMessage >= 0, "Updated message wasn't returned from server");
		JSONObject messageAfterUpdate = updatedMessageArray.getJSONObject(indexOfUpdatedMessage);

		assertTrue(messageAfterUpdate.getJSONObject("metadata").has("update_reason") && messageAfterUpdate.getJSONObject("metadata").has("edited"));

		ZonedDateTime timeModified = ZonedDateTime.parse(messageAfterUpdate.getJSONObject("metadata").getString("edited"),
				Constants.FORMATTER);

		logger.info(String.format("Expected record_time_received range: [%s, %s]",
				sendBegin.format(Constants.FORMATTER), sendEnd.format(Constants.FORMATTER)));
		assertTrue(timeModified.isAfter(sendBegin) && timeModified.isBefore(sendEnd));
		assertThat(messageAfterUpdate.getJSONObject("metadata").getString("update_reason"), is(updateReason));
	}

	@Test
	@DisplayName("Test user is datarecord owner")
	@Order(4)
	void testOnlyAllowUpdatesFromOwner() throws InterruptedException {
		logger.info("Testing if updates are only allowed from datarecord owner");
		User recordOwner = Helpers.createUser();
		User forbiddenUpdater = Helpers.createUser();
		registerJson(recordOwner);
		registerJson(forbiddenUpdater);
		testClient.setAuth(recordOwner.getUsername(), recordOwner.getPassword());

		StateVector vector = Helpers.createStateVector();
		MessageHorizons message = new MessageHorizons(
			Helpers.generateRandomString(1),
			Helpers.generateRandomString(1),
			ZonedDateTime.now(),
			new OrbitalElements(vector),
			vector,
			Helpers.generateRandomString(8) // Add payload
		);
		StatusCode result = testClient.sendMessage(message.toJsonString());
		assertThat(result.getBody(), result, is(httpSuccess()));

		testClient.setAuth(forbiddenUpdater.getUsername(), forbiddenUpdater.getPassword());
		StatusCode response = testClient.getMessages();
		assertThat(response.getBody(), response, is(httpSuccess()));
		String messages = response.getBody();
		JSONArray messageArray = new JSONArray(messages);

		JSONObject messageToUpdate = messageArray.getJSONObject(messageArray.length() - 1);
		int messageId = messageToUpdate.getJSONObject("metadata").getInt("id");

		// Update some fields with random strings
		StateVector updatedVector = Helpers.createStateVector();
		OrbitalElements updatedOE = new OrbitalElements(updatedVector);
		messageToUpdate.put("state_vector", new JSONObject(updatedVector));
		messageToUpdate.put("orbital_elements", new JSONObject(updatedOE));
		String updateReason = "Testing: Fixed typo";
		messageToUpdate.put("update_reason", updateReason);
		// Remove ID from the content
		messageToUpdate.getJSONObject("metadata").remove("id");

		// Check that missing id gets denied
		StatusCode updateResult = testClient.updateMessage(messageToUpdate.toString(), "id=" + messageId);
		assertThat(updateResult, is(httpClientError()));

		// Check that id not matching message owner gets denied
		messageToUpdate.put("id", messageId);
		updateResult = testClient.updateMessage(messageToUpdate.toString(), "id=" + messageId);
		assertThat(updateResult, is(httpClientError()));
	}

	int searchForMessage(JSONArray array, int messageId) {
		JSONObject currentObject;
		for (int i = 0; i < array.length(); ++i) {
			currentObject = array.getJSONObject(i);

			if (currentObject.getJSONObject("metadata").getInt("id") == messageId)
				return i;
		}

		return -1;
	}
}
