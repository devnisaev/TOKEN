package com.tokenrealty.gateway.health;

import com.tokenrealty.gateway.config.GatewayRouteProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformHealthService unit tests")
class PlatformHealthServiceTest {

    @Mock GatewayRouteProperties routeProperties;
    @Mock RestClient.Builder restClientBuilder;
    @InjectMocks PlatformHealthService platformHealthService;

    @Test
    void checkPlatform_withNoRoutes_returnsUp() {
        when(routeProperties.getRoutes()).thenReturn(List.of());

        Map<String, Object> result = platformHealthService.checkPlatform();

        assertThat(result.get("status")).isEqualTo("UP");
        assertThat(result.get("services")).isEqualTo(Map.of());
    }
}
