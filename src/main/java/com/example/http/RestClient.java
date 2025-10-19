package com.example.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A lightweight HTTP client for interacting with REST APIs using the JDK {@link HttpClient}.
 * The client is immutable and threadsafe.
 */
public final class RestClient {
    private final HttpClient httpClient;
    private final URI baseUri;
    private final Map<String, String> defaultHeaders;
    private final Duration requestTimeout;

    private RestClient(Builder builder) {
        this.baseUri = builder.baseUri;
        this.defaultHeaders = Map.copyOf(builder.defaultHeaders);
        this.requestTimeout = builder.requestTimeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(builder.connectTimeout)
                .followRedirects(builder.redirectPolicy)
                .version(builder.httpVersion)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public RestResponse get(String path) {
        return get(path, RequestOptions.empty());
    }

    public RestResponse get(String path, RequestOptions options) {
        return execute(HttpMethod.GET, path, options);
    }

    public RestResponse delete(String path) {
        return delete(path, RequestOptions.empty());
    }

    public RestResponse delete(String path, RequestOptions options) {
        return execute(HttpMethod.DELETE, path, options);
    }

    public RestResponse head(String path) {
        return head(path, RequestOptions.empty());
    }

    public RestResponse head(String path, RequestOptions options) {
        return execute(HttpMethod.HEAD, path, options);
    }

    public RestResponse options(String path) {
        return options(path, RequestOptions.empty());
    }

    public RestResponse options(String path, RequestOptions options) {
        return execute(HttpMethod.OPTIONS, path, options);
    }

    public RestResponse post(String path, RequestOptions options) {
        return execute(HttpMethod.POST, path, options);
    }

    public RestResponse put(String path, RequestOptions options) {
        return execute(HttpMethod.PUT, path, options);
    }

    public RestResponse patch(String path, RequestOptions options) {
        return execute(HttpMethod.PATCH, path, options);
    }

    public RestResponse send(HttpMethod method, String path, RequestOptions options) {
        return execute(method, path, options);
    }

    private RestResponse execute(HttpMethod method, String path, RequestOptions options) {
        Objects.requireNonNull(method, "HTTP method is required");
        Objects.requireNonNull(path, "Request path is required");
        var opts = options == null ? RequestOptions.empty() : options;

        URI uri = buildUri(path, opts.queryParameters());
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .timeout(resolveTimeout(opts))
                .method(method.name(), createBodyPublisher(method, opts.body()));

        defaultHeaders.forEach(requestBuilder::header);
        opts.headers().forEach(requestBuilder::header);

        HttpRequest request = requestBuilder.build();

        try {
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new RestResponse(response.statusCode(), response.headers().map(), response.body(), response.uri());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RestClientException("Request interrupted", e);
        } catch (IOException e) {
            throw new RestClientException("I/O error while invoking " + method + " " + uri, e);
        }
    }

    private Duration resolveTimeout(RequestOptions options) {
        if (options.timeout() != null) {
            return options.timeout();
        }
        if (requestTimeout != null) {
            return requestTimeout;
        }
        return Duration.ofSeconds(30);
    }

    private static HttpRequest.BodyPublisher createBodyPublisher(HttpMethod method, String body) {
        return switch (method) {
            case GET, DELETE, HEAD, OPTIONS -> BodyPublishers.noBody();
            default -> body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body, StandardCharsets.UTF_8);
        };
    }

    private URI buildUri(String path, Map<String, List<String>> queryParameters) {
        URI resolved = baseUri == null ? URI.create(path) : baseUri.resolve(path);
        String rawQuery = resolved.getRawQuery();
        String optionsQuery = encodeQueryParameters(queryParameters);

        String combinedQuery;
        if (rawQuery == null || rawQuery.isEmpty()) {
            combinedQuery = optionsQuery;
        } else if (optionsQuery.isEmpty()) {
            combinedQuery = rawQuery;
        } else {
            combinedQuery = rawQuery + "&" + optionsQuery;
        }

        try {
            return new URI(
                    resolved.getScheme(),
                    resolved.getRawAuthority(),
                    normalizePath(resolved.getRawPath()),
                    combinedQuery.isEmpty() ? null : combinedQuery,
                    resolved.getRawFragment());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Failed to build URI for path '%s'".formatted(path), e);
        }
    }

    private static String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return "/";
        }
        return rawPath;
    }

    private static String encodeQueryParameters(Map<String, List<String>> queryParameters) {
        if (queryParameters.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        queryParameters.forEach((name, values) -> {
            if (values.isEmpty()) {
                appendQueryParameter(builder, name, null);
            } else {
                for (String value : values) {
                    appendQueryParameter(builder, name, value);
                }
            }
        });
        return builder.toString();
    }

    private static void appendQueryParameter(StringBuilder builder, String name, String value) {
        if (builder.length() > 0) {
            builder.append('&');
        }
        builder.append(URLEncoder.encode(name, StandardCharsets.UTF_8));
        if (value != null) {
            builder.append('=');
            builder.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
    }

    /**
     * Builder for creating immutable {@link RestClient} instances.
     */
    public static final class Builder {
        private URI baseUri;
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration requestTimeout;
        private final Map<String, String> defaultHeaders = new LinkedHashMap<>();
        private HttpClient.Version httpVersion = HttpClient.Version.HTTP_2;
        private HttpClient.Redirect redirectPolicy = HttpClient.Redirect.NORMAL;

        private Builder() {
        }

        public Builder baseUri(String baseUri) {
            this.baseUri = baseUri == null ? null : URI.create(baseUri);
            return this;
        }

        public Builder connectTimeout(Duration timeout) {
            this.connectTimeout = Objects.requireNonNull(timeout, "Connect timeout is required");
            return this;
        }

        public Builder requestTimeout(Duration timeout) {
            this.requestTimeout = timeout;
            return this;
        }

        public Builder defaultHeader(String name, String value) {
            this.defaultHeaders.put(Objects.requireNonNull(name, "Header name is required"),
                    Objects.requireNonNull(value, "Header value is required"));
            return this;
        }

        public Builder defaultHeaders(Map<String, String> headers) {
            Objects.requireNonNull(headers, "Headers map is required");
            headers.forEach(this::defaultHeader);
            return this;
        }

        public Builder httpVersion(HttpClient.Version httpVersion) {
            this.httpVersion = Objects.requireNonNull(httpVersion, "HTTP version is required");
            return this;
        }

        public Builder redirectPolicy(HttpClient.Redirect redirectPolicy) {
            this.redirectPolicy = Objects.requireNonNull(redirectPolicy, "Redirect policy is required");
            return this;
        }

        public RestClient build() {
            return new RestClient(this);
        }
    }
}
