# DeepBlue Rescue

Capa de persistencia de **DeepBlue Rescue**, una plataforma para organizaciones dedicadas al rescate y rehabilitación de fauna marina.

Proyecto desarrollado como laboratorio práctico de persistencia con **Java 21**, **Spring Boot 4**, **Spring Data JPA**, **Hibernate**, **Flyway**, **PostgreSQL** y **Testcontainers**.

## Descripción

Cuando un animal es encontrado herido o en peligro, un centro de rescate registra un caso, se identifica al animal involucrado, se abre su expediente médico y distintos especialistas participan en su recuperación mediante tratamientos. Este proyecto implementa exclusivamente la **capa de persistencia** de ese dominio: modelo relacional, migraciones, entidades JPA, repositories y pruebas de integración contra una base de datos PostgreSQL real. No incluye Controller, REST API, Service, DTO, seguridad ni frontend.

## Modelo de datos

### Tablas

| Tabla | Tipo | Descripción |
|---|---|---|
| `rescue_centers` | Entidad | Centros de rescate |
| `rescue_cases` | Entidad | Casos de rescate |
| `animals` | Entidad | Animales rescatados |
| `medical_records` | Entidad | Expedientes médicos |
| `specialists` | Entidad | Especialistas veterinarios |
| `expertise` | Entidad | Catálogo de áreas de experiencia |
| `specialist_expertise` | Asociativa | Relación N:M entre especialistas y experiencias |
| `treatments` | Entidad | Tratamientos realizados a los animales |

### Restricciones principales

- `rescue_centers.code` → `UNIQUE`
- `rescue_cases.case_code` → `UNIQUE`; `rescue_cases.status` → `CHECK` (solo admite `ADMITTED`, `UNDER_EVALUATION`, `IN_REHABILITATION`, `READY_FOR_RELEASE`, `RELEASED`, `CLOSED`)
- `animals.animal_code` → `UNIQUE`; `animals.rescue_case_id` → `UNIQUE` (garantiza 1:1 con `rescue_cases`); `animals.tracking_device_code` → `UNIQUE`, nullable (agregado en V3)
- `medical_records.animal_id` → `UNIQUE` (garantiza 1:1 con `animals`)
- `specialists.professional_code` y `specialists.email` → `UNIQUE`
- `expertise.name` → `UNIQUE`
- `specialist_expertise` → clave primaria compuesta `(specialist_id, expertise_id)`
- Índices sobre `rescue_cases.rescue_center_id`, `rescue_cases.status`, `rescue_cases.rescue_date`, `treatments.animal_id`, `treatments.specialist_id`, `treatments.performed_at`

## Relaciones

```
RescueCenter
     |
     | 1:N
     v
RescueCase
     |
     | 1:1
     v
Animal
     |
     +──────── 1:1 ──────── MedicalRecord
     |
     +──────── 1:N ──────── Treatment
                                |
                                | N:1
                                v
                           Specialist
                                |
                                | N:M
                                v
                           Expertise
```

- **RescueCenter 1:N RescueCase** — FK `rescue_center_id` en `rescue_cases`.
- **RescueCase 1:1 Animal** — FK `rescue_case_id` en `animals` (con `UNIQUE`). `Animal` es el propietario de la relación (tiene el `@JoinColumn`); `RescueCase` usa `mappedBy`.
- **Animal 1:1 MedicalRecord** — FK `animal_id` en `medical_records` (con `UNIQUE`). `MedicalRecord` es el propietario; `Animal` usa `mappedBy`.
- **Animal 1:N Treatment** — FK `animal_id` en `treatments`.
- **Specialist 1:N Treatment** — FK `specialist_id` en `treatments`.
- **Specialist N:M Expertise** — tabla intermedia `specialist_expertise`.

## Requisitos previos

- Java 21
- Maven (o el wrapper `mvnw` / `mvnw.cmd` incluido en el proyecto)
- Docker Desktop en ejecución (requerido por Testcontainers para las pruebas)
- Una instancia de PostgreSQL disponible si se desea ejecutar la aplicación fuera de las pruebas (no requerida solo para `mvn test`)

