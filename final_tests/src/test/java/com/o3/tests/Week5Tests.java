package com.o3.tests;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static com.o3.tests.matchers.IsHttpClientError.httpClientError;
import static com.o3.tests.matchers.IsHttpSuccess.httpSuccess;

import com.o3.tests.bmp.Bmp;
import com.o3.tests.client.StatusCode;
import com.o3.tests.client.User;
import com.o3.tests.messages.MessageHorizons;
import com.o3.tests.messages.Observatory;
import com.o3.tests.messages.OrbitalElements;
import com.o3.tests.messages.StateVector;

class Week5Tests extends TestBaseJson {
	@Test
	@DisplayName("Test sending a message")
	@Order(3)
	void testSendMessage() throws InterruptedException {
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());
		logger.info("Testing sending a message to the server");
		Bmp bmp = new Bmp(20, 20);

		StateVector vector = Helpers.createStateVector();
		MessageHorizons message = new MessageHorizons(
			Helpers.generateRandomString(1),
			Helpers.generateRandomString(1),
			ZonedDateTime.now(),
			new OrbitalElements(vector),
			vector,
			bmp.getDataString()
		);

		ZonedDateTime sendBegin = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		StatusCode result = testClient.sendMessage(message.toJsonString());
		ZonedDateTime sendEnd = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		assertThat(result.getBody(), result, is(httpSuccess()));

		StatusCode response = testClient.getMessages();
		assertThat(response.getBody(), response, is(httpSuccess()));
		String messages = response.getBody();

		message.getMetadata().setOwner(user.getNickname());
		logger.info(String.format("Expected message:%n%s",
				message.toJsonString(4).replaceFirst(Constants.IMAGE_DATA_REGEX, Constants.IMAGE_DATA_PLACEHOLDER)));
		logger.info(String.format("Expected recordTimeReceived range: [%s, %s]",
				sendBegin.format(Constants.FORMATTER), sendEnd.format(Constants.FORMATTER)));

