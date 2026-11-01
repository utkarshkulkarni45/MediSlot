package com.utkarsh.medislot.integration;

import com.utkarsh.medislot.dto.BookAppointmentRequest;
import com.utkarsh.medislot.entity.Appointment;
import com.utkarsh.medislot.entity.AppointmentStatus;
import com.utkarsh.medislot.entity.Doctor;
import com.utkarsh.medislot.entity.DoctorSchedule;
import com.utkarsh.medislot.entity.Patient;
import com.utkarsh.medislot.repository.AppointmentRepository;
import com.utkarsh.medislot.repository.DoctorRepository;
import com.utkarsh.medislot.repository.DoctorScheduleRepository;
import com.utkarsh.medislot.repository.PatientRepository;
import com.utkarsh.medislot.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

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

/**
 * Same race as {@link ConcurrencyBookingTest}, but against a real MySQL 8 InnoDB server.
 * H2 and InnoDB use different locking implementations, so this is the run that shows
 * the behaviour on the production database engine.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MySqlConcurrencyBookingTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
            .withDatabaseName("medislot")
            .withUsername("medislot")
            .withPassword("medislot")
            .withUrlParam("allowPublicKeyRetrieval", "true")
            .withUrlParam("useSSL", "false");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "20");
    }

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

        doctor = doctorRepository.save(Doctor.builder()
                .name("Dr. House")
                .specialization("Diagnostics")
                .consultationMinutes(30)
                .build());

        doctorScheduleRepository.save(DoctorSchedule.builder()
                .doctor(doctor)
                .dayOfWeek(DayOfWeek.FRIDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .build());

        for (int i = 1; i <= 10; i++) {
            Patient p = patientRepository.save(Patient.builder()
                    .name("Patient " + i)
                    .email("patient" + i + "@example.com")
                    .phone("999000000" + i)
                    .build());
            patients.add(p);
        }

        LocalDate nextFriday = LocalDate.now().plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        targetSlot = nextFriday.atTime(10, 0);
    }

    @Test
    @DisplayName("MySQL: 10 parallel threads race for one slot -> exactly 1 booking commits")
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
                    startLatch.await();

                    appointmentService.bookAppointment(BookAppointmentRequest.builder()
                            .doctorId(doctor.getId())
                            .patientId(patient.getId())
                            .slotStart(targetSlot)
                            .build());
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    // SlotAlreadyBookedException, a lock/deadlock error, or the unique-constraint safety net
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        finishLatch.await();
        executorService.shutdown();

        assertEquals(1, successCount.get(), "Exactly 1 booking request must succeed");
        assertEquals(9, failureCount.get(), "Exactly 9 booking requests must be rejected");

        List<Appointment> booked = appointmentRepository.findByDoctorIdAndStatusAndSlotStartBetween(
                doctor.getId(),
                AppointmentStatus.BOOKED,
                targetSlot.minusMinutes(1),
                targetSlot.plusMinutes(1)
        );
        assertEquals(1, booked.size(), "Exactly one active row may exist for the slot");
    }
}
