package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");
    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Paso 47: Validar que Flyway aplicó las migraciones V1, V2 y V3")
    void testFlywayMigrationsApplied() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1', '2', '3') AND success = true",
                Integer.class
        );
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Paso 59: Probar constraint UNIQUE en animalCode arrojando DataIntegrityViolationException")
    void testUniqueConstraintViolation() {
        RescueCenter center = new RescueCenter("DB-VAL", "Validation Center", "Cartagena");
        rescueCenterRepository.save(center);

        RescueCase case1 = new RescueCase("CASE-VAL-01", LocalDate.now(), "Playa Blanca", RescueStatus.ADMITTED);
        center.addCase(case1);
        rescueCaseRepository.save(case1);

        Animal a1 = new Animal("AN-DUPLICATE", "Turtle 1", "Chelonia mydas", AnimalSex.MALE);
        case1.assignAnimal(a1);
        animalRepository.saveAndFlush(a1);

        RescueCase case2 = new RescueCase("CASE-VAL-02", LocalDate.now(), "Playa Blanca", RescueStatus.ADMITTED);
        center.addCase(case2);
        rescueCaseRepository.save(case2);

        Animal a2 = new Animal("AN-DUPLICATE", "Turtle 2", "Chelonia mydas", AnimalSex.FEMALE);
        case2.assignAnimal(a2);

        assertThrows(DataIntegrityViolationException.class, () -> {
            animalRepository.saveAndFlush(a2);
        });
    }

    @Test
    @DisplayName("Pasos 65 y 66: Reto Integrador completo (Escenario tortuga con Elena Vargas) y consultas")
    void testIntegratorScenario() {
        // 1. Centro
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");
        rescueCenterRepository.save(center);

        // 2. Caso
        RescueCase rescueCase = new RescueCase("RES-2026-100", LocalDate.of(2026, 8, 18), "Bahía Concha", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        // 3. Animal
        Animal animal = new Animal("AN-2026-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        animal.setTrackingDeviceCode("GPS-TRK-9001");
        rescueCase.assignAnimal(animal);

        // 4. Expediente
        MedicalRecord record = new MedicalRecord(new BigDecimal("27.80"), "STABLE", "Injury caused by fishing net", "Possible plastic ingestion");
        animal.assignMedicalRecord(record);
        animalRepository.save(animal);
        // Verificación directa de la relación 1:1 Animal ↔ MedicalRecord (Paso 51)
        Optional<MedicalRecord> savedRecord = medicalRecordRepository.findById(animal.getMedicalRecord().getId());
        assertThat(savedRecord).isPresent();
        assertThat(savedRecord.get().getInitialCondition()).isEqualTo("STABLE");
        assertThat(savedRecord.get().getInitialWeight()).isEqualByComparingTo(new BigDecimal("27.80"));

        // 5. Especialista y catálogo V2
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehab = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Expertise reptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();

        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehab);
        elena.addExpertise(reptiles);
        specialistRepository.save(elena);

        // 6. Tratamientos
        Treatment t1 = new Treatment(animal, elena, LocalDateTime.of(2026, 8, 18, 10, 0), TreatmentType.WOUND_CARE, "Cleaning of left front flipper");
        Treatment t2 = new Treatment(animal, elena, LocalDateTime.of(2026, 8, 19, 11, 0), TreatmentType.HYDRATION, "Subcutaneous fluid therapy");
        treatmentRepository.save(t1);
        treatmentRepository.save(t2);

        // Consultas de validación (Paso 66)
        Optional<RescueCase> foundCase = rescueCaseRepository.findByCaseCode("RES-2026-100");
        assertThat(foundCase).isPresent();

        List<RescueCase> rehabCases = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);
        assertThat(rehabCases).isNotEmpty();

        List<Animal> carAnimals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");
        assertThat(carAnimals).hasSize(1);
        assertThat(carAnimals.get(0).getAnimalCode()).isEqualTo("AN-2026-100");

        List<Animal> turtles = animalRepository.findByCommonNameContainingIgnoreCase("turtle");
        assertThat(turtles).isNotEmpty();

        List<Specialist> traumaDocs = specialistRepository.findActiveByExpertise("Trauma");
        assertThat(traumaDocs).extracting(Specialist::getFirstName).contains("Elena");

        List<Treatment> treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());
        assertThat(treatments).hasSize(2);
        assertThat(treatments.get(0).getType()).isEqualTo(TreatmentType.WOUND_CARE);
        assertThat(treatments.get(1).getType()).isEqualTo(TreatmentType.HYDRATION);

        List<Treatment> rehabTreatments = treatmentRepository.findTreatmentsBySpecialistExpertise("Rehabilitation");
        assertThat(rehabTreatments).hasSize(2);

        List<Treatment> dateTreatments = treatmentRepository.findTreatmentsBetweenDates(
                LocalDateTime.of(2026, 8, 18, 0, 0),
                LocalDateTime.of(2026, 8, 18, 23, 59)
        );
        assertThat(dateTreatments).hasSize(1);
        assertThat(dateTreatments.get(0).getType()).isEqualTo(TreatmentType.WOUND_CARE);

        List<Animal> challengeAnimals = animalRepository.findAnimalsInStatusTreatedByExpertise(
                RescueStatus.IN_REHABILITATION,
                "Trauma"
        );
        assertThat(challengeAnimals).hasSize(1);
        assertThat(challengeAnimals.get(0).getAnimalCode()).isEqualTo("AN-2026-100");
    }
}