## Cómo ejecutar

Clonar el repositorio y, desde la raíz del proyecto:

```bash
./mvnw clean install
```

En Windows:

```powershell
.\mvnw.cmd clean install
```

La configuración de conexión a base de datos (`application.yml`) usa variables de entorno con valores por defecto:

```yaml
DB_URL      -> jdbc:postgresql://localhost:5432/deepblue
DB_USER     -> postgres
DB_PASSWORD -> postgres
```

## Cómo ejecutar las pruebas

```bash
./mvnw clean test
```

En Windows:

```powershell
.\mvnw.cmd clean test
```

No se necesita una base de datos levantada manualmente: **Testcontainers** crea automáticamente un contenedor Docker con PostgreSQL antes de las pruebas y lo destruye al finalizar. Durante la ejecución se puede confirmar en otra terminal con `docker ps` que existe temporalmente un contenedor `postgres:18-alpine`.

## Flyway

El esquema de la base de datos es responsabilidad exclusiva de **Flyway**, no de Hibernate. Se usa `ddl-auto: validate`, lo que significa que Hibernate únicamente **compara** el esquema esperado por las entidades contra el esquema real creado por Flyway; si no coinciden, la aplicación falla al arrancar en lugar de modificar la base de datos automáticamente.

Migraciones incluidas:

| Migración | Descripción |
|---|---|
| `V1__create_schema.sql` | Crea todas las tablas del dominio, sus PK, FK, UNIQUE, CHECK e índices |
| `V2__insert_expertise_catalog.sql` | Inserta el catálogo inicial de áreas de experiencia (Marine Reptiles, Marine Mammals, Marine Birds, Trauma, Rehabilitation, Toxicology) |
| `V3__add_tracking_device_to_animal.sql` | Agrega la columna `tracking_device_code` (nullable, `UNIQUE`) a `animals` para soportar dispositivos GPS |

El historial de migraciones aplicadas queda registrado en la tabla `flyway_schema_history`, la cual es verificada explícitamente en las pruebas de integración mediante una consulta directa con `JdbcTemplate`.

## Testcontainers

Las pruebas de integración se ejecutan contra una instancia **real de PostgreSQL** (no H2), levantada dinámicamente mediante Testcontainers:

```java
@Container
@ServiceConnection
static final PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:18-alpine");
```

Esto es importante porque permite validar comportamiento que una base en memoria como H2 no reproduce fielmente: constraints `UNIQUE`, `CHECK`, tipos de datos específicos de PostgreSQL y el comportamiento real de Flyway. La anotación `@ServiceConnection` conecta automáticamente el contenedor con el `DataSource` de Spring, sin configuración manual adicional.

## Query Methods implementados

| Repository | Método | Propósito |
|---|---|---|
| `RescueCenterRepository` | `findByCode(String code)` | Buscar un centro por su código |
| `RescueCaseRepository` | `findByCaseCode(String caseCode)` | Buscar un caso por su código |
| `RescueCaseRepository` | `findByStatusOrderByRescueDateAsc(RescueStatus status)` | Casos según estado, ordenados por fecha de rescate ascendente |
| `RescueCaseRepository` | `findByRescueCenterCode(String code)` | Casos pertenecientes a un centro determinado |
| `AnimalRepository` | `findByAnimalCode(String animalCode)` | Buscar animal por su código |
| `AnimalRepository` | `findByCommonNameContainingIgnoreCase(String text)` | Animales cuyo nombre común contiene cierto texto, sin distinguir mayúsculas |
| `AnimalRepository` | `findByRescueCaseRescueCenterCode(String centerCode)` | Animales pertenecientes a un centro determinado (navega Animal → RescueCase → RescueCenter) |
| `ExpertiseRepository` | `findByNameIgnoreCase(String name)` | Buscar una experiencia por nombre, sin distinguir mayúsculas |
| `TreatmentRepository` | `findByAnimalIdOrderByPerformedAtAsc(Long animalId)` | Tratamientos de un animal ordenados cronológicamente |

