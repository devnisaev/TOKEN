package com.tokenrealty.wallet.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("WalletSignRateLimitFilter unit tests")
class WalletSignRateLimitFilterTest {

    private WalletSignRateLimitProperties properties;
    private WalletSignRateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        properties = new WalletSignRateLimitProperties();
        properties.setEnabled(true);
        properties.setRequestsPerMinute(2);
        filter = new WalletSignRateLimitFilter(properties);
        chain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("allows sign requests under the limit")
    void allowsUnderLimit() throws Exception {
        UUID investorId = UUID.randomUUID();
        MockHttpServletRequest request = signRequest(investorId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);
        filter.doFilter(signRequest(investorId), new MockHttpServletResponse(), chain);

        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    @DisplayName("returns 429 when sign rate limit exceeded")
    void returns429WhenExceeded() throws Exception {
        UUID investorId = UUID.randomUUID();
        for (int i = 0; i < 2; i++) {
            filter.doFilter(signRequest(investorId), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(signRequest(investorId), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(429);
        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    @DisplayName("skips non-sign endpoints")
    void skipsOtherEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/wallets/" + UUID.randomUUID());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    private static MockHttpServletRequest signRequest(UUID investorId) {
        return new MockHttpServletRequest("POST", "/v1/wallets/" + investorId + "/sign");
    }
}
