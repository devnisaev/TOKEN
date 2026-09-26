package com.tokenrealty.registry.config;

import com.tokenrealty.registry.dto.PropertyDtos.CreateSpvRequest;
import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.registry.entity.Building.BuildingStatus;
import com.tokenrealty.registry.entity.Flat;
import com.tokenrealty.registry.entity.SpvEntity;
import com.tokenrealty.registry.entity.Valuation;
import com.tokenrealty.registry.repository.BuildingRepository;
import com.tokenrealty.registry.repository.FlatRepository;
import com.tokenrealty.registry.repository.ValuationRepository;
import com.tokenrealty.registry.service.SpvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DevPropertyDataInitializer implements ApplicationRunner {

    private static final String DEMO_ADDRESS = "1 Chui Ave";
    private static final String DEMO_CITY = "Bishkek";
    private static final String SPV_WALLET = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266";

    private final BuildingRepository buildingRepository;
    private final FlatRepository flatRepository;
    private final ValuationRepository valuationRepository;
    private final SpvService spvService;

    @Value("${tokenrealty.registry.seed-dev-properties:true}")
    private boolean seedDevProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedDevProperties) {
            return;
        }
        if (buildingRepository.existsByAddressAndCity(DEMO_ADDRESS, DEMO_CITY)) {
            log.debug("Demo property already seeded — skip");
            return;
        }

        Building building = buildingRepository.save(Building.builder()
                .name("Sunrise Tower")
                .address(DEMO_ADDRESS)
                .city(DEMO_CITY)
                .country("KG")
                .postalCode("720001")
                .totalFloors(12)
                .totalFlats(48)
                .constructionYear(2022)
                .totalAreaSqm(4800.0)
                .latitude(42.8746)
                .longitude(74.5698)
                .status(BuildingStatus.PENDING_REVIEW)
                .build());

        Flat flat = flatRepository.save(Flat.builder()
                .building(building)
                .flatNumber("101")
                .floor(1)
                .areaSqm(85.0)
                .netUsableAreaSqm(72.0)
                .numRooms(3)
                .numBathrooms(2)
                .cadastralReference("KG-BISH-2022-001")
                .status(Flat.FlatStatus.AVAILABLE)
                .build());

        valuationRepository.save(Valuation.builder()
                .flat(flat)
                .valuationDate(LocalDate.of(2024, 6, 1))
                .valueUsd(new BigDecimal("120000.00"))
                .appraiserName("TokenRealty Appraisals")
                .method(Valuation.ValuationMethod.INCOME_APPROACH)
                .operatingExpensesEstimateUsd(new BigDecimal("2400.00"))
                .targetRentalYieldPct(new BigDecimal("6.50"))
                .build());

        spvService.create(building.getId(), new CreateSpvRequest(
                "Sunrise SPV LLC",
                "KG-2024-001",
                "KG",
                LocalDate.of(2024, 1, 15),
                DEMO_ADDRESS,
                SPV_WALLET,
                "TAX-001",
                "Bishkek",
                SpvEntity.OwnershipType.SPV_SHARE_EQUITY));

        log.info("Seeded demo property building={} flat={} (Sunrise Tower / 101)", building.getId(), flat.getId());
    }
}
