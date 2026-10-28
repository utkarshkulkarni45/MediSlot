# problem.md

## Project
Healthcare Slot Booking Platform (Spring Boot REST backend)

## Problem Statement
Clinics lose time and trust when two patients end up booked for the same doctor at the same time. This happens when many people try to book simultaneously and the system checks availability and saves the booking as two separate, unprotected steps (a race condition).

## What This Project Solves
1. **Double-booking**: only one patient can ever hold a given doctor + slot, even under parallel requests.
2. **Slot generation**: turn a doctor's working hours (e.g. 09:00-13:00, 30-min visits) into bookable slots, hiding taken and past ones.
3. **Clean errors**: every failure returns the same JSON shape with the right HTTP status.

## Learning Goals (Why I Built This)
- Understand race conditions and database locking (pessimistic vs optimistic)
- Practice `java.time` (LocalDate, LocalTime) interval math
- Practice `@RestControllerAdvice` centralized exception handling
- Practice layered architecture with DTOs and tests

## Scope Boundaries
In scope: doctors, schedules, patients, booking, cancelling, slot listing.
Out of scope: authentication, payments, notifications, frontend, multi-timezone, microservices.
