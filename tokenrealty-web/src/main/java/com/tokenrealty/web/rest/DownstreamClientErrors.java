package com.tokenrealty.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

import static com.tokenrealty.web.rest.DownstreamServices.ServiceSpec;

/**
 * Shared RestClient error mapping for outbound adapters.
 * 5xx / timeout → {@link ValidationException}; 404 → {@link ResourceNotFoundException} when configured.
 */
public final class DownstreamClientErrors {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DownstreamClientErrors() {
    }

    public static <T> T read(Supplier<T> call, ServiceSpec service) {
        return read(call, service, null);
    }

    public static <T> T read(Supplier<T> call, ServiceSpec service, String notFoundMessage) {
        try {
            T result = call.get();
            if (result == null && notFoundMessage != null) {
                throw new ResourceNotFoundException(notFoundMessage);
            }
            return result;
        } catch (RestClientResponseException ex) {
            throw mapResponse(ex, service, notFoundMessage);
        } catch (ResourceAccessException ex) {
            throw unavailable(service);
        }
    }

    public static <T> T readAllowNull(Supplier<T> call, ServiceSpec service) {
        try {
            return call.get();
        } catch (RestClientResponseException ex) {
            throw mapResponse(ex, service, null);
        } catch (ResourceAccessException ex) {
            throw unavailable(service);
        }
    }

    public static void run(Runnable call, ServiceSpec service) {
        run(call, service, null);
    }

    public static void run(Runnable call, ServiceSpec service, String notFoundMessage) {
        read(() -> {
            call.run();
            return null;
        }, service, notFoundMessage);
    }

    private static RuntimeException mapResponse(
            RestClientResponseException ex,
            ServiceSpec service,
            String notFoundMessage) {
        if (notFoundMessage != null && ex.getStatusCode().value() == 404) {
            return new ResourceNotFoundException(notFoundMessage);
        }
        if (ex.getStatusCode().is5xxServerError()) {
            return unavailable(service);
        }
        return rejected(service, ex);
    }

    private static ValidationException unavailable(ServiceSpec service) {
        return new ValidationException(service.label() + " unavailable");
    }

    private static ValidationException rejected(ServiceSpec service, RestClientResponseException ex) {
        String detail = problemDetail(ex);
        return new ValidationException(service.label() + " rejected request: " + detail);
    }

    static String problemDetail(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            try {
                JsonNode node = MAPPER.readTree(body);
                if (node.hasNonNull("detail")) {
                    return node.get("detail").asText();
                }
                if (node.hasNonNull("title")) {
                    return node.get("title").asText();
                }
            } catch (Exception ignored) {
                return body.length() > 200 ? body.substring(0, 200) : body;
            }
        }
        return ex.getStatusText();
    }
}
