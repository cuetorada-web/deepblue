package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    // Paso 34 - Consulta A: Buscar animal por animalCode
    Optional<Animal> findByAnimalCode(String animalCode);

    // Paso 34 - Consulta B: Buscar cuyo commonName contenga texto ignorando mayúsculas/minúsculas
    List<Animal> findByCommonNameContainingIgnoreCase(String commonName);

    // Paso 35: Animales cuyo caso de rescate tenga determinado estado
    List<Animal> findByRescueCaseStatus(RescueStatus status);

    // Paso 36: Animales pertenecientes a un centro determinado (Animal -> RescueCase -> RescueCenter -> code)
    List<Animal> findByRescueCaseRescueCenterCode(String centerCode);

    // Parte XIII (Reto sin guía): Animales por estado de caso y especialidad del tratante
    @Query("""
        select distinct a
        from Animal a
        join a.rescueCase rc
        join a.treatments t
        join t.specialist s
        join s.expertiseAreas e
        where rc.status = :status
          and lower(e.name) = lower(:expertiseName)
        """)
    List<Animal> findAnimalsInStatusTreatedByExpertise(
        @Param("status") RescueStatus status,
        @Param("expertiseName") String expertiseName
    );
}
