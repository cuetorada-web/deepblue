package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    @Test
    @DisplayName("Debe lanzar BusinessRuleException si el especialista está inactivo")
    void shouldThrowExceptionWhenSpecialistIsInactive() {
        // Arrange
        Animal animal = new Animal();
        Specialist specialist = new Specialist();
        specialist.setActive(false);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001", LocalDateTime.now(), TreatmentType.WOUND_CARE, "Limpieza"
        );

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        // Act & Assert
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Specialist is not active");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar BusinessRuleException si el animal está liberado (RELEASED)")
    void shouldThrowExceptionWhenAnimalIsReleased() {
        // Arrange
        RescueCase rescueCase = new RescueCase();
        rescueCase.setStatus(RescueStatus.RELEASED);

        Animal animal = new Animal();
        animal.setRescueCase(rescueCase);

        Specialist specialist = new Specialist();
        specialist.setActive(true);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001", LocalDateTime.now(), TreatmentType.WOUND_CARE, "Limpieza"
        );

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        // Act & Assert
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot register treatment for animal in status");

        verify(treatmentRepository, never()).save(any());
    }
}