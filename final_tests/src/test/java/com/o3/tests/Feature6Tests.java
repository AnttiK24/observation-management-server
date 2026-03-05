package com.o3.tests;

import static com.o3.tests.matchers.IsHttpSuccess.httpSuccess;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.ZonedDateTime;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.o3.tests.client.StatusCode;
import com.o3.tests.client.User;
import com.o3.tests.messages.MessageHorizons;

class Feature6Tests extends TestBase {

	@Test
	@DisplayName("Test creating collection")
	@Order(2)
	void testCollectionCreation() throws InterruptedException {

		// register user
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());

		// create collection
		logger.info("Creating empty collection");
		StatusCode result = testClient.createCollection();
		assertThat(result.getBody(), result, is(httpSuccess()));

		// check that id is returned
		JSONObject returnedId = new JSONObject(result.getBody());
		assertThat("json did not contain key 'created_collection_id'", returnedId.has("created_collection_id"), is(true));

		// test creating collection with messages
		logger.info("Creating collection with message");

		logger.info("sending message to server");
		MessageHorizons msg = createRandomMessage();
		testClient.sendMessage(msg.toJsonString());

		logger.info("getting messages from server");
		result = testClient.getMessages();
		JSONArray messages = new JSONArray(result.getBody());
		JSONObject previousMsg = messages.getJSONObject(messages.length() - 1);
		int messageId = previousMsg.getJSONObject("metadata").getInt("id");

		result = testClient.createCollection(messageId);
		assertThat(result.getBody(), result, is(httpSuccess()));
		returnedId = new JSONObject(result.getBody());
		assertThat("json did not contain key 'created_collection_id'", returnedId.has("created_collection_id"), is(true));
	}

	@Test
	@DisplayName("Test getting all collections")
	@Order(3)
	void testGettingCollection() throws InterruptedException {

		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());

		// Test getting all collections
		logger.info("Creating empty collections");
		// Create random amount of empty collections
		int collectionCount = random.nextInt(1, 6);
		int [] collectionIds = new int[collectionCount];
		for(int i = 0; i < collectionCount; i++) {
			StatusCode result = testClient.createCollection();
			JSONObject returnedId = new JSONObject(result.getBody());
			// store their returned ids
			collectionIds[i] = returnedId.getInt("created_collection_id");
		}

		// get collections
		logger.info("Getting collections");
		StatusCode result = testClient.getCollections();
		assertThat(result.getBody(), result, is(httpSuccess()));
		JSONArray returnedIds = new JSONArray(result.getBody());
		assertThat("recieved less ids than expected", returnedIds.length() >= collectionIds.length, is(true));

		logger.info("Testing if previously created collection's ids are present");
		for(int expectedId : collectionIds) {
			boolean found = false;
			for(int i = 0; i < returnedIds.length(); i++) {
				int id = returnedIds.getInt(i);

				if(id == expectedId) {
					found = true;
					break;
				}
			}
			assertThat("Returned ids did not contain expected collection id: " + expectedId, found, is(true));
		}
	}

	@Test
	@DisplayName("Test getting specific collection")
	@Order(4)
	void testGettingMessagesFromCollection() throws InterruptedException {

		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());

		// create collection with random messages passed
		logger.info("Sending messages to server");
		int msgCount = random.nextInt(1, 6);
		for(int i = 0; i < msgCount; i++) {
			testClient.sendMessage(createRandomMessage().toJsonString());
		}

		logger.info("Getting messages from server");
		StatusCode result = testClient.getMessages();
		JSONArray messages = new JSONArray(result.getBody());

		// get last X messages' ids
		logger.info("Reading ids of previously sent messages");
		int[] messageIds = getLastXMessageIds(messages, msgCount);

		logger.info("Creating new collection using previous message's ids");
		result = testClient.createCollection(messageIds);
		assertThat(result.getBody(), result, is(httpSuccess()));

		// get the specific collection
		JSONObject returnedId = new JSONObject(result.getBody());
		int collectionId = returnedId.getInt("created_collection_id");

		logger.info("Getting messages from created collection");
		result = testClient.getCollection(collectionId);
		assertThat(result.getBody(), result, is(httpSuccess()));

		JSONArray returnedMessages = new JSONArray(result.getBody());
		// check that returned message's ids match
		assertThat("recieved less messages from collection than expected", returnedMessages.length() >= msgCount, is(true));

		logger.info("Testing if previously created collection's ids are present");
		for(int expectedId : messageIds) {
			boolean found = false;
			for(int i = 0; i < returnedMessages.length(); i++) {
				int returnedMessageId = returnedMessages.getJSONObject(i).getJSONObject("metadata").getInt("id");
				if(returnedMessageId == expectedId) {
					found = true;
					break;
				}
			}
			assertThat("collection did not contain message with id: " + expectedId, found, is(true));
		}
	}

	@Test
	@DisplayName("Test adding messages to collection")
	@Order(5)
	void testAddingToCollection() throws InterruptedException {

		// register user
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());

		// create collection
		logger.info("Creating empty collection");
		StatusCode result = testClient.createCollection();
		assertThat(result.getBody(), result, is(httpSuccess()));

		JSONObject returnedId = new JSONObject(result.getBody());
		int collectionId = returnedId.getInt("created_collection_id");

		logger.info("sending message(s) to server");

		// Create random amount of messages to be added
		int msgCount = random.nextInt(1, 6);
		for(int i = 0; i < msgCount; i++) {
			testClient.sendMessage(createRandomMessage().toJsonString());
		}
		logger.info("getting messages from server");
		result = testClient.getMessages();
		JSONArray messages = new JSONArray(result.getBody());

		// get last X messages' ids
		int[] messageIds = getLastXMessageIds(messages, msgCount);

		// add them to da collection
		result = testClient.addToCollection(collectionId, messageIds);
		assertThat(result.getBody(), result, is(httpSuccess()));

		// verify ids match
		StatusCode response = testClient.getCollection(collectionId);
		assertThat(response.getBody(), response, is(httpSuccess()));
		JSONArray collectionMessages = new JSONArray(response.getBody());
		// Should return same amount as ids:
		assertThat(collectionMessages.length(), is(messageIds.length));

		for(int expectedId : messageIds) {
			boolean found = false;
			for(int i = 0; i < collectionMessages.length(); i++) {
				int returnedMessageId = collectionMessages.getJSONObject(i).getJSONObject("metadata").getInt("id");
				if(returnedMessageId == expectedId) {
					found = true;
					break;
				}
			}
			assertThat("collection did not contain message with id: " + expectedId, found, is(true));
		}
	}

	private int[] getLastXMessageIds(JSONArray allMessages, int lastX) {

		int start = Math.max(0, allMessages.length() - lastX);
		int [] messageIds = new int[lastX];
		int indexForArray = 0;
		for(int i = start; i < allMessages.length(); i++) {
			messageIds[indexForArray++] = allMessages.getJSONObject(i).getJSONObject("metadata").getInt("id");
		}

		return messageIds;
	}

	private MessageHorizons createRandomMessage() {

		MessageHorizons message = new MessageHorizons(
			Helpers.generateRandomString(1),
			Helpers.generateRandomString(1),
			ZonedDateTime.now(),
			Helpers.createStateVector(),
			Helpers.generateRandomString(8) // Add payload
		);

		return message;
	}
}
