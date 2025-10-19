package com.example.http;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Immutable request options that can be supplied to every HTTP call.
 */
public final class RequestOptions {
    private static final RequestOptions EMPTY = new Builder().build();

    private final Map<String, List<String>> queryParameters;
    private final Map<String, String> headers;
    private final String body;
    private final Duration timeout;

    private RequestOptions(Map<String, List<String>> queryParameters, Map<String, String> headers, String body,
                           Duration timeout) {
        this.queryParameters = queryParameters;
        this.headers = headers;
        this.body = body;
        this.timeout = timeout;
    }

    public static RequestOptions empty() {
        return EMPTY;
    }

    public Map<String, List<String>> queryParameters() {
        return queryParameters;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public String body() {
        return body;
    }

    public Duration timeout() {
        return timeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, List<String>> queryParameters = new LinkedHashMap<>();
        private final Map<String, String> headers = new LinkedHashMap<>();
        private String body;
        private Duration timeout;

        public Builder queryParam(String name, String value) {
            Objects.requireNonNull(name, "Query parameter name is required");
            Objects.requireNonNull(value, "Query parameter value is required");
            queryParameters
                    .computeIfAbsent(name, key -> new ArrayList<>())
                    .add(value);
            return this;
        }

        public Builder header(String name, String value) {
            headers.put(Objects.requireNonNull(name, "Header name is required"),
                    Objects.requireNonNull(value, "Header value is required"));
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            Objects.requireNonNull(headers, "Headers map is required");
            headers.forEach(this::header);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public RequestOptions build() {
            Map<String, List<String>> query = queryParameters.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
            Map<String, String> hdrs = Map.copyOf(headers);
            return new RequestOptions(query, hdrs, body, timeout);
        }
    }
}