		checkJsonResponse(message.toJson(), messages, sendBegin, sendEnd);
	}

	@Test
	@DisplayName("Test sending a message with observatory data")
	@Order(3)
	void testSendObservatory() throws InterruptedException {
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());
		logger.info("Testing sending a message to the server");

		Bmp bmp = new Bmp(20, 20);
		StateVector vector = Helpers.createStateVector();
		MessageHorizons message = new MessageHorizons(
			Helpers.generateRandomString(1),
			Helpers.generateRandomString(1),
			ZonedDateTime.now(),
			new OrbitalElements(vector),
			vector,
			bmp.getDataString()
		);

		Observatory observatory = new Observatory(
				Helpers.generateRandomString(4),
				random.nextDouble(0, 90),
				random.nextDouble(0, 180));
		message.getMetadata().setObservatories(new ArrayList<>() {
			{
				add(observatory);
			}
		});

		logger.info(String.format("Created message:%n%s", message.toJsonString(4)));

		ZonedDateTime sendBegin = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		StatusCode result = testClient.sendMessage(message.toJsonString());
		ZonedDateTime sendEnd = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		assertThat(result.getBody(), result, is(httpSuccess()));

		message.getMetadata().setOwner(user.getNickname());
		logger.info(String.format("Expected message:%n%s",
				message.toJsonString(4).replaceFirst(Constants.IMAGE_DATA_REGEX, Constants.IMAGE_DATA_PLACEHOLDER)));
		logger.info(String.format("Expected recordTimeReceived range: [%s, %s]",
				sendBegin, sendEnd));
		StatusCode response = testClient.getMessages();
		assertThat(response.getBody(), response, is(httpSuccess()));
		String messages = response.getBody();
		checkJsonResponse(message.toJson(), messages, sendBegin, sendEnd);
	}

	@Test
	@DisplayName("Test sending a message with missing observatory data")
	@Order(3)
	void testSendObservatoryNoCoords() throws InterruptedException {
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());
		logger.info("Testing sending a message to the server");
		Bmp bmp = new Bmp(20, 20);
		StateVector vector = Helpers.createStateVector();
		MessageHorizons message = new MessageHorizons(
			Helpers.generateRandomString(1),
			Helpers.generateRandomString(1),
			ZonedDateTime.now(),
			new OrbitalElements(vector),
			vector,
			bmp.getDataString()
		);

		Observatory observatory = new Observatory(
				Helpers.generateRandomString(4));
		message.getMetadata().setObservatories(new ArrayList<>() {
			{
				add(observatory);
			}
		});

		StatusCode result = testClient.sendMessage(message.toJsonString());
		assertThat(result.getBody(), result, is(httpClientError()));
	}

	@Test
	@DisplayName("Test messages with non-ASCII characters")
	@Order(3)
	void testNonAsciiMessages() throws InterruptedException {
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());
		logger.info("Testing sending a message with non-ASCII characters to the server");

		StateVector vector = Helpers.createStateVector();
		MessageHorizons message = new MessageHorizons(
			Helpers.generateRandomString(1)  + " 你好\u2b7fåäö\u200bΩ",
			Helpers.generateRandomString(1) + "😀",
			ZonedDateTime.now(),
			new OrbitalElements(vector),
			vector,
			Helpers.generateRandomString(8)
		);

		ZonedDateTime sendBegin = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		StatusCode result = testClient.sendMessage(message.toJsonString());
		ZonedDateTime sendEnd = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		assertThat(result.getBody(), result, is(httpSuccess()));

		message.getMetadata().setOwner(user.getNickname());
		logger.info(String.format("Expected message:%n%s", message.toJsonString(4)));
		logger.info(String.format("Expected recordTimeReceived range: [%s, %s]",
				sendBegin, sendEnd));

		StatusCode response = testClient.getMessages();
		assertThat(response.getBody(), response, is(httpSuccess()));
		String messages = response.getBody();

		checkJsonResponse(message.toJson(), messages, sendBegin, sendEnd);
	}

	@Test
	@DisplayName("Test sending a message with only a state vector")
	@Order(3)
	void testSendMessageWithOnlyStateVector() throws InterruptedException {
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());
		logger.info("Testing sending a message with only a state vector to the server");
		Bmp bmp = new Bmp(20, 20);

		StateVector vector = Helpers.createStateVector();
		MessageHorizons message = new MessageHorizons(
			Helpers.generateRandomString(1),
			Helpers.generateRandomString(1),
			ZonedDateTime.now(),
			vector,
			bmp.getDataString()
		);

		ZonedDateTime sendBegin = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		StatusCode result = testClient.sendMessage(message.toJsonString());
		ZonedDateTime sendEnd = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		assertThat(result.getBody(), result, is(httpSuccess()));

		StatusCode response = testClient.getMessages();
		assertThat(response.getBody(), response, is(httpSuccess()));
		String messages = response.getBody();

		message.getMetadata().setOwner(user.getNickname());
		logger.info(String.format("Expected message:%n%s",
				message.toJsonString(4).replaceFirst(Constants.IMAGE_DATA_REGEX, Constants.IMAGE_DATA_PLACEHOLDER)));
		logger.info(String.format("Expected recordTimeReceived range: [%s, %s]",
				sendBegin.format(Constants.FORMATTER), sendEnd.format(Constants.FORMATTER)));

		checkJsonResponse(message.toJson(), messages, sendBegin, sendEnd);
	}

	@Test
	@DisplayName("Test sending a message with only orbital elements")
	@Order(3)
	void testSendMessageWithOnlyOrbitalElements() throws InterruptedException {
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());
		logger.info("Testing sending a message with only orbital elements to the server");
		Bmp bmp = new Bmp(20, 20);

		StateVector vector = Helpers.createStateVector();
		MessageHorizons message = new MessageHorizons(
			Helpers.generateRandomString(1),
			Helpers.generateRandomString(1),
			ZonedDateTime.now(),
			new OrbitalElements(vector),
			bmp.getDataString()
		);

		ZonedDateTime sendBegin = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		StatusCode result = testClient.sendMessage(message.toJsonString());
		ZonedDateTime sendEnd = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		assertThat(result.getBody(), result, is(httpSuccess()));

		StatusCode response = testClient.getMessages();
		assertThat(response.getBody(), response, is(httpSuccess()));
		String messages = response.getBody();

		message.getMetadata().setOwner(user.getNickname());
		logger.info(String.format("Expected message:%n%s",
				message.toJsonString(4).replaceFirst(Constants.IMAGE_DATA_REGEX, Constants.IMAGE_DATA_PLACEHOLDER)));
		logger.info(String.format("Expected recordTimeReceived range: [%s, %s]",
				sendBegin.format(Constants.FORMATTER), sendEnd.format(Constants.FORMATTER)));

		checkJsonResponse(message.toJson(), messages, sendBegin, sendEnd);
	}
}
