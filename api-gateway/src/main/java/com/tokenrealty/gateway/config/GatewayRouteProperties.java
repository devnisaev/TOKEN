package com.tokenrealty.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "tokenrealty.gateway")
public class GatewayRouteProperties {

    private List<Route> routes = new ArrayList<>();

    @Getter
    @Setter
    public static class Route {
        private String id;
        private String target;
        private List<String> paths = new ArrayList<>();
    }
}
