# Spring 2026 tests

This repository contains the tests for the Spring 2025 programming 3 course.
To run the tests locally, open this folder in your editor of choice and run the tests from there.
For VSCode, the tests can be found under the `Testing` tab on the left side of the window.
By expanding the `tests > com.o3.tests` folder, you can see all the tests that are available.

The tests can also be run from the command line by running the following command:
```bash
mvn test -Dtest=TestClassName
```
where `TestClassName` is the name of the test class you want to run.

Feature 5 tests require that you have the mock weather server running. To start the server, run the following command:
```bash
java -jar weatherserver.jar
```
This will start the server on port 4001. You can get info about the server by visiting `http://localhost:4001/wfs`.

Feature 8 tests require that you have the orbit conversion server running. To start the server, run the following command:
```bash
java -jar orbit.gql.jar
```
This will start the server on port 4003. You can get to the GraphQL IDE by visiting `http://localhost:4003/graphiql`. You can see the available queries by opening the `Explorer` tab in the side panel.
