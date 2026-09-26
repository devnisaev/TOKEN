package com.tokenrealty.rental.service;

import com.tokenrealty.rental.dto.RentalDtos.CreateLeaseRequest;
import com.tokenrealty.rental.entity.Lease;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.rental.mapper.RentalMapper;
import com.tokenrealty.rental.repository.LeaseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaseService unit tests")
class LeaseServiceTest {

    @Mock LeaseRepository leaseRepository;
    @Mock RentalMapper mapper;
    @InjectMocks LeaseService leaseService;

    @Test
    @DisplayName("create throws conflict when active lease exists")
    void create_throwsConflict_whenActiveLeaseExists() {
        UUID flatId = UUID.randomUUID();
        when(leaseRepository.existsByFlatIdAndStatus(flatId, Lease.LeaseStatus.ACTIVE)).thenReturn(true);

        var request = new CreateLeaseRequest(
                flatId, UUID.randomUUID(), "0xTenant", UUID.randomUUID(), "0xSpv",
                BigDecimal.valueOf(1500), LocalDate.now(), LocalDate.now().plusYears(1));

        assertThatThrownBy(() -> leaseService.create(request))
                .isInstanceOf(ConflictException.class);

        verify(leaseRepository, never()).save(any());
    }
}
