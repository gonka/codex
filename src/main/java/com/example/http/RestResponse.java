package com.example.http;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a response from the {@link RestClient}.
 */
public record RestResponse(int statusCode, Map<String, List<String>> headers, String body, URI uri) {

    public Optional<String> header(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(headers.get(name)).flatMap(values -> values.stream().findFirst());
    }

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
}
