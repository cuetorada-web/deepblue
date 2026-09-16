package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RescueCaseServiceImplTest {

    @Mock
    private RescueCaseRepository repository;

    @Mock
    private RescueCaseMapper mapper;

    @InjectMocks
    private RescueCaseServiceImpl service;

    @Test
    @DisplayName("Debe retornar RescueCaseResponse cuando el código existe")
    void shouldFindRescueCaseByCode() {
        // Arrange
        RescueCase rescueCase = new RescueCase();
        RescueCaseResponse response = new RescueCaseResponse(
                1L, "RES-001", LocalDate.now(), "Bahía Concha", RescueStatus.ADMITTED, "DB-CAR", "AN-001"
        );

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        // Act
        RescueCaseResponse result = service.findByCode("RES-001");

        // Assert
        assertThat(result).isEqualTo(response);
        verify(repository).findByCaseCode("RES-001");
        verify(mapper).toResponse(rescueCase);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando el código no existe")
    void shouldThrowExceptionWhenCaseNotFound() {
        // Arrange
        when(repository.findByCaseCode("RES-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.findByCode("RES-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Rescue case not found: RES-999");

        verify(mapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Debe permitir transición de estado válida (ADMITTED -> UNDER_EVALUATION)")
    void shouldAllowValidStatusTransition() {
        // Arrange
        RescueCase rescueCase = new RescueCase();
        rescueCase.setStatus(RescueStatus.ADMITTED);

        ChangeRescueStatusRequest request = new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION);
        RescueCaseResponse response = new RescueCaseResponse(
                1L, "RES-001", LocalDate.now(), "Bahía Concha", RescueStatus.UNDER_EVALUATION, "DB-CAR", "AN-001"
        );

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));
        when(repository.save(rescueCase)).thenReturn(rescueCase);
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        // Act
        RescueCaseResponse result = service.changeStatus("RES-001", request);

        // Assert
        assertThat(result.status()).isEqualTo(RescueStatus.UNDER_EVALUATION);
        verify(repository).save(rescueCase);
    }

    @Test
    @DisplayName("Debe lanzar BusinessRuleException en transición inválida y NUNCA guardar")
    void shouldThrowExceptionOnInvalidTransitionAndNeverSave() {
        // Arrange
        RescueCase rescueCase = new RescueCase();
        rescueCase.setStatus(RescueStatus.ADMITTED);

        ChangeRescueStatusRequest request = new ChangeRescueStatusRequest(RescueStatus.READY_FOR_RELEASE);

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));

        // Act & Assert
        assertThatThrownBy(() -> service.changeStatus("RES-001", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid status transition");

        verify(repository, never()).save(any());
    }
}