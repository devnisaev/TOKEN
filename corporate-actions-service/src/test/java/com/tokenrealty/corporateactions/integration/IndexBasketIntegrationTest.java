package com.tokenrealty.corporateactions.integration;

import com.tokenrealty.corporateactions.dto.CorporateActionDtos.*;
import com.tokenrealty.corporateactions.entity.IndexDefinition;
import com.tokenrealty.corporateactions.service.IndexBasketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Index basket — Phase 15 integration")
class IndexBasketIntegrationTest {

    @Autowired IndexBasketService indexBasketService;

    @Test
    @DisplayName("Create and activate index with weighted constituents")
    void createAndActivateIndex() {
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        IndexDefinitionResponse created = indexBasketService.create(CreateIndexDefinitionRequest.builder()
                .name("US Commercial Operators")
                .symbol("USCO")
                .description("Stabilized US commercial basket")
                .constituents(List.of(
                        new IndexConstituentRequest(c1, 6000),
                        new IndexConstituentRequest(c2, 4000)))
                .build());
        assertThat(created.constituents()).hasSize(2);

        IndexDefinitionResponse active = indexBasketService.activate(created.id());
        assertThat(active.status()).isEqualTo(IndexDefinition.IndexStatus.ACTIVE);
    }
}
