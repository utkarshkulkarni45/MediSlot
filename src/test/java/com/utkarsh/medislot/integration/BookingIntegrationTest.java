package com.utkarsh.medislot.integration;

import com.utkarsh.medislot.dto.AppointmentResponse;
import com.utkarsh.medislot.dto.BookAppointmentRequest;
import com.utkarsh.medislot.entity.Appointment;
import com.utkarsh.medislot.entity.AppointmentStatus;
import com.utkarsh.medislot.entity.Doctor;
import com.utkarsh.medislot.entity.DoctorSchedule;
import com.utkarsh.medislot.entity.Patient;
import com.utkarsh.medislot.exception.InvalidSlotException;
import com.utkarsh.medislot.exception.SlotAlreadyBookedException;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class BookingIntegrationTest {

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
    private Patient patient;
    private LocalDateTime futureSlot;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        doctorScheduleRepository.deleteAll();
        doctorRepository.deleteAll();
        patientRepository.deleteAll();

        // 1. Create Doctor
        doctor = doctorRepository.save(Doctor.builder()
                .name("Dr. Alice")
                .specialization("Pediatrics")
                .consultationMinutes(30)
                .build());

        // 2. Create Schedule: Every Wednesday 09:00 - 13:00
        doctorScheduleRepository.save(DoctorSchedule.builder()
                .doctor(doctor)
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(13, 0))
                .build());

        // 3. Create Patient
        patient = patientRepository.save(Patient.builder()
                .name("John Doe")
                .email("john@example.com")
                .phone("1234567890")
                .build());

        // 4. Calculate next Wednesday at 10:00 AM (guaranteed to be in the future)
        LocalDate nextWednesday = LocalDate.now().plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
        futureSlot = nextWednesday.atTime(10, 0);
    }

    @Test
    @DisplayName("Booking Flow: Successfully book and then cancel an appointment")
    void testBookAndCancelFlow() {
        BookAppointmentRequest request = BookAppointmentRequest.builder()
                .doctorId(doctor.getId())
                .patientId(patient.getId())
                .slotStart(futureSlot)
                .build();

        AppointmentResponse response = appointmentService.bookAppointment(request);
        assertNotNull(response.getId());
        assertEquals(AppointmentStatus.BOOKED, response.getStatus());

        // Attempting to book the same slot again synchronously throws SlotAlreadyBookedException
        assertThrows(SlotAlreadyBookedException.class, () -> appointmentService.bookAppointment(request));

        // Cancel the appointment
        AppointmentResponse cancelResponse = appointmentService.cancelAppointment(response.getId());
        assertEquals(AppointmentStatus.CANCELLED, cancelResponse.getStatus());

        // After cancellation, the slot becomes available again and can be rebooked
        AppointmentResponse rebooked = appointmentService.bookAppointment(request);
        assertEquals(AppointmentStatus.BOOKED, rebooked.getStatus());
    }

    @Test
    @DisplayName("Validation: Booking a past slot is rejected")
    void testBookingPastSlotThrowsException() {
        BookAppointmentRequest request = BookAppointmentRequest.builder()
                .doctorId(doctor.getId())
                .patientId(patient.getId())
                .slotStart(LocalDateTime.now().minusDays(1).withHour(10).withMinute(0))
                .build();

        assertThrows(InvalidSlotException.class, () -> appointmentService.bookAppointment(request));
    }
}

