package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @InjectMocks
    private AnimalServiceImpl service;

    @Test
    @DisplayName("Debe retornar true cuando el animal está en evaluación o rehabilitación")
    void shouldReturnTrueWhenAnimalCanReceiveTreatment() {
        // Arrange
        RescueCase rescueCase = new RescueCase();
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);

        Animal animal = new Animal();
        animal.setRescueCase(rescueCase);

        when(animalRepository.findByAnimalCode("AN-100")).thenReturn(Optional.of(animal));

        // Act
        boolean result = service.canReceiveTreatment("AN-100");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Debe retornar false cuando el animal está liberado (RELEASED)")
    void shouldReturnFalseWhenAnimalCannotReceiveTreatment() {
        // Arrange
        RescueCase rescueCase = new RescueCase();
        rescueCase.setStatus(RescueStatus.RELEASED);

        Animal animal = new Animal();
        animal.setRescueCase(rescueCase);

        when(animalRepository.findByAnimalCode("AN-100")).thenReturn(Optional.of(animal));

        // Act
        boolean result = service.canReceiveTreatment("AN-100");

        // Assert
        assertThat(result).isFalse();
    }
}