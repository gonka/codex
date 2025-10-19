package com.example.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RestClientTest {

    private HttpServer server;
    private ExecutorService executor;
    private URI baseUri;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.start();
        baseUri = URI.create("http://localhost:" + server.getAddress().getPort() + "/");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        executor.shutdownNow();
    }

    @Test
    void getRequestIncludesQueryParametersAndHeaders() {
        AtomicReference<URI> uriRef = new AtomicReference<>();
        AtomicReference<String> traceHeader = new AtomicReference<>();

        registerHandler("/users", exchange -> {
            uriRef.set(exchange.getRequestURI());
            traceHeader.set(exchange.getRequestHeaders().getFirst("X-Trace"));
            respond(exchange, 200, "[]", Map.of("Content-Type", "application/json"));
        });

        RestClient client = RestClient.builder()
                .baseUri(baseUri.toString())
                .defaultHeader("Accept", "application/json")
                .build();

        RequestOptions options = RequestOptions.builder()
                .queryParam("page", "1")
                .queryParam("sort", "name")
                .header("X-Trace", "trace-123")
                .build();

        RestResponse response = client.get("/users", options);

        assertTrue(response.isSuccessful());
        assertEquals("page=1&sort=name", uriRef.get().getQuery());
        assertEquals("trace-123", traceHeader.get());
        assertEquals("application/json", response.header("Content-Type").orElseThrow());
        assertEquals("[]", response.body());
    }

    @Test
    void postRequestSendsBody() {
        AtomicReference<String> bodyRef = new AtomicReference<>();

        registerHandler("/messages", exchange -> {
            bodyRef.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 201, "{\"status\":\"created\"}",
                    Map.of("Content-Type", "application/json", "status", "created"));
        });

        RestClient client = RestClient.builder()
                .baseUri(baseUri.toString())
                .requestTimeout(Duration.ofSeconds(2))
                .build();

        RequestOptions options = RequestOptions.builder()
                .body("{\"message\":\"Hello\"}")
                .header("Content-Type", "application/json")
                .build();

        RestResponse response = client.post("messages", options);

        assertEquals("{\"message\":\"Hello\"}", bodyRef.get());
        assertEquals(201, response.statusCode());
        assertTrue(response.isSuccessful());
        assertEquals("created", response.header("status").orElseThrow());
    }

    @Test
    void connectionFailureThrowsException() {
        RestClient client = RestClient.builder()
                .baseUri("http://localhost:1")
                .connectTimeout(Duration.ofMillis(200))
                .build();

        assertThrows(RestClientException.class, () -> client.get("/unreachable"));
    }

    private void registerHandler(String path, HttpHandler handler) {
        server.createContext(path, handler);
    }

    private static void respond(HttpExchange exchange, int statusCode, String body, Map<String, String> headers)
            throws IOException {
        headers.forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
