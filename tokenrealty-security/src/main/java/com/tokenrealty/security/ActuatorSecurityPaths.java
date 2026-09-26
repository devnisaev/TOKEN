package com.tokenrealty.security;

public final class ActuatorSecurityPaths {

    public static final String[] PUBLIC = {
            "/actuator/health",
            "/actuator/info",
            "/actuator/metrics",
            "/actuator/prometheus"
    };

    private ActuatorSecurityPaths() {
    }
}
