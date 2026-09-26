package com.tokenrealty.issuance.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.tokenrealty.issuance.client.PropertyRegistryClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Property Registry client integration test")
class PropertyRegistryClientIntegrationTest {

    private static final WireMockServer REGISTRY = new WireMockServer(wireMockConfig().port(9999));
    private static PropertyRegistryClient propertyRegistryClient;

    @BeforeAll
    static void setUpClient() {
        REGISTRY.start();
        RestClient restClient = RestClient.builder()
                .baseUrl("http://127.0.0.1:9999/api")
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();
        propertyRegistryClient = new PropertyRegistryClient(restClient);
    }

    @AfterAll
    static void tearDown() {
        REGISTRY.stop();
    }

    @Test
    @DisplayName("setTokenInfo PATCHes Registry flat token-info endpoint")
    void setTokenInfo_patchesRegistryTokenInfo() {
        UUID flatId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        UUID buildingId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        REGISTRY.stubFor(patch(urlPathMatching("/api/v1/flats/.+/token-info"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "%s",
                                  "buildingId": "%s",
                                  "buildingName": "Demo Tower",
                                  "flatNumber": "12A",
                                  "floor": 12,
                                  "areaSqm": 85.5,
                                  "numRooms": 3,
                                  "numBathrooms": 2,
                                  "status": "TOKENIZED",
                                  "tokenContractAddress": "0xContractAddress123",
                                  "totalTokens": 1000,
                                  "tokenPriceUsd": 45.00
                                }
                                """.formatted(flatId, buildingId))));

        var response = propertyRegistryClient.setTokenInfo(
                flatId,
                "0xContractAddress123",
                1000L,
                BigDecimal.valueOf(45.00));

        assertThat(response.status()).isEqualTo("TOKENIZED");
        assertThat(response.tokenContractAddress()).isEqualTo("0xContractAddress123");
        assertThat(response.totalTokens()).isEqualTo(1000L);

        REGISTRY.verify(patchRequestedFor(urlPathMatching("/api/v1/flats/.+/token-info")));
    }
}
