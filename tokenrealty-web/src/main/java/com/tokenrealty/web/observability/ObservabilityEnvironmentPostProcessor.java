package com.tokenrealty.web.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

public class ObservabilityEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE = "tokenrealtyObservabilityDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(SOURCE)) {
            return;
        }
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("management.endpoints.web.exposure.include", "health,info,metrics,prometheus");
        defaults.put("management.metrics.tags.application", "${spring.application.name:tokenrealty}");
        defaults.put("management.endpoint.prometheus.access", "read_only");
        environment.getPropertySources().addLast(new MapPropertySource(SOURCE, defaults));
    }
}
