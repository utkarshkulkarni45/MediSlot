# MediSlot - Healthcare Slot Booking Platform

MediSlot is a Spring Boot 3 REST backend that allows patients to book appointment slots with doctors. It addresses the real-world race condition problem where simultaneous booking requests could result in double-booking.

---

## 🌟 Key Highlights & Engineering Goals

1. **Double-Booking Prevention with Database Locking**:
   - Uses **Pessimistic Locking** (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) inside an atomic `@Transactional` service boundary.
   - Enforces a composite **Database Unique Constraint** on `(doctor_id, slot_start, active_status)` as an inviolable secondary safety net.
   - Verified with a multi-threaded test firing **10 concurrent threads** at the same slot, proving **exactly 1 succeeds and 9 fail**.
2. **Discrete Slot Interval Math**:
   - Slices doctor schedules into clean slots based on consultation minutes (e.g. 15, 30, 45 mins).
   - Drops indivisible remainder periods at schedule boundaries.
   - Strips already-booked slots and past slots for the current date.
   - Validates that booking requests strictly align to the doctor's consultation grid (e.g. `09:10` is rejected for a 30-min interval).
3. **Centralized Exception Handling**:
   - Uniform error JSON shape across all endpoints using `@RestControllerAdvice`:
     ```json
     {
       "timestamp": "2026-10-28T22:50:00",
       "status": 409,
       "error": "Conflict",
       "message": "Slot starting at 2026-11-06T10:00:00 is already booked",
       "path": "/appointments"
     }
     ```
   - Proper HTTP status codes: `400` (Validation / Invalid Slot), `404` (Not Found), `409` (Conflict / Double-booking).

---

## 🛠️ Tech Stack

- **Java 17 / 21+**
- **Spring Boot 3.2.5** (Web, Data JPA, Validation)
- **MySQL 8** (Runtime) & **H2 Database** (Isolated integration tests)
- **Project Lombok** & **Jakarta Bean Validation**
- **JUnit 5, Mockito & AssertJ**

---

## 🏗️ Architecture & Package Structure

```
com.example.medislot
 ├── controller          # HTTP REST API layer (DTO in, DTO out)
 │    ├── AppointmentController.java
 │    ├── DoctorController.java
 │    └── PatientController.java
 ├── service             # Core business rules & transaction boundary
 │    ├── AppointmentService.java
 │    ├── DoctorService.java
 │    ├── PatientService.java
 │    └── SlotGeneratorService.java
 ├── repository          # Spring Data JPA repositories with @Lock queries
 │    ├── AppointmentRepository.java
 │    ├── DoctorRepository.java
 │    ├── DoctorScheduleRepository.java
 │    └── PatientRepository.java
 ├── entity              # JPA database models
 │    ├── Appointment.java
 │    ├── AppointmentStatus.java (BOOKED, CANCELLED)
 │    ├── Doctor.java
 │    ├── DoctorSchedule.java
 │    └── Patient.java
 ├── dto                 # Request & Response POJOs
 ├── exception           # Custom exceptions + GlobalExceptionHandler
 └── MediSlotApplication.java
```

---

## 🚀 Setup & Running Locally

### Prerequisites
- JDK 17 or higher (`java -version`)
- Maven 3.8+ (`mvn -v`)
- MySQL Server (optional, tests run automatically on in-memory H2)

### 1. Clone the repository
```bash
git clone https://github.com/utkarshkulkarni45/MediSlot.git
cd MediSlot
```

### 2. Configure Database
In `src/main/resources/application.yml`, configure your MySQL credentials:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/medislot?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
    username: root
    password: your_password
```

### 3. Run All Tests
```bash
mvn test
```
All 8 unit, integration, and concurrency tests will run on the embedded H2 database.

### 4. Start the Application
```bash
mvn spring-boot:run
```
The server starts on port `8080`.

---

## 📡 API Reference & Sample Requests

### 1. Register a Doctor
`POST /doctors`
```bash
curl -X POST http://localhost:8080/doctors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Dr. Sarah Adams",
    "specialization": "Cardiology",
    "consultationMinutes": 30
  }'
