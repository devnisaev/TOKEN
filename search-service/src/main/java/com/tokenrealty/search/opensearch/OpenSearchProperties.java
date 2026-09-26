package com.tokenrealty.search.opensearch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tokenrealty.search.opensearch")
public record OpenSearchProperties(
        String url,
        String listingsIndex,
        String buildingsIndex
) {
    public OpenSearchProperties {
        if (url == null || url.isBlank()) {
            url = "http://localhost:9200";
        }
        if (listingsIndex == null || listingsIndex.isBlank()) {
            listingsIndex = "tokenrealty-listings";
        }
        if (buildingsIndex == null || buildingsIndex.isBlank()) {
            buildingsIndex = "tokenrealty-buildings";
        }
    }
}
