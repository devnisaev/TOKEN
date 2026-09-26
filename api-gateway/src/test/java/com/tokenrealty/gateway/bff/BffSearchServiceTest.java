package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.SearchClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BffSearchService unit tests")
class BffSearchServiceTest {

    @Mock SearchClient searchClient;
    @InjectMocks BffSearchService searchService;

    @Test
    @DisplayName("searchListings delegates to SearchClient")
    void searchListings_delegates() {
        var pageable = PageRequest.of(0, 20);
        var expected = new SearchClient.SpringPage<>(
                List.of(new SearchClient.ListingSearchResult(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "PRIMARY",
                        BigDecimal.valueOf(45),
                        800L,
                        BigDecimal.valueOf(44.5),
                        Instant.parse("2025-09-25T10:00:00Z"))),
                1,
                1,
                20,
                0);

        when(searchClient.searchListings("tower", "PRIMARY", BigDecimal.TEN, BigDecimal.valueOf(100), pageable))
                .thenReturn(expected);

        var result = searchService.searchListings("tower", "PRIMARY", BigDecimal.TEN, BigDecimal.valueOf(100), pageable);

        assertThat(result).isSameAs(expected);
        verify(searchClient).searchListings("tower", "PRIMARY", BigDecimal.TEN, BigDecimal.valueOf(100), pageable);
    }

    @Test
    @DisplayName("searchBuildings delegates to SearchClient")
    void searchBuildings_delegates() {
        var pageable = PageRequest.of(1, 10);
        var expected = new SearchClient.SpringPage<>(
                List.of(new SearchClient.BuildingSearchResult(
                        UUID.randomUUID(),
                        Instant.parse("2025-09-20T08:00:00Z"),
                        12,
                        BigDecimal.valueOf(50),
                        BigDecimal.valueOf(49.5),
                        Instant.parse("2025-09-25T10:00:00Z"))),
                15,
                2,
                10,
                1);

        when(searchClient.searchBuildings("sunrise", pageable)).thenReturn(expected);

        var result = searchService.searchBuildings("sunrise", pageable);

        assertThat(result).isSameAs(expected);
        verify(searchClient).searchBuildings("sunrise", pageable);
    }
}
