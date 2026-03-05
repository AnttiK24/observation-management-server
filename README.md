# Programming 3 assignment

Student Number: 2409565

## Introduction

This repository contains the template Java project for the Programming 3 assignment.
The assignment should be implemented in the [src/main/java/com/o3/server](src/main/java/com/o3/server) subfolder.
Any additional tests can be implemented in the [src/main/java/com/o3/server](src/test/java/com/o3/server) subfolder.
The [pom.xml](pom.xml) file contains the necessary dependencies for the assignment.
New dependencies will be added to the [pom.xml](pom.xml) file during the course.


## Features Implemented

- Feature 1: User can store observation information as a record
- Feature 2: User can get all observations from the server
- Feature 3: User can register and authenticate
- Feature 4: User can attach observatory information to the observation data

- Feature 5, weather service
- Feature 6, The endpoint collections must be implemented with the following behaviour:


## Running the server
- Java version: 21

- Start the server:
    In class server.java , pressing (run) over the main. starts the server
    
    other possibility if did not work or pipeline having troubles using commands in terminal:
        mvn clean package
        java -jar target/server-1.0-jar-with-dependencies.jar

- Start the weather service for feature 5 to work:
    java -jar weatherserver.jar 



## Notes
- Server can be found localhost:8001
- Weather service can be found on localhost:4001

  

