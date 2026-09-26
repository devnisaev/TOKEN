package com.tokenrealty.wallet.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class WalletSignRateLimitFilter extends OncePerRequestFilter {

    private static final Pattern SIGN_PATH =
            Pattern.compile("/v1/wallets/([0-9a-f-]{36})/sign/?$", Pattern.CASE_INSENSITIVE);

    private final WalletSignRateLimitProperties properties;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return extractInvestorId(request.getRequestURI()) == null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID investorId = extractInvestorId(request.getRequestURI());
        if (investorId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = investorId.toString();
        long windowStart = Instant.now().getEpochSecond() / 60;
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(windowStart));

        synchronized (counter) {
            if (counter.windowStart != windowStart) {
                counter.windowStart = windowStart;
                counter.count.set(0);
            }
            if (counter.count.incrementAndGet() > properties.getRequestsPerMinute()) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/problem+json");
                response.getWriter().write("""
                        {"type":"about:blank","title":"Too Many Requests","status":429,\
                        "detail":"Sign rate limit exceeded. Try again in a minute."}""");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static UUID extractInvestorId(String uri) {
        Matcher matcher = SIGN_PATH.matcher(uri);
        if (!matcher.find()) {
            return null;
        }
        try {
            return UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static final class WindowCounter {
        private long windowStart;
        private final AtomicInteger count = new AtomicInteger();

        private WindowCounter(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
