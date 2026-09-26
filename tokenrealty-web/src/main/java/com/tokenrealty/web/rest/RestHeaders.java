package com.tokenrealty.web.rest;

public final class RestHeaders {

    public static final String TRACE_ID = "X-Trace-Id";
    public static final String TRACE_ID_MDC = "traceId";
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private RestHeaders() {
    }
}
