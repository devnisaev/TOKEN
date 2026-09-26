package com.tokenrealty.gateway.health;

import com.tokenrealty.gateway.config.GatewayRouteProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlatformHealthService {

    private static final String HEALTH_PATH = "/api/actuator/health";

    private final GatewayRouteProperties routeProperties;
    private final RestClient.Builder restClientBuilder;

    public Map<String, Object> checkPlatform() {
        Map<String, Object> services = new LinkedHashMap<>();
        boolean allUp = true;

        for (GatewayRouteProperties.Route route : routeProperties.getRoutes()) {
            String status = probe(route.getTarget());
            services.put(route.getId(), status);
            if (!"UP".equals(status)) {
                allUp = false;
            }
        }

        return Map.of(
                "status", allUp ? "UP" : "DEGRADED",
                "services", services);
    }

    private String probe(String targetBase) {
        try {
            restClientBuilder.baseUrl(targetBase).build()
                    .get()
                    .uri(HEALTH_PATH)
                    .retrieve()
                    .toBodilessEntity();
            return "UP";
        } catch (RestClientException ex) {
            return "DOWN";
        }
    }
}