## Consultas JPQL implementadas

| Repository | Método | Propósito |
|---|---|---|
| `SpecialistRepository` | `findActiveByExpertise(String expertiseName)` | Especialistas activos con determinada experiencia (`JOIN` + `LOWER` + parámetro nombrado + `active = true`) |
| `TreatmentRepository` | `findTreatmentsBetweenDates(LocalDateTime start, LocalDateTime end)` | Tratamientos realizados entre dos fechas |
| `TreatmentRepository` | `findTreatmentsBySpecialistExpertise(String expertiseName)` | Tratamientos realizados por especialistas con determinada experiencia (navega Treatment → Specialist → Expertise, con `DISTINCT`) |
| `AnimalRepository` | `findAnimalsInStatusTreatedByExpertise(RescueStatus status, String expertiseName)` | Animales en determinado estado que recibieron al menos un tratamiento de un especialista con cierta experiencia (navega dos caminos: Animal → RescueCase → status, y Animal → treatments → specialist → expertiseAreas; usa `DISTINCT` para evitar duplicados) |

## Pruebas de integración incluidas

La clase `PersistenceIntegrationTest` agrupa la validación en 3 pruebas:

1. **`testFlywayMigrationsApplied`** — confirma, consultando `flyway_schema_history` vía `JdbcTemplate`, que las migraciones V1, V2 y V3 fueron aplicadas exitosamente por Flyway (y no por Hibernate).
2. **`testUniqueConstraintViolation`** — crea un animal duplicado (`animal_code` repetido) y verifica, usando `saveAndFlush()`, que PostgreSQL lanza `DataIntegrityViolationException` por violar el constraint `UNIQUE`.
3. **`testIntegratorScenario`** — cubre el reto integrador completo:
   - Persistencia encadenada: `RescueCenter` → `RescueCase` (1:N) → `Animal` (1:1) → `MedicalRecord` (1:1, con verificación explícita vía `medicalRecordRepository`).
   - Relación N:M entre `Specialist` y `Expertise` (Elena Vargas con 3 áreas de experiencia).
   - Registro de `Treatment` con relaciones N:1 hacia `Animal` y `Specialist`.
   - Las 8 consultas del Paso 66 (existencia por código, casos por estado, animales por centro, búsqueda por texto, especialistas por expertise, tratamientos por animal, tratamientos por expertise del especialista, tratamientos por rango de fechas).
   - La consulta del reto sin guía (Parte XIII): animales en determinado estado tratados por especialistas con cierta experiencia, usando JPQL con `DISTINCT`.

## Estructura del proyecto

```
deepblue-rescue/
│
├── pom.xml
├── README.md
│
├── src/main/java/com/deepblue/rescue
│   ├── DeepblueRescueApplication.java
│   ├── domain
│   │   ├── RescueCenter.java
│   │   ├── RescueCase.java
│   │   ├── RescueStatus.java
│   │   ├── Animal.java
│   │   ├── AnimalSex.java
│   │   ├── MedicalRecord.java
│   │   ├── Specialist.java
│   │   ├── Expertise.java
│   │   ├── Treatment.java
│   │   └── TreatmentType.java
│   └── repository
│       ├── RescueCenterRepository.java
│       ├── RescueCaseRepository.java
│       ├── AnimalRepository.java
│       ├── MedicalRecordRepository.java
│       ├── SpecialistRepository.java
│       ├── ExpertiseRepository.java
│       └── TreatmentRepository.java
│
├── src/main/resources
│   ├── application.yml
│   └── db/migration
│       ├── V1__create_schema.sql
│       ├── V2__insert_expertise_catalog.sql
│       └── V3__add_tracking_device_to_animal.sql
│
└── src/test/java/com/deepblue/rescue
    └── PersistenceIntegrationTest.java
```

## Resultado de las pruebas

```
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
