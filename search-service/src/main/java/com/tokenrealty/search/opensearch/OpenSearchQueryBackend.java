package com.tokenrealty.search.opensearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.search.entity.BuildingIndex;
import com.tokenrealty.search.entity.ListingIndex;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "tokenrealty.search.backend", havingValue = "opensearch")
@RequiredArgsConstructor
public class OpenSearchQueryBackend {

    private final RestClient openSearchRestClient;
    private final OpenSearchProperties properties;
    private final ObjectMapper objectMapper;

    public Page<ListingIndex> searchListings(
            String q,
            String listingType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        Map<String, Object> query = buildListingQuery(q, listingType, minPrice, maxPrice);
        return search(query, properties.listingsIndex(), pageable, this::toListingIndex);
    }

    public Page<BuildingIndex> searchBuildings(String q, Pageable pageable) {
        Map<String, Object> query = buildTextQuery(q);
        return search(query, properties.buildingsIndex(), pageable, this::toBuildingIndex);
    }

    private <T> Page<T> search(
            Map<String, Object> query,
            String index,
            Pageable pageable,
            java.util.function.Function<JsonNode, T> mapper) {
        query.put("from", pageable.getOffset());
        query.put("size", pageable.getPageSize());
        try {
            String body = objectMapper.writeValueAsString(query);
            String response = openSearchRestClient.post()
                    .uri("/{index}/_search", index)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            long total = root.path("hits").path("total").path("value").asLong();
            List<T> results = new ArrayList<>();
            for (JsonNode hit : root.path("hits").path("hits")) {
                results.add(mapper.apply(hit.path("_source")));
            }
            return new PageImpl<>(results, pageable, total);
        } catch (Exception ex) {
            throw new IllegalStateException("OpenSearch query failed for index " + index, ex);
        }
    }

    private Map<String, Object> buildListingQuery(
            String q,
            String listingType,
            BigDecimal minPrice,
            BigDecimal maxPrice) {
        Map<String, Object> query = buildTextQuery(q);
        List<Map<String, Object>> filters = new ArrayList<>();
        if (listingType != null) {
            filters.add(termFilter("listingType", listingType));
        }
        if (minPrice != null) {
            filters.add(rangeGte("priceUsd", minPrice));
        }
        if (maxPrice != null) {
            filters.add(rangeLte("priceUsd", maxPrice));
        }
        if (filters.isEmpty()) {
            return query;
        }
        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("must", List.of(query.get("query")));
        bool.put("filter", filters);
        return Map.of("query", Map.of("bool", bool));
    }

    private Map<String, Object> buildTextQuery(String q) {
        if (q == null || q.isBlank()) {
            return Map.of("query", Map.of("match_all", Map.of()));
        }
        return Map.of("query", Map.of("multi_match", Map.of(
                "query", q,
                "fields", List.of("searchText", "buildingName", "city"))));
    }

    private Map<String, Object> termFilter(String field, Object value) {
        return Map.of("term", Map.of(field, value));
    }

    private Map<String, Object> rangeGte(String field, BigDecimal value) {
        return Map.of("range", Map.of(field, Map.of("gte", value)));
    }

    private Map<String, Object> rangeLte(String field, BigDecimal value) {
        return Map.of("range", Map.of(field, Map.of("lte", value)));
    }

    private ListingIndex toListingIndex(JsonNode source) {
        ListingIndex index = ListingIndex.builder()
                .listingId(uuid(source, "listingId"))
                .flatId(uuid(source, "flatId"))
                .buildingId(uuid(source, "buildingId"))
                .buildingName(text(source, "buildingName"))
                .city(text(source, "city"))
                .listingType(text(source, "listingType"))
                .priceUsd(decimal(source, "priceUsd"))
                .navPerTokenUsd(decimal(source, "navPerTokenUsd"))
                .searchText(text(source, "searchText"))
                .indexedAt(instant(source, "indexedAt"))
                .build();
        index.setSourceEventId(UUID.randomUUID());
        return index;
    }

    private BuildingIndex toBuildingIndex(JsonNode source) {
        BuildingIndex index = BuildingIndex.builder()
                .buildingId(uuid(source, "buildingId"))
                .buildingName(text(source, "buildingName"))
                .city(text(source, "city"))
                .approvedAt(instant(source, "approvedAt"))
                .flatCount(source.path("flatCount").asInt())
                .latestTokenPriceUsd(decimal(source, "latestTokenPriceUsd"))
                .latestNavPerTokenUsd(decimal(source, "latestNavPerTokenUsd"))
                .searchText(text(source, "searchText"))
                .indexedAt(instant(source, "indexedAt"))
                .build();
        index.setSourceEventId(UUID.randomUUID());
        return index;
    }

    private UUID uuid(JsonNode source, String field) {
        String value = text(source, field);
        return value != null ? UUID.fromString(value) : null;
    }

    private String text(JsonNode source, String field) {
        JsonNode node = source.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private BigDecimal decimal(JsonNode source, String field) {
        String value = text(source, field);
        return value != null ? new BigDecimal(value) : null;
    }

    private Instant instant(JsonNode source, String field) {
        String value = text(source, field);
        return value != null ? Instant.parse(value) : null;
    }
}
