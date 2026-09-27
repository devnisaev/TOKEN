package com.tokenrealty.reporting.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
public class NotificationClient extends DownstreamRestClientSupport {

    private static final String OPERATOR_ALERT_EVENT = "tokenrealty.reporting.operator.alert.v1";
    private static final String OPERATOR_DIGEST_EVENT = "tokenrealty.reporting.operator.digest.v1";

    public NotificationClient(@Qualifier("notificationRestClient") RestClient restClient) {
        super(restClient);
    }

    public void sendOperatorDigest(
            long openAlerts, long criticalAlerts, long decliningHealth, String avgHealthScore) {
        try {
            postVoid(
                    "/v1/notifications/send",
                    Map.of(
                            "eventType", OPERATOR_DIGEST_EVENT,
                            "payload", Map.of(
                                    "openAlerts", openAlerts,
                                    "criticalAlerts", criticalAlerts,
                                    "decliningHealth", decliningHealth,
                                    "averageHealthScore", avgHealthScore)),
                    DownstreamServices.NOTIFICATION,
                    Map.of());
        } catch (RuntimeException ex) {
            log.warn("Skipping operator digest notification: {}", ex.getMessage());
        }
    }

    public void sendOperatorAlert(String alertType, String severity, String message) {
        try {
            postVoid(
                    "/v1/notifications/send",
                    Map.of(
                            "eventType", OPERATOR_ALERT_EVENT,
                            "payload", Map.of(
                                    "alertType", alertType,
                                    "severity", severity,
                                    "message", message)),
                    DownstreamServices.NOTIFICATION,
                    Map.of());
        } catch (RuntimeException ex) {
            log.warn("Skipping operator alert notification: {}", ex.getMessage());
        }
    }
}
