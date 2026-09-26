package com.tokenrealty.gateway.filter;

import com.tokenrealty.gateway.config.GatewayRateLimitProperties;
import com.tokenrealty.gateway.ratelimit.RateLimitCounterStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class GatewayRateLimitFilter extends OncePerRequestFilter {

    private final GatewayRateLimitProperties properties;
    private final RateLimitCounterStore counterStore;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled() || isExempt(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!counterStore.tryConsume(clientKey(request), properties.getRequestsPerMinute())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/problem+json");
            response.getWriter().write("""
                    {"type":"about:blank","title":"Too Many Requests","status":429,\
                    "detail":"Rate limit exceeded. Try again in a minute."}""");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExempt(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/");
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
