
# Observation Management Server

A Java-based HTTPS server developed as part of a university software development project.

The server provides an HTTP API for storing, retrieving, and organizing observation records. It includes user registration and authentication, SQLite database integration, and weather data retrieval through an external service.

## Technologies Used

- Java 21
- Maven
- SQLite and JDBC
- HTTP / HTTPS
- JSON and XML
- HTTP Basic Authentication

## Features Implemented

- **Observation management:** Create and retrieve observation records using HTTP requests.
- **User authentication:** Register users and authenticate requests using HTTP Basic Authentication.
- **Database integration:** Store user information and observation records in SQLite.
- **Observatory information:** Attach observatory metadata to observation records.
- **Weather integration:** Retrieve weather information from a separate weather service based on observatory coordinates.
- **Collection management:** Create collections, add observation records to collections, and retrieve stored collections.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/registration` | Register a user |
| GET | `/datarecord` | Retrieve observation records |
| POST | `/datarecord` | Store an observation record |
| GET | `/collections` | Retrieve collection IDs |
| POST | `/collections/create` | Create a collection |
| POST | `/collections/add` | Add records to a collection |
| GET | `/collections/{id}` | Retrieve records from a collection |

Authentication is required for observation and collection endpoints.

## Running the Server

### Requirements

- Java 21
- Maven
- SQLite database path
- Java keystore for HTTPS
- Weather service (optional, required for weather integration)

### Build

```bash
mvn clean package
```

### Configuration

The server requires the `DATABASE_PATH` environment variable and two command-line arguments:

1. Path to the SSL/TLS keystore
2. Keystore password

Example environment variable on Linux:

```bash
export DATABASE_PATH="jdbc:sqlite:observations.db"
```

### Start the Server

```bash
java -jar target/server-1.0-jar-with-dependencies.jar path/to/keystore.jks YOUR_KEYSTORE_PASSWORD
```

Replace the keystore path and password with your local configuration.

The server can also be started through the IDE by running the `Server.java` main method with the required environment variable and program arguments configured.

**Server address:**

`https://localhost:8001`

### Weather Service

To enable weather integration, start the separate weather server:

```bash
java -jar weatherserver.jar
```

The main application expects the weather service at:

`http://localhost:4001`

## Project Structure

Source code:

`src/main/java/com/o3/server`

Tests:

`src/test/java/com/o3/server`

Main components:

- `Server.java` – HTTPS server and observation request handling
- `MessageDatabase.java` – SQLite database operations
- `UserAuthenticator.java` – User authentication
- `RegistrationHandler.java` – User registration
- `CollectionsHandler.java` – Collection management
- `ObservationRecord.java` – Observation data model
- `weather.java` – Weather service integration

## Learning Outcomes

This project provided practical experience in:

- Java backend development
- HTTP request and response handling
- SQL database operations using JDBC
- User authentication and password hashing
- JSON and XML data processing
- External service integration
- Data validation and error handling

## Known Limitations

This project was developed for educational purposes and is not production-ready.

Areas for future improvement include:

- More comprehensive authorization and access control
- Improved database concurrency handling
- Refactoring request handlers into separate service layers
- More consistent API error responses
- Additional automated tests

