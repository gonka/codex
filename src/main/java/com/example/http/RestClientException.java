package com.example.http;

/**
 * Exception thrown when the {@link RestClient} cannot complete a request.
 */
public final class RestClientException extends RuntimeException {
    public RestClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
