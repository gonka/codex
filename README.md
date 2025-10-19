# Java REST HTTP Client

A lightweight HTTP client built with Java 25 and the JDK `HttpClient` API for consuming RESTful services. It
provides a fluent builder for configuring a reusable client instance, immutable request options, and
`RestResponse` metadata helpers.

## Features

- Configure a base URI, timeouts, redirect policy, HTTP version, and default headers.
- Compose requests with method-specific helpers (`get`, `post`, `put`, `patch`, `delete`, `head`, `options`).
- Supply per-request headers, query parameters, request bodies, and custom timeouts via `RequestOptions`.
- Receive rich response information including headers, request URI, status code, body, and success helpers.

## Building and Testing

This project uses Maven. Ensure you are running Java 25 (or a compatible toolchain) and then execute:

```bash
mvn clean test
```

## Usage

```java
var client = RestClient.builder()
        .baseUri("https://api.example.com/")
        .defaultHeader("Accept", "application/json")
        .requestTimeout(Duration.ofSeconds(10))
        .build();

var options = RequestOptions.builder()
        .queryParam("page", "1")
        .header("Authorization", "Bearer ...")
        .body("{\"name\":\"Sample\"}")
        .build();

RestResponse response = client.post("v1/resources", options);

if (response.isSuccessful()) {
    System.out.println(response.body());
} else {
    throw new IllegalStateException("Call failed: " + response.statusCode());
}
```
