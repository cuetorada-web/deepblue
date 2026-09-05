package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Specialist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialistRepository extends JpaRepository<Specialist, Long> {

    Optional<Specialist> findByProfessionalCode(String professionalCode);

    // Paso 39: Encontrar especialistas activos con determinada especialidad vía JPQL
    @Query("""
        select distinct s
        from Specialist s
        join s.expertiseAreas e
        where s.active = true
          and lower(e.name) = lower(:expertiseName)
        order by s.lastName asc, s.firstName asc
        """)
    List<Specialist> findActiveByExpertise(@Param("expertiseName") String expertiseName);
}
