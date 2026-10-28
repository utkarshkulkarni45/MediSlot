package com.example.medislot.integration;

import com.example.medislot.dto.BookAppointmentRequest;
import com.example.medislot.entity.Appointment;
import com.example.medislot.entity.AppointmentStatus;
import com.example.medislot.entity.Doctor;
import com.example.medislot.entity.DoctorSchedule;
import com.example.medislot.entity.Patient;
import com.example.medislot.repository.AppointmentRepository;
import com.example.medislot.repository.DoctorRepository;
import com.example.medislot.repository.DoctorScheduleRepository;
import com.example.medislot.repository.PatientRepository;
import com.example.medislot.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyBookingTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    private Doctor doctor;
    private final List<Patient> patients = new ArrayList<>();
    private LocalDateTime targetSlot;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        doctorScheduleRepository.deleteAll();
        doctorRepository.deleteAll();
        patientRepository.deleteAll();
        patients.clear();

        // 1. Create Doctor
        doctor = doctorRepository.save(Doctor.builder()
                .name("Dr. House")
                .specialization("Diagnostics")
                .consultationMinutes(30)
                .build());

        // 2. Doctor works on Fridays
        doctorScheduleRepository.save(DoctorSchedule.builder()
                .doctor(doctor)
                .dayOfWeek(DayOfWeek.FRIDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .build());

        // 3. Create 10 distinct patients
        for (int i = 1; i <= 10; i++) {
            Patient p = patientRepository.save(Patient.builder()
                    .name("Patient " + i)
                    .email("patient" + i + "@example.com")
                    .phone("999000000" + i)
                    .build());
            patients.add(p);
        }

        // 4. Target slot: Next Friday at 10:00 AM
        LocalDate nextFriday = LocalDate.now().plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        targetSlot = nextFriday.atTime(10, 0);
    }

    @Test
    @DisplayName("Concurrency Control: 10 parallel threads race for the exact same slot -> Exactly 1 succeeds, 9 fail")
    void testConcurrentBooking_OnlyOneSucceeds() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final Patient patient = patients.get(i);
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    // Wait for the gun: ensure all 10 threads hit the database simultaneously
                    startLatch.await();

                    BookAppointmentRequest request = BookAppointmentRequest.builder()
                            .doctorId(doctor.getId())
                            .patientId(patient.getId())
                            .slotStart(targetSlot)
                            .build();

                    appointmentService.bookAppointment(request);
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    // Expected: SlotAlreadyBookedException or DataIntegrityViolationException
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Wait until all threads are spun up and waiting
        readyLatch.await();
        // Fire all threads simultaneously
        startLatch.countDown();
        // Wait for all 10 threads to finish execution
        finishLatch.await();
        executorService.shutdown();

        // Assert exactly ONE patient acquired the slot
        assertEquals(1, successCount.get(), "Exactly 1 booking request must succeed");
        assertEquals(9, failureCount.get(), "Exactly 9 booking requests must be rejected with conflict");

        // Verify the database state: exactly 1 BOOKED appointment exists for that doctor & slot
        List<Appointment> bookedList = appointmentRepository.findByDoctorIdAndStatusAndSlotStartBetween(
                doctor.getId(),
                AppointmentStatus.BOOKED,
                targetSlot.minusMinutes(1),
                targetSlot.plusMinutes(1)
        );
        assertEquals(1, bookedList.size());
    }
}

