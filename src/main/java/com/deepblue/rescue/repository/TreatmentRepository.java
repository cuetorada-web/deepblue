package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {

    // Paso 41: Query Method cronológico por animal
    List<Treatment> findByAnimalIdOrderByPerformedAtAsc(Long animalId);

    // Paso 42: JPQL entre dos fechas ordenado cronológicamente
    @Query("""
        select t
        from Treatment t
        where t.performedAt between :start and :end
        order by t.performedAt asc
        """)
    List<Treatment> findTreatmentsBetweenDates(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    // Paso 43: JPQL navegando Treatment -> Animal -> RescueCase -> RescueCenter
    @Query("""
        select t
        from Treatment t
        join t.animal a
        join a.rescueCase rc
        join rc.rescueCenter center
        where center.code = :centerCode
        order by t.performedAt asc
        """)
    List<Treatment> findTreatmentsByRescueCenterCode(@Param("centerCode") String centerCode);

    // Paso 44: JPQL con Specialist y su colección N:M Expertise
    @Query("""
        select distinct t
        from Treatment t
        join t.specialist s
        join s.expertiseAreas e
        where lower(e.name) = lower(:expertiseName)
        order by t.performedAt asc
        """)
    List<Treatment> findTreatmentsBySpecialistExpertise(@Param("expertiseName") String expertiseName);
}
