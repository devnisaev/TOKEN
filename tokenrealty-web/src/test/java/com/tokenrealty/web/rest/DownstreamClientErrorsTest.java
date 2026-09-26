package com.tokenrealty.web.rest;

import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import static com.tokenrealty.web.rest.DownstreamServices.PAYMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DownstreamClientErrorsTest {

    @Test
    void read_returnsValue() {
        String result = DownstreamClientErrors.read(() -> "ok", PAYMENT);
        assertThat(result).isEqualTo("ok");
    }

    @Test
    void read_nullWithNotFoundMessage_throwsResourceNotFound() {
        assertThatThrownBy(() -> DownstreamClientErrors.read(() -> null, PAYMENT, "Payment not found"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment not found");
    }

    @Test
    void read_404WithNotFoundMessage_throwsResourceNotFound() {
        assertThatThrownBy(() -> DownstreamClientErrors.read(
                        () -> {
                            throw HttpClientErrorException.create(
                                    HttpStatus.NOT_FOUND, "Not Found", null, null, null);
                        },
                        PAYMENT,
                        "Payment not found"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment not found");
    }

    @Test
    void read_5xx_throwsUnavailable() {
        assertThatThrownBy(() -> DownstreamClientErrors.read(
                        () -> {
                            throw HttpServerErrorException.create(
                                    HttpStatus.BAD_GATEWAY, "Bad Gateway", null, null, null);
                        },
                        PAYMENT))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Payment service unavailable");
    }

    @Test
    void read_4xx_throwsRejectedWithProblemDetail() {
        assertThatThrownBy(() -> DownstreamClientErrors.read(
                        () -> {
                            throw HttpClientErrorException.create(
                                    HttpStatus.BAD_REQUEST,
                                    "Bad Request",
                                    null,
                                    "{\"detail\":\"Invalid amount\"}".getBytes(),
                                    null);
                        },
                        PAYMENT))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Payment service rejected request: Invalid amount");
    }

    @Test
    void readAllowNull_returnsNull() {
        String value = DownstreamClientErrors.readAllowNull(() -> null, PAYMENT);
        assertThat(value).isNull();
    }

    @Test
    void readAllowNotFound_404_returnsNull() {
        String value = DownstreamClientErrors.readAllowNotFound(
                () -> {
                    throw HttpClientErrorException.create(
                            HttpStatus.NOT_FOUND, "Not Found", null, null, null);
                },
                PAYMENT);
        assertThat(value).isNull();
    }

    @Test
    void read_timeout_throwsUnavailable() {
        assertThatThrownBy(() -> DownstreamClientErrors.read(
                        () -> {
                            throw new ResourceAccessException("timeout");
                        },
                        PAYMENT))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Payment service unavailable");
    }

    @Test
    void run_executesWithoutError() {
        DownstreamClientErrors.run(() -> { }, PAYMENT);
    }
}
