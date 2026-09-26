package com.tokenrealty.web.rest;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Shared {@link RestClient} transport — raw verbs with optional headers.
 * Error mapping stays in {@link DownstreamClientErrors} + {@link DownstreamRestClientSupport}.
 */
public final class RestClientOperations {

    private final RestClient client;

    public RestClientOperations(RestClient client) {
        this.client = client;
    }

    public RestClient client() {
        return client;
    }

    public <T> T get(String uri, Class<T> responseType, Object... uriVariables) {
        return client.get().uri(uri, uriVariables).retrieve().body(responseType);
    }

    public <T> T get(Function<UriBuilder, URI> uriFunction, Class<T> responseType) {
        return client.get().uri(uriFunction).retrieve().body(responseType);
    }

    public <T> T get(String uri, Class<T> responseType, Map<String, String> headers, Object... uriVariables) {
        return applyHeaders(client.get().uri(uri, uriVariables), headers).retrieve().body(responseType);
    }

    public <T> T get(Function<UriBuilder, URI> uriFunction, ParameterizedTypeReference<T> responseType) {
        return client.get().uri(uriFunction).retrieve().body(responseType);
    }

    public <T> T post(String uri, Object requestBody, Class<T> responseType, Object... uriVariables) {
        return client.post().uri(uri, uriVariables).body(requestBody).retrieve().body(responseType);
    }

    public <T> T post(
            String uri,
            Object requestBody,
            Class<T> responseType,
            Map<String, String> headers,
            Object... uriVariables) {
        return applyHeaders(client.post().uri(uri, uriVariables), headers)
                .body(requestBody)
                .retrieve()
                .body(responseType);
    }

    public void postVoid(String uri, Object... uriVariables) {
        client.post().uri(uri, uriVariables).retrieve().toBodilessEntity();
    }

    public void postVoidWithBody(String uri, Object requestBody, Object... uriVariables) {
        client.post().uri(uri, uriVariables).body(requestBody).retrieve().toBodilessEntity();
    }

    public void postVoidWithBody(
            String uri,
            Object requestBody,
            Map<String, String> headers,
            Object... uriVariables) {
        applyHeaders(client.post().uri(uri, uriVariables), headers)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }

    public void patchVoid(String uri, Object... uriVariables) {
        client.patch().uri(uri, uriVariables).retrieve().toBodilessEntity();
    }

    public void patchVoid(String uri, Object requestBody, Object... uriVariables) {
        client.patch().uri(uri, uriVariables).body(requestBody).retrieve().toBodilessEntity();
    }

    public void patchVoid(String uri, Map<String, String> headers, Object... uriVariables) {
        applyHeaders(client.patch().uri(uri, uriVariables), headers).retrieve().toBodilessEntity();
    }

    public <T> T put(String uri, Object requestBody, Class<T> responseType, Object... uriVariables) {
        return client.put().uri(uri, uriVariables).body(requestBody).retrieve().body(responseType);
    }

    public static Map<String, String> header(String name, String value) {
        return Map.of(name, value);
    }

    public static Map<String, String> idempotencyKey(String key) {
        return Map.of(RestHeaders.IDEMPOTENCY_KEY, key);
    }

    public static Map<String, String> merge(Map<String, String> base, String name, String value) {
        var merged = new LinkedHashMap<>(base);
        merged.put(name, value);
        return Map.copyOf(merged);
    }

    private static RestClient.RequestHeadersSpec<?> applyHeaders(
            RestClient.RequestHeadersSpec<?> spec,
            Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return spec;
        }
        RestClient.RequestHeadersSpec<?> current = spec;
        for (var entry : headers.entrySet()) {
            current = current.header(entry.getKey(), entry.getValue());
        }
        return current;
    }

    private static RestClient.RequestBodySpec applyHeaders(
            RestClient.RequestBodySpec spec,
            Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return spec;
        }
        RestClient.RequestBodySpec current = spec;
        for (var entry : headers.entrySet()) {
            current = current.header(entry.getKey(), entry.getValue());
        }
        return current;
    }
}
