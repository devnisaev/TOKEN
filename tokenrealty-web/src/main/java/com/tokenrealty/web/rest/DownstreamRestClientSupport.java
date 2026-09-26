package com.tokenrealty.web.rest;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;

import static com.tokenrealty.web.rest.DownstreamServices.ServiceSpec;

/**
 * Base for outbound REST adapters — wraps {@link RestClientOperations} + {@link DownstreamClientErrors}.
 */
public abstract class DownstreamRestClientSupport {

    private final RestClientOperations http;

    protected DownstreamRestClientSupport(RestClient restClient) {
        this.http = new RestClientOperations(restClient);
    }

    protected RestClientOperations http() {
        return http;
    }

    protected <T> T get(String uri, Class<T> responseType, ServiceSpec service, Object... uriVariables) {
        return DownstreamClientErrors.read(
                () -> http.get(uri, responseType, uriVariables),
                service);
    }

    protected <T> T get(
            String uri,
            Class<T> responseType,
            ServiceSpec service,
            String notFoundMessage,
            Object... uriVariables) {
        return DownstreamClientErrors.read(
                () -> http.get(uri, responseType, uriVariables),
                service,
                notFoundMessage);
    }

    protected <T> T get(
            Function<UriBuilder, URI> uriFunction,
            Class<T> responseType,
            ServiceSpec service,
            String notFoundMessage) {
        return DownstreamClientErrors.read(
                () -> http.get(uriFunction, responseType),
                service,
                notFoundMessage);
    }

    protected <T> T getAllowNull(String uri, Class<T> responseType, ServiceSpec service, Object... uriVariables) {
        return DownstreamClientErrors.readAllowNull(
                () -> http.get(uri, responseType, uriVariables),
                service);
    }

    protected <T> T getAllowNotFound(String uri, Class<T> responseType, ServiceSpec service, Object... uriVariables) {
        return DownstreamClientErrors.readAllowNotFound(
                () -> http.get(uri, responseType, uriVariables),
                service);
    }

    protected <T> T get(
            Function<UriBuilder, URI> uriFunction,
            ParameterizedTypeReference<T> responseType,
            ServiceSpec service) {
        return DownstreamClientErrors.read(
                () -> http.get(uriFunction, responseType),
                service);
    }

    protected <T> T getAllowNotFound(
            Function<UriBuilder, URI> uriFunction,
            ParameterizedTypeReference<T> responseType,
            ServiceSpec service) {
        return DownstreamClientErrors.readAllowNotFound(
                () -> http.get(uriFunction, responseType),
                service);
    }

    protected <T> T post(String uri, Object body, Class<T> responseType, ServiceSpec service, Object... uriVars) {
        return DownstreamClientErrors.read(
                () -> http.post(uri, body, responseType, uriVars),
                service);
    }

    protected <T> T post(
            String uri,
            Object body,
            Class<T> responseType,
            ServiceSpec service,
            Map<String, String> headers,
            Object... uriVars) {
        return DownstreamClientErrors.read(
                () -> http.post(uri, body, responseType, headers, uriVars),
                service);
    }

    protected void postVoid(
            String uri,
            Object body,
            ServiceSpec service,
            Map<String, String> headers,
            Object... uriVars) {
        DownstreamClientErrors.run(
                () -> http.postVoidWithBody(uri, body, headers, uriVars),
                service);
    }

    protected void patchVoid(String uri, ServiceSpec service, Object... uriVariables) {
        DownstreamClientErrors.run(
                () -> http.patchVoid(uri, uriVariables),
                service);
    }

    protected void patchVoid(Function<UriBuilder, URI> uriFunction, ServiceSpec service) {
        DownstreamClientErrors.run(
                () -> http.patchVoid(uriFunction),
                service);
    }

    protected <T> T patch(
            Function<UriBuilder, URI> uriFunction,
            Class<T> responseType,
            ServiceSpec service) {
        return DownstreamClientErrors.read(
                () -> http.patch(uriFunction, responseType),
                service);
    }

    protected <T> T patch(String uri, Class<T> responseType, ServiceSpec service, Object... uriVariables) {
        return DownstreamClientErrors.read(
                () -> http.patch(uri, responseType, uriVariables),
                service);
    }
}