```

### 2. List Doctors
`GET /doctors`
```bash
curl http://localhost:8080/doctors
```

### 3. Add Working Schedule for a Doctor
`POST /doctors/{id}/schedules`
```bash
curl -X POST http://localhost:8080/doctors/1/schedules \
  -H "Content-Type: application/json" \
  -d '{
    "dayOfWeek": "MONDAY",
    "startTime": "09:00",
    "endTime": "13:00"
  }'
```

### 4. Get Available Slots for a Doctor on a Date
`GET /doctors/{id}/slots?date=YYYY-MM-DD`
```bash
curl "http://localhost:8080/doctors/1/slots?date=2026-11-02"
```

### 5. Register a Patient
`POST /patients`
```bash
curl -X POST http://localhost:8080/patients \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Doe",
    "phone": "+1234567890",
    "email": "jane.doe@example.com"
  }'
```

### 6. Book an Appointment
`POST /appointments`
```bash
curl -X POST http://localhost:8080/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "doctorId": 1,
    "patientId": 1,
    "slotStart": "2026-11-02T09:30:00"
  }'
```

### 7. Cancel an Appointment
`PUT /appointments/{id}/cancel`
```bash
curl -X PUT http://localhost:8080/appointments/1/cancel
```

### 8. View Patient Appointment History
`GET /patients/{id}/appointments`
```bash
curl http://localhost:8080/patients/1/appointments
```

---

## 💡 Key Design Decisions & Interview Defense

### 1. Pessimistic vs. Optimistic Locking
- **What is Optimistic Locking?** Rows carry a `@Version` number. When updating, Hibernate executes `UPDATE ... WHERE version = expectedVersion`. If another transaction updated the row first, an `OptimisticLockException` is thrown.
- **What is Pessimistic Locking?** The database issues an exclusive row-level lock (`SELECT ... FOR UPDATE`) before changes are made, preventing concurrent transactions from acquiring or updating the row until the transaction commits.
- **Why Pessimistic Locking was chosen here:** In appointment scheduling, popular slots experience **high contention**. Under optimistic locking, multiple simultaneous requests would all proceed through the booking logic only to collide during commit, forcing repeated application-level retries and wasting CPU/database resources. Pessimistic locking establishes mutual exclusion upfront, allowing conflicting transactions to immediately fail fast with a friendly HTTP 409 Conflict.

### 2. Multi-Layer Concurrency Defense (Database Safety Net)
Even with application-level pessimistic locking, if two requests check for an appointment when **no record exists yet**, there is no existing row to lock. To completely eliminate any chance of a duplicate insert under race conditions, we added a unique constraint:
```java
@Table(
    name = "appointments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_doctor_slot_active", columnNames = {"doctor_id", "slot_start", "active_status"})
    }
)
```
- For active bookings (`status = 'BOOKED'`), `active_status` is `'BOOKED'`.
- When an appointment is cancelled, `active_status` is set to `null`.
- In SQL standards, `NULL` values do not conflict with each other. This allows cancelled historical bookings to stay in the database while allowing the exact same slot to be re-booked!

### 3. Thread-Safe Time Math
All time calculations leverage Java 8+ `java.time` types (`LocalDate`, `LocalTime`, `LocalDateTime`, `Duration`). They are immutable, thread-safe, and free from the concurrency bugs inherent to legacy `java.util.Date` and `java.util.Calendar`.

---

## 🧪 Testing Verification

Run the entire suite via:
```bash
mvn clean test
```

| Test Class | Focus | Assertions |
|---|---|---|
| `SlotGeneratorServiceTest` | Interval slicing & edge cases | Divisibility, past-slot removal, fully-booked day, grid alignment |
| `BookingIntegrationTest` | Service lifecycle | Booking, synchronous double-booking block, cancellation, and rebooking |
| `ConcurrencyBookingTest` | Parallel race conditions | 10 parallel threads hit the same slot via `ExecutorService` -> exactly 1 succeeds, 9 fail |
