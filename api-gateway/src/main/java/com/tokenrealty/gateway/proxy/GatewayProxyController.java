package com.tokenrealty.gateway.proxy;

import com.tokenrealty.gateway.config.GatewayRouteProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class GatewayProxyController {

    private final GatewayRouteProperties routeProperties;
    private final RestClient.Builder restClientBuilder;

    @RequestMapping("/api/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws IOException {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String targetBase = resolveTarget(path);
        String targetUrl = targetBase + path + (query != null ? "?" + query : "");

        RestClient client = restClientBuilder.baseUrl(targetBase).build();
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        RestClient.RequestBodySpec spec = client.method(method)
                .uri(targetUrl.replace(targetBase, ""))
                .headers(headers -> copyHeaders(request, headers));

        RestClient.ResponseSpec response = body.length == 0
                ? spec.retrieve()
                : spec.body(body).retrieve();

        return response.toEntity(byte[].class);
    }

    private String resolveTarget(String path) {
        for (GatewayRouteProperties.Route route : routeProperties.getRoutes()) {
            if (matches(path, route.getPaths())) {
                return route.getTarget();
            }
        }
        throw new GatewayRouteNotFoundException("No route for path: " + path);
    }

    private static boolean matches(String path, List<String> prefixes) {
        return prefixes.stream().anyMatch(path::startsWith);
    }

    private static void copyHeaders(HttpServletRequest request, HttpHeaders headers) {
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if ("host".equalsIgnoreCase(name) || "content-length".equalsIgnoreCase(name)) {
                continue;
            }
            headers.add(name, request.getHeader(name));
        }
    }
}
