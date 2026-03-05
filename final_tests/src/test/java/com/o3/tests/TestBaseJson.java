package com.o3.tests;

import static com.o3.tests.matchers.IsHttpClientError.httpClientError;
import static com.o3.tests.matchers.IsHttpSuccess.httpSuccess;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;

import com.o3.tests.client.StatusCode;
import com.o3.tests.client.User;
import com.o3.tests.messages.MessageHorizons;
import com.o3.tests.messages.Metadata;
import com.o3.tests.messages.OrbitalElements;
import com.o3.tests.messages.StateVector;

class TestBaseJson extends TestBase {
	@Test
	@DisplayName("Test registering a user")
	@Order(2)
	void testRegisterUser() throws InterruptedException {
		logger.info("Testing registering a user...");
		StatusCode result = registerJson(Helpers.createUser());
		assertThat(result.getBody(), result, is(httpSuccess()));
	}

	@Test
	@DisplayName("Test registering a user with an existing username")
	@Order(3)
	void testRegisterExistingUser() throws InterruptedException {
		logger.info("Testing registering a user with an existing username...");
		User user = Helpers.createUser();

		registerJson(user);
		StatusCode result = registerJson(user);
		assertThat(result.getBody(), result, is(httpClientError()));
	}

	@Test
	@DisplayName("Test sending GET request to the registration endpoint")
	@Order(3)
	void testGetRequestToRegistrationEndpoint() throws InterruptedException {
		logger.info("Testing sending a GET request to the registration endpoint...");

		StatusCode result = testClient.getRegister();
		assertThat(result.getBody(), result, is(httpClientError()));
	}

	@Test
	@DisplayName("Test sending a message without authentication")
	@Order(3)
	void testSendMessageWithoutAuth() throws InterruptedException {
		logger.info("Testing sending a message without authentication to the server");

		StateVector vector = Helpers.createStateVector();
		MessageHorizons message = new MessageHorizons(
				Helpers.generateRandomString(1),
				Helpers.generateRandomString(1),
				ZonedDateTime.now(),
				new OrbitalElements(vector),
				vector);
		logger.info(String.format("Created message:%n%s", message.toJsonString(4)));

		StatusCode result = testClient.sendMessage(message.toJsonString());
		assertThat(result.getBody(), result, is(httpClientError()));
	}

	@RepeatedTest(63)
	@DisplayName("Test sending a message with missing fields")
	@Order(4)
	void testFaultyMessage1(RepetitionInfo repetitionInfo) throws InterruptedException {
		int repetition = repetitionInfo.getCurrentRepetition();
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());
		logger.info("Testing sending a message with missing fields to the server");

		MessageHorizons message = new MessageHorizons(
				(repetition & 0b000001) == 0 ? Helpers.generateRandomString(1) : null,
				(repetition & 0b000010) == 0 ? Helpers.generateRandomString(1) : null,
				(repetition & 0b000100) == 0 ? ZonedDateTime.now(ZoneOffset.UTC) : null);
		OrbitalElements elements = Helpers.createOrbitalElements();
		StateVector vector = OrekitOrbitConversions.orbitalElementsToStateVector(elements);
		message.setMetadata(new Metadata((String) null));

		if ((repetition & 0b001000) != 0) {
			elements.setArgumentOfPeriapsisDeg(null);
			elements.setEccentricity(null);
			elements.setInclinationDeg(null);
		}
		if ((repetition & 0b010000) != 0) {
			elements.setLongitudeAscendingNodeDeg(null);
			elements.setMeanAnomalyDeg(null);
			elements.setSemiMajorAxisAu(null);
		}
		if ((repetition & 0b100000) != 0)
			vector.setPositionAu(null);
		if ((repetition & 0b100000) != 0)
			vector.setVelocityAuPerDay(null);

		message.setOrbitalElements(elements);
		message.setStateVector(vector);
		logger.info(String.format("Iteration: %d, message: {%s}", repetition, message));

		StatusCode result = testClient.sendMessage(message.toJsonString());
		assertThat(String.format("Message: {%s}", message), result, is(httpClientError()));
	}

	@RepeatedTest(63)
	@DisplayName("Test sending a message with invalid fields")
	@Order(4)
	void testFaultyMessage2(RepetitionInfo repetitionInfo) throws InterruptedException {
		int repetition = repetitionInfo.getCurrentRepetition();
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());
		logger.info("Testing sending a message with invalid fields to the server");

		JSONObject message = new JSONObject();
		message.put("target_body_name",
				(repetition & 0b000001) == 0 ? Helpers.generateRandomString(1) : Helpers.random.nextDouble());
		message.put("center_body_name",
				(repetition & 0b000010) == 0 ? Helpers.generateRandomString(1) : Helpers.random.nextDouble());
		message.put("epoch",
				(repetition & 0b000100) == 0 ? ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER) : new JSONArray());

		JSONObject orbitalElements = new JSONObject();
		orbitalElements.put("semi_major_axis_au",
				(repetition & 0b001000) == 0 ? Helpers.random.nextDouble() : Helpers.generateRandomString(1));
		orbitalElements.put("eccentricity",
				(repetition & 0b001000) == 0 ? Helpers.random.nextDouble() : Helpers.generateRandomString(1));
		orbitalElements.put("inclination_deg",
				(repetition & 0b001000) == 0 ? Helpers.random.nextDouble() : Helpers.generateRandomString(1));
		orbitalElements.put("longitude_ascending_node_deg",
				(repetition & 0b001000) == 0 ? Helpers.random.nextDouble() : Helpers.generateRandomString(1));
		orbitalElements.put("argument_of_periapsis_deg",
				(repetition & 0b100000) == 0 ? Helpers.random.nextDouble() : Helpers.generateRandomString(1));
		orbitalElements.put("mean_anomaly_deg",
				(repetition & 0b100000) == 0 ? Helpers.random.nextDouble() : Helpers.generateRandomString(1));

		JSONObject stateVector = new JSONObject();
		stateVector.put("position_au",
				(repetition & 0b010000) == 0 ? new JSONArray().put(Helpers.random.nextDouble())
						.put(Helpers.random.nextDouble()).put(Helpers.random.nextDouble()) : new JSONObject());
		stateVector.put("velocity_au_per_day",
				(repetition & 0b010000) == 0 ? new JSONArray().put(Helpers.random.nextDouble())
						.put(Helpers.random.nextDouble()).put(Helpers.random.nextDouble()) : new JSONObject());

		JSONObject metadata = new JSONObject();
		metadata.put("record_payload",
				(repetition & 0b000100) == 0 ? Helpers.random.nextDouble() : Helpers.generateRandomString(1));

		JSONObject observatory = new JSONObject();
		observatory.put("latitude",
				(repetition & 0b001000) == 0 ? Helpers.random.nextDouble() : Helpers.generateRandomString(1));
		observatory.put("longitude",
				(repetition & 0b010000) == 0 ? Helpers.random.nextDouble() : Helpers.generateRandomString(1));
		observatory.put("observatory_name",
				(repetition & 0b100000) == 0 ? Helpers.generateRandomString(1) : Helpers.random.nextInt());
		metadata.put("observatory", new JSONArray().put(observatory));

		message.put("orbital_elements", orbitalElements);
		message.put("state_vector", stateVector);
		message.put("metadata", metadata);

		logger.info(String.format("Iteration: %d, message: %s", repetition, message));

		StatusCode result = testClient.sendMessage(message.toString());
		assertThat(String.format("Message: %s", message), result, is(httpClientError()));
	}

}
