package com.tokenrealty.events.avro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Registers Avro schema JSON with Apicurio Registry REST API.
 */
public final class AvroSchemaRegistrar {

    private static final Logger log = LoggerFactory.getLogger(AvroSchemaRegistrar.class);
    private static final String DEFAULT_GROUP = "default";

    private final HttpClient httpClient;
    private final String registryBaseUrl;

    public AvroSchemaRegistrar(String registryBaseUrl) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), registryBaseUrl);
    }

    AvroSchemaRegistrar(HttpClient httpClient, String registryBaseUrl) {
        this.httpClient = httpClient;
        this.registryBaseUrl = trimTrailingSlash(registryBaseUrl);
    }

    public void registerArtifact(String artifactId, String schemaJson) throws IOException, InterruptedException {
        registerArtifact(DEFAULT_GROUP, artifactId, schemaJson);
    }

    public void registerArtifact(String groupId, String artifactId, String schemaJson)
            throws IOException, InterruptedException {
        String url = registryBaseUrl
                + "/apis/registry/v2/groups/"
                + groupId
                + "/artifacts?artifactId="
                + artifactId
                + "&ifExists=RETURN_OR_UPDATE";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(schemaJson, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException("Schema registry returned " + response.statusCode() + ": " + response.body());
        }
        log.info("Registered Avro artifact {} in group {} (HTTP {})", artifactId, groupId, response.statusCode());
    }

    public void registerFromClasspath(String artifactId, String resourcePath)
            throws IOException, InterruptedException {
        try (InputStream in = AvroSchemaRegistrar.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Avro schema not found: " + resourcePath);
            }
            String schemaJson = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            registerArtifact(artifactId, schemaJson);
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:8092";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
