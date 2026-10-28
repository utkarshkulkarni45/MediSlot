# progress.md

## Status Legend
[ ] not started   [~] in progress   [x] done

## Setup
- [x] Spring Boot project created (Web, JPA, MySQL, H2, Lombok, Validation)
- [x] Database config and profiles
- [ ] Git repo initialized, first commit

## Domain
- [x] Entities: Doctor, DoctorSchedule, Patient, Appointment
- [x] Repositories
- [x] DTOs and mappers

## Core Features
- [x] Doctor / schedule / patient CRUD
- [x] SlotGeneratorService
- [x] Booking with pessimistic lock
- [x] Unique constraint safety net
- [x] Cancel appointment
- [x] Patient appointments list

## Error Handling
- [x] Custom exceptions
- [x] GlobalExceptionHandler
- [x] Consistent error JSON

## Testing
- [x] SlotGeneratorService unit tests
- [x] Booking integration test
- [x] Concurrency test (10 threads, 1 winner)

## Finish
- [x] README (setup, APIs, sample requests, design decisions, limitations)
- [x] Postman collection or curl examples
- [x] Final cleanup and push to GitHub

## Notes / Decisions Log
- 2026-10-28: Initialized project with Java 17 / Spring Boot 3.2.5.
- 2026-10-28: Implemented Pessimistic Write Lock (`SELECT ... FOR UPDATE`) in AppointmentRepository.
- 2026-10-28: Added database safety net unique constraint `(doctor_id, slot_start, active_status)` supporting cancellation and rebooking.
- 2026-10-28: Implemented discrete slot interval math with java.time in SlotGeneratorService.
- 2026-10-28: Configured GlobalExceptionHandler with @RestControllerAdvice returning uniform JSON errors.
- 2026-10-28: Created 10-thread parallel concurrency test verifying exactly 1 succeeds and 9 fail with HTTP 409. All 8 tests passing.
