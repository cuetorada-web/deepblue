package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RescueCaseRepository extends JpaRepository<RescueCase, Long> {
    
    // Consulta A: Buscar por caseCode
    Optional<RescueCase> findByCaseCode(String caseCode);

    // Consulta B: Buscar por status ordenados por rescueDate ASC
    List<RescueCase> findByStatusOrderByRescueDateAsc(RescueStatus status);

    // Consulta C: Buscar casos pertenecientes a un centro mediante su código (navegación de relación)
    List<RescueCase> findByRescueCenterCode(String code);

    // Consulta con fechas: Casos posteriores a fecha ordenados desc
    List<RescueCase> findByRescueDateAfterOrderByRescueDateDesc(LocalDate date);
}
