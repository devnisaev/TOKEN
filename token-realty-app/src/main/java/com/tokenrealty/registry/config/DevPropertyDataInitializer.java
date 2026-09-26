package com.tokenrealty.registry.config;

import com.tokenrealty.registry.dto.PropertyDtos.CreateSpvRequest;
import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.registry.entity.Building.BuildingStatus;
import com.tokenrealty.registry.entity.Flat;
import com.tokenrealty.registry.entity.PropertyDocument;
import com.tokenrealty.registry.entity.SpvEntity;
import com.tokenrealty.registry.entity.Valuation;
import com.tokenrealty.registry.mapper.PropertyMapper;
import com.tokenrealty.registry.repository.BuildingRepository;
import com.tokenrealty.registry.repository.FlatRepository;
import com.tokenrealty.registry.repository.PropertyDocumentRepository;
import com.tokenrealty.registry.repository.SpvRepository;
import com.tokenrealty.registry.repository.ValuationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DevPropertyDataInitializer implements ApplicationRunner {

    public static final UUID DEMO_BUILDING_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final UUID DEMO_FLAT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID DEMO_SPV_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID DEMO_DOCUMENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private static final String DEMO_ADDRESS = "1 Chui Ave";
    private static final String DEMO_CITY = "Bishkek";
    public static final String DEMO_SPV_WALLET = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266";

    private final BuildingRepository buildingRepository;
    private final FlatRepository flatRepository;
    private final ValuationRepository valuationRepository;
    private final SpvRepository spvRepository;
    private final PropertyDocumentRepository documentRepository;
    private final PropertyMapper mapper;

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

        Building building = Building.builder()
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
                .build();
        building.setId(DEMO_BUILDING_ID);
        building = buildingRepository.save(building);

        Flat flat = Flat.builder()
                .building(building)
                .flatNumber("101")
                .floor(1)
                .areaSqm(85.0)
                .netUsableAreaSqm(72.0)
                .numRooms(3)
                .numBathrooms(2)
                .cadastralReference("KG-BISH-2022-001")
                .status(Flat.FlatStatus.AVAILABLE)
                .build();
        flat.setId(DEMO_FLAT_ID);
        flat = flatRepository.save(flat);

        valuationRepository.save(Valuation.builder()
                .flat(flat)
                .valuationDate(LocalDate.of(2024, 6, 1))
                .valueUsd(new BigDecimal("120000.00"))
                .appraiserName("TokenRealty Appraisals")
                .method(Valuation.ValuationMethod.INCOME_APPROACH)
                .operatingExpensesEstimateUsd(new BigDecimal("2400.00"))
                .targetRentalYieldPct(new BigDecimal("6.50"))
                .build());

        SpvEntity spv = mapper.toSpv(new CreateSpvRequest(
                "Sunrise SPV LLC",
                "KG-2024-001",
                "KG",
                LocalDate.of(2024, 1, 15),
                DEMO_ADDRESS,
                DEMO_SPV_WALLET,
                "TAX-001",
                "Bishkek",
                SpvEntity.OwnershipType.SPV_SHARE_EQUITY));
        spv.setId(DEMO_SPV_ID);
        spv.setBuilding(building);
        spvRepository.save(spv);
        building.setStatus(BuildingStatus.APPROVED);
        buildingRepository.save(building);

        if (!documentRepository.existsById(DEMO_DOCUMENT_ID)) {
            PropertyDocument document = PropertyDocument.builder()
                    .building(building)
                    .flat(flat)
                    .documentName("Sunrise Tower Title Deed")
                    .documentType(PropertyDocument.DocumentType.TITLE_DEED)
                    .ipfsCid("QmDemoTitleDeedCid")
                    .uploadedBy("dev-seed")
                    .isVerified(false)
                    .build();
            document.setId(DEMO_DOCUMENT_ID);
            documentRepository.save(document);
        }

        log.info("Seeded demo property building={} flat={} document={} (Sunrise Tower / 101)",
                building.getId(), flat.getId(), DEMO_DOCUMENT_ID);
    }
}
