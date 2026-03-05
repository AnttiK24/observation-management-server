package com.o3.tests;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static com.o3.tests.matchers.IsHttpSuccess.httpSuccess;

import com.o3.tests.client.Client;
import com.o3.tests.client.StatusCode;
import com.o3.tests.client.User;
import com.o3.tests.messages.MessageHorizons;
import com.o3.tests.messages.OrbitalElements;
import com.o3.tests.messages.StateVector;

class Week5FloodTest extends TestBase {
	private static User user;
	private static Client client;

	@BeforeAll
	static void setup() throws InterruptedException, KeyManagementException, NoSuchAlgorithmException {
		System.setProperty("output.silent", "true");
		user = Helpers.createUser();
		client = new Client(
				Constants.ENVIRONMENT.getOrDefault(Constants.SERVER_DEFAULT_ADDRESS_KEY,
						Constants.SERVER_DEFAULT_ADDRESS_HTTPS));
		client.register(user.toJsonString());
		client.setAuth(user.getUsername(), user.getPassword());
	}

	@DisplayName("Test flooding the server with messages")
	@Order(4)
	@RepeatedTest(100)
	@Execution(ExecutionMode.CONCURRENT)
	void testFloodServer() throws InterruptedException, KeyManagementException, NoSuchAlgorithmException {
		StateVector vector = Helpers.createStateVector();
		MessageHorizons message = new MessageHorizons(
				Helpers.generateRandomString(1),
				Helpers.generateRandomString(1),
				ZonedDateTime.now(),
				new OrbitalElements(vector),
				vector,
				Helpers.generateRandomString(3));
		ZonedDateTime sendBegin = ZonedDateTime.now(ZoneOffset.UTC);
		StatusCode result = client.sendMessage(message.toJsonString());
		ZonedDateTime sendEnd = ZonedDateTime.now(ZoneOffset.UTC);
		assertThat(result.getBody(), result, is(httpSuccess()));

		StatusCode response = client.getMessages();
		assertThat(response.getBody(), response, is(httpSuccess()));
		String messages = response.getBody();
		message.getMetadata().setOwner(user.getNickname());
		checkJsonResponse(message.toJson(), messages, sendBegin, sendEnd);
	}
}
