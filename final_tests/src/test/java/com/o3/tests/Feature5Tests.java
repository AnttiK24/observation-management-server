package com.o3.tests;

import static com.o3.tests.matchers.IsHttpClientError.httpClientError;
import static com.o3.tests.matchers.IsHttpSuccess.httpSuccess;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.o3.tests.bmp.Bmp;
import com.o3.tests.client.StatusCode;
import com.o3.tests.client.User;
import com.o3.tests.messages.MessageHorizons;
import com.o3.tests.messages.Observatory;
import com.o3.tests.messages.OrbitalElements;
import com.o3.tests.messages.StateVector;
import com.o3.tests.messages.Weather;

class Feature5Tests extends TestBase {
	@Test
	@DisplayName("Test attaching weather information")
	@Order(3)
	void testWeather() throws InterruptedException {

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
				bmp.getDataString());
		Observatory observatory = new Observatory(
				Helpers.generateRandomString(4),
				random.nextDouble(0, 90),
				random.nextDouble(0, 180));
		observatory.setWeather(new Weather(0, 0, 0)); // Set weather (optional field)
		// when MessageHorizons is made into JSONObject with new JSONObject(this) if the field has non null value it is included
		message.getMetadata().setObservatories(new ArrayList<>() {
			{
				add(observatory);
			}
		});
		logger.info(String.format("Created message:%n%s", message.toJsonString(4)
				.replaceFirst(Constants.IMAGE_DATA_REGEX, Constants.IMAGE_DATA_PLACEHOLDER)));

		ZonedDateTime sendBegin = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		StatusCode result = testClient.sendMessage(message.toJsonString(4));
		ZonedDateTime sendEnd = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		assertThat(result.getBody(), result, is(httpSuccess()));

		message.getMetadata().setOwner(user.getNickname());
		logger.info(String.format("Expected record_time_received range: [%s, %s]",
				sendBegin, sendEnd));
		StatusCode response = testClient.getMessages();
		String messages = response.getBody();
		assertThat(response.getBody(), response, is(httpSuccess()));
		checkJsonResponse(message.toJson(), messages, sendBegin, sendEnd);
		assertThat(checkWeather(messages, 1), is(true));
	}

	@Test
	@DisplayName("Test attaching multiple weather information")
	@Order(4)
	void testWeatherMultiple() throws InterruptedException {

		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());

		logger.info("Testing attaching weather with multiple observatories");

		Bmp bmp = new Bmp(20, 20);
		StateVector vector = Helpers.createStateVector();
		MessageHorizons message = new MessageHorizons(
				Helpers.generateRandomString(1),
				Helpers.generateRandomString(1),
				ZonedDateTime.now(),
				new OrbitalElements(vector),
				bmp.getDataString());

		// Create random amount, check later that count matches
		int limit = random.nextInt(4, 10);
		int weatherCount = 0;
		double lat = random.nextDouble(0, 90); // Same lat and long for
		double longi = random.nextDouble(0, 180); // Used to check that returned weather value matches
		ArrayList<Observatory> observatories = new ArrayList<>();

		for (int i = 0; i < limit; i++) {
			Observatory observatory = new Observatory(
					Helpers.generateRandomString(3),
					lat,
					longi);
			if(i % 2 == 0) {
				observatory.setWeather(new Weather(0, 0, 0));
				weatherCount++;
			}
			observatories.add(observatory);
		}
		message.getMetadata().setObservatories(observatories);
		logger.info(String.format("Created message:%n%s", message.toJsonString(4)
				.replaceFirst(Constants.IMAGE_DATA_REGEX, Constants.IMAGE_DATA_PLACEHOLDER)));

		ZonedDateTime sendBegin = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		StatusCode result = testClient.sendMessage(message.toJsonString(4));
		ZonedDateTime sendEnd = ZonedDateTime.parse(ZonedDateTime.now(ZoneOffset.UTC).format(Constants.FORMATTER));
		assertThat(result.getBody(), result, is(httpSuccess()));

		message.getMetadata().setOwner(user.getNickname());
		logger.info(String.format("Expected record_time_received range: [%s, %s]",
				sendBegin.format(Constants.FORMATTER), sendEnd.format(Constants.FORMATTER)));

		StatusCode response = testClient.getMessages();
		String messages = response.getBody();
		assertThat(response.getBody(), response, is(httpSuccess()));
		checkJsonResponse(message.toJson(), messages, sendBegin, sendEnd);

		// We should have weatherCount weather objects
		assertThat(checkWeather(messages, weatherCount), is(true));

		logger.info("Testing that returned weather is expected");

		// Test that same latitude and longitude returns the same value:
		// weather server is seeded with: long seed = Double.doubleToLongBits(lat) ^ Double.doubleToLongBits(lon)
		JSONArray messageArray = new JSONArray(messages);
		JSONObject latestMessage = messageArray.getJSONObject(messageArray.length() - 1);
		JSONArray obs = latestMessage.getJSONObject("metadata").getJSONArray("observatory");

		JSONArray weathers = new JSONArray();
		for(int i = 0; i < obs.length(); i++) {
			if(obs.getJSONObject(i).has("weather")) {
				weathers.put(obs.getJSONObject(i).getJSONObject("weather"));
			}
		}

		for (int i = 0; i < weathers.length() - 1; i++) {
			JSONObject w1 = weathers.getJSONObject(i);
			JSONObject w2 = weathers.getJSONObject(i + 1);
			assertThat(true, is(w1.similar(w2)));
		}
	}

	@Test
	@DisplayName("Test attaching weather information without lat/lon")
	@Order(5)
	void testWeatherNoLatlon() throws InterruptedException {

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
				bmp.getDataString());
		Observatory observatory = new Observatory(
				Helpers.generateRandomString(2));
		message.getMetadata().setObservatories(new ArrayList<>() {
			{
				add(observatory);
			}
		});
		message.getMetadata().setOwner(user.getNickname());
		logger.info(String.format("Created message:%n%s", message.toJsonString(4)
				.replaceFirst(Constants.IMAGE_DATA_REGEX, Constants.IMAGE_DATA_PLACEHOLDER)));

		StatusCode result = testClient.sendMessage(message.toJsonString(4));
		assertThat(result.getBody(), result, is(httpClientError()));
	}

	private boolean checkWeather(String messages, int expected) {

		JSONArray json = new JSONArray(messages);
		// Get message and its metadata for each message:
		for (int i = 0; i < json.length(); i++) {

			int actual = 0;
			var message = json.getJSONObject(i);
			JSONObject metadata = message.getJSONObject("metadata");

			if (metadata.has("observatory")) {
				// Get observatories from metadata...
				JSONArray observatories = metadata.getJSONArray("observatory");
				for (int j = 0; j < observatories.length(); j++) {
					// ... and check each of them:
					JSONObject obs = observatories.getJSONObject(j);
					if (obs.has("weather")) {
						JSONObject weather = obs.getJSONObject("weather");
						if (weather.has("temperature_in_kelvins") && weather.has("cloudiness_percentage")
								&& weather.has("background_light_volume")) {
							actual++;
						}
					}
				}

			}

			System.out.println(actual);
			if (actual == expected)
				return true;
		}
		return false;
	}
}
