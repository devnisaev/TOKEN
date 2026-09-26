package com.tokenrealty.security.client;

import com.tokenrealty.security.ServiceTokenProvider;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

public final class ServiceRestClientBuilder {

    /** Must match {@code com.tokenrealty.web.rest.RestHeaders#TRACE_ID}. */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    /** Must match {@code com.tokenrealty.web.rest.RestHeaders#TRACE_ID_MDC}. */
    private static final String TRACE_ID_MDC = "traceId";

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private ServiceRestClientBuilder() {
    }

    public static RestClient build(String baseUrl, ObjectProvider<ServiceTokenProvider> serviceTokenProvider) {
        return build(baseUrl, DEFAULT_TIMEOUT, serviceTokenProvider);
    }

    public static RestClient build(
            String baseUrl,
            Duration readTimeout,
            ObjectProvider<ServiceTokenProvider> serviceTokenProvider) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = (int) readTimeout.toMillis();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory);

        builder.requestInterceptor((request, body, execution) -> {
            serviceTokenProvider.ifAvailable(provider ->
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getAccessToken()));
            String traceId = MDC.get(TRACE_ID_MDC);
            if (traceId != null && !traceId.isBlank()) {
                request.getHeaders().set(TRACE_ID_HEADER, traceId);
            }
            return execution.execute(request, body);
        });
        return builder.build();
    }
}
