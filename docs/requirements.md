# requirements.md

## Tech Stack
Java 17, Spring Boot 3, Spring Data JPA, MySQL (H2 for tests), Maven, Lombok, Bean Validation, JUnit 5.

## Functional Requirements
| ID | Requirement |
|----|-------------|
| FR1 | Create and list doctors (name, specialization, consultation minutes) |
| FR2 | Add weekly schedule for a doctor (dayOfWeek, startTime, endTime) |
| FR3 | Register patients (name, phone, email) |
| FR4 | `GET /doctors/{id}/slots?date=` returns available slots for that date |
| FR5 | `POST /appointments` books a slot for a patient |
| FR6 | `PUT /appointments/{id}/cancel` cancels and frees the slot |
| FR7 | `GET /patients/{id}/appointments` lists a patient's appointments |

## Business Rules
- A doctor + slotStart can have at most ONE active (BOOKED) appointment.
- Slot must fall inside the doctor's schedule for that day.
- Slot start must align to the consultation interval (09:10 invalid for 30-min).
- Past slots cannot be booked.
- A cancelled slot becomes bookable again.

## Non-Functional Requirements
- Correct under concurrent requests (verified by a test)
- Consistent JSON error responses
- DTOs only in API (no entities exposed)
- Readable code, README with setup and sample requests

## Acceptance Criteria
- [ ] 10 parallel bookings for the same slot -> exactly 1 succeeds, 9 get HTTP 409
- [ ] Slot generator unit tests pass (normal, indivisible schedule, past slots, fully booked)
- [ ] Invalid input returns 400, missing resource 404, conflict 409, all in one JSON format
- [ ] App runs from README steps on a fresh machine
