package com.o3.tests;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

public class Constants {
	public static final String SERVER_DEFAULT_ADDRESS_HTTP = "http://localhost:8001";
	public static final String SERVER_DEFAULT_ADDRESS_HTTPS = "https://localhost:8001";
	public static final String SERVER_DEFAULT_ADDRESS_KEY = "SERVER_ADDRESS";
	public static final Map<String, String> ENVIRONMENT = Collections.unmodifiableMap(System.getenv());
	public static final String IMAGE_DATA_REGEX = "data:image\\/\\w+;base64,([a-zA-Z0-9\\/\\+=])+";
	public static final String IMAGE_DATA_PLACEHOLDER = "data:image/...";
	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

	private Constants() {
	}
}
