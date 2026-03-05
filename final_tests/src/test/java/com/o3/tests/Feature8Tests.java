package com.o3.tests;

import static com.o3.tests.matchers.IsHttpClientError.httpClientError;
import static com.o3.tests.matchers.IsHttpSuccess.httpSuccess;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.o3.tests.bmp.Bmp;
import com.o3.tests.client.StatusCode;
import com.o3.tests.client.User;
import com.o3.tests.messages.MessageHorizons;
import com.o3.tests.messages.OrbitalElements;
import com.o3.tests.messages.StateVector;

class Feature8Tests extends TestBase {
	@Test
	@DisplayName("Test converting state vector")
	@Order(2)
	void testConvertStateVector() throws InterruptedException {
		logger.info("Testing sending a partial message with state vector...");
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());

		Bmp bmp = new Bmp(20, 20);
		OrbitalElements elements = Helpers.createOrbitalElements();
		StateVector vector = OrekitOrbitConversions.orbitalElementsToStateVector(elements);
		MessageHorizons message = new MessageHorizons(
				Helpers.generateRandomString(1),
				Helpers.generateRandomString(1),
				ZonedDateTime.now(),
				vector,
				bmp.getDataString());

		ZonedDateTime sendBegin = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		StatusCode result = testClient.sendPartialMessage(message.toJsonString());
		ZonedDateTime sendEnd = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		assertThat(result.getBody(), result, is(httpSuccess()));

		StatusCode response = testClient.getMessages();
		assertThat(response.getBody(), response, is(httpSuccess()));
		String messages = response.getBody();

		message.setOrbitalElements(OrekitOrbitConversions.stateVectorToOrbitalElements(vector));
		message.getMetadata().setOwner(user.getNickname());
		logger.info(String.format("Expected message:%n%s",
				message.toJsonString(4).replaceFirst(Constants.IMAGE_DATA_REGEX, Constants.IMAGE_DATA_PLACEHOLDER)));
		logger.info(String.format("Expected recordTimeReceived range: [%s, %s]",
				sendBegin.format(Constants.FORMATTER), sendEnd.format(Constants.FORMATTER)));

		checkJsonResponse(message.toJson(), messages, sendBegin, sendEnd);
	}

	@Test
	@DisplayName("Test converting orbital elements")
	@Order(3)
	void testConvertOrbitalElements() throws InterruptedException {
		logger.info("Testing sending a partial message...");
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());

		Bmp bmp = new Bmp(20, 20);
		OrbitalElements elements = Helpers.createOrbitalElements();
		StateVector vector = OrekitOrbitConversions.orbitalElementsToStateVector(elements);
		MessageHorizons message = new MessageHorizons(
				Helpers.generateRandomString(1),
				Helpers.generateRandomString(1),
				ZonedDateTime.now(),
				elements,
				bmp.getDataString());

		ZonedDateTime sendBegin = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		StatusCode result = testClient.sendPartialMessage(message.toJsonString());
		ZonedDateTime sendEnd = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		assertThat(result.getBody(), result, is(httpSuccess()));

		StatusCode response = testClient.getMessages();
		assertThat(response.getBody(), response, is(httpSuccess()));
		String messages = response.getBody();

		message.setStateVector(vector);
		message.getMetadata().setOwner(user.getNickname());
		logger.info(String.format("Expected message:%n%s",
				message.toJsonString(4).replaceFirst(Constants.IMAGE_DATA_REGEX, Constants.IMAGE_DATA_PLACEHOLDER)));
		logger.info(String.format("Expected recordTimeReceived range: [%s, %s]",
				sendBegin.format(Constants.FORMATTER), sendEnd.format(Constants.FORMATTER)));

		checkJsonResponse(message.toJson(), messages, sendBegin, sendEnd);
	}

	@Test
	@DisplayName("Test converting both observation types")
	@Order(4)
	void testConvertBoth() throws InterruptedException {
		logger.info("Testing sending a complete message to partial message endpoint...");
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());

		Bmp bmp = new Bmp(20, 20);
		OrbitalElements elements = Helpers.createOrbitalElements();
		StateVector vector = OrekitOrbitConversions.orbitalElementsToStateVector(elements);
		MessageHorizons message = new MessageHorizons(
				Helpers.generateRandomString(1),
				Helpers.generateRandomString(1),
				ZonedDateTime.now(),
				elements,
				vector,
				bmp.getDataString());

		StatusCode result = testClient.sendPartialMessage(message.toJsonString());
		assertThat(result.getBody(), result, is(httpClientError()));
	}

	@Test
	@DisplayName("Test sending message with no data")
	@Order(4)
	void testSendIncompleteMessage() throws InterruptedException {
				logger.info("Testing sending a partial message...");
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());

		Bmp bmp = new Bmp(20, 20);
		MessageHorizons message = new MessageHorizons(
				Helpers.generateRandomString(1),
				Helpers.generateRandomString(1),
				ZonedDateTime.now(),
				bmp.getDataString());

		StatusCode result = testClient.sendPartialMessage(message.toJsonString());
		assertThat(result.getBody(), result, is(httpClientError()));
	}
}
