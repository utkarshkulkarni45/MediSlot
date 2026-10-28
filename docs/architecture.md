# architecture.md

## Layers
Controller -> Service -> Repository -> Database
DTOs at the controller boundary; entities stay inside service/repository.

## Package Structure
```
com.example.slotbooking
 ├── controller
 ├── service          (AppointmentService, SlotGeneratorService, ...)
 ├── repository
 ├── entity
 ├── dto
 ├── exception        (custom exceptions + GlobalExceptionHandler)
 └── config
```

## Entities
- **Doctor**(id, name, specialization, consultationMinutes)
- **DoctorSchedule**(id, doctor, dayOfWeek, startTime, endTime)
- **Patient**(id, name, phone, email)
- **Appointment**(id, doctor, patient, slotStart, slotEnd, status, createdAt)

Relationships: Doctor 1-N DoctorSchedule, Doctor 1-N Appointment, Patient 1-N Appointment.

## 1. Concurrency Design
Booking runs inside one `@Transactional` method:
1. Validate slot (inside schedule, aligned, not in past)
2. Query existing BOOKED appointment for doctor + slotStart using `@Lock(PESSIMISTIC_WRITE)`
3. If found -> throw `SlotAlreadyBookedException`
4. Else save appointment

Safety net: unique constraint on (doctor_id, slot_start, active flag) so the DB itself rejects a duplicate even if app logic is bypassed.

Why pessimistic: booking the same slot is a high-conflict case, so blocking is simpler and clearer than retrying on version conflicts.

## 2. Slot Interval Math
`SlotGeneratorService.generate(doctor, date)`:
- Find schedule for `date.getDayOfWeek()`
- Loop: `start = schedule.start; while (start + interval <= schedule.end) { add slot; start += interval }`
- Remove slots with existing BOOKED appointments
- If date is today, remove slots before current time

## 3. Exception Handling
`GlobalExceptionHandler` (`@RestControllerAdvice`):
| Exception | HTTP |
|-----------|------|
| ResourceNotFoundException | 404 |
| SlotAlreadyBookedException | 409 |
| InvalidSlotException | 400 |
| MethodArgumentNotValidException | 400 |
| Exception (fallback) | 500 |

Error body: `{ timestamp, status, error, message, path }`

## Testing
- Unit: SlotGeneratorService
- Integration: booking flow with H2
- Concurrency: ExecutorService with 10 threads on one slot
