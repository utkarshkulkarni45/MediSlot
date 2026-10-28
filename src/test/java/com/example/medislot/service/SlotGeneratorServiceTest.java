package com.example.medislot.service;

import com.example.medislot.dto.SlotDto;
import com.example.medislot.entity.Appointment;
import com.example.medislot.entity.AppointmentStatus;
import com.example.medislot.entity.Doctor;
import com.example.medislot.entity.DoctorSchedule;
import com.example.medislot.exception.InvalidSlotException;
import com.example.medislot.repository.AppointmentRepository;
import com.example.medislot.repository.DoctorScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotGeneratorServiceTest {

    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private SlotGeneratorService slotGeneratorService;

    private Doctor doctor;
    private DoctorSchedule schedule;
    private LocalDate targetDate;

    @BeforeEach
    void setUp() {
        doctor = Doctor.builder()
                .id(1L)
                .name("Dr. Smith")
                .specialization("Cardiology")
                .consultationMinutes(30)
                .build();

        // 2026-11-02 is a Monday
        targetDate = LocalDate.of(2026, 11, 2);

        schedule = DoctorSchedule.builder()
                .id(1L)
                .doctor(doctor)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(13, 0)) // 4 hours -> 8 slots of 30 mins
                .build();
    }

    @Test
    @DisplayName("Normal Case: 09:00 - 13:00 with 30-min intervals yields exactly 8 slots")
    void testGenerateSlots_NormalCase() {
        when(doctorScheduleRepository.findByDoctorIdAndDayOfWeek(doctor.getId(), DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.findByDoctorIdAndStatusAndSlotStartBetween(eq(doctor.getId()), eq(AppointmentStatus.BOOKED), any(), any()))
                .thenReturn(Collections.emptyList());

        // Simulation point: early morning before 09:00
        LocalDateTime currentTime = LocalDateTime.of(2026, 11, 2, 8, 0);

        List<SlotDto> slots = slotGeneratorService.generateAvailableSlots(doctor, targetDate, currentTime);

        assertEquals(8, slots.size());
        assertEquals(LocalDateTime.of(2026, 11, 2, 9, 0), slots.get(0).getStart());
        assertEquals(LocalDateTime.of(2026, 11, 2, 9, 30), slots.get(0).getEnd());
        assertEquals(LocalDateTime.of(2026, 11, 2, 12, 30), slots.get(7).getStart());
        assertEquals(LocalDateTime.of(2026, 11, 2, 13, 0), slots.get(7).getEnd());
    }

    @Test
    @DisplayName("Schedule Indivisible: 09:00 - 10:20 with 30-min intervals drops trailing partial 20 mins (2 slots)")
    void testGenerateSlots_ScheduleNotDivisibleByInterval() {
        schedule.setEndTime(LocalTime.of(10, 20)); // 80 minutes: 09:00-09:30, 09:30-10:00 (10:00-10:30 exceeds 10:20)
        when(doctorScheduleRepository.findByDoctorIdAndDayOfWeek(doctor.getId(), DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.findByDoctorIdAndStatusAndSlotStartBetween(eq(doctor.getId()), eq(AppointmentStatus.BOOKED), any(), any()))
                .thenReturn(Collections.emptyList());

        LocalDateTime currentTime = LocalDateTime.of(2026, 11, 2, 8, 0);

        List<SlotDto> slots = slotGeneratorService.generateAvailableSlots(doctor, targetDate, currentTime);

        assertEquals(2, slots.size());
        assertEquals(LocalDateTime.of(2026, 11, 2, 9, 0), slots.get(0).getStart());
        assertEquals(LocalDateTime.of(2026, 11, 2, 9, 30), slots.get(1).getStart());
    }

    @Test
    @DisplayName("Past Slots: If current time is 10:15, slots at 09:00, 09:30, 10:00 are removed")
    void testGenerateSlots_FiltersPastSlots() {
        when(doctorScheduleRepository.findByDoctorIdAndDayOfWeek(doctor.getId(), DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.findByDoctorIdAndStatusAndSlotStartBetween(eq(doctor.getId()), eq(AppointmentStatus.BOOKED), any(), any()))
                .thenReturn(Collections.emptyList());

        // Simulated current time: 10:15 AM
        LocalDateTime currentTime = LocalDateTime.of(2026, 11, 2, 10, 15);

        List<SlotDto> slots = slotGeneratorService.generateAvailableSlots(doctor, targetDate, currentTime);

        // 8 total slots minus 3 past slots (09:00, 09:30, 10:00) = 5 remaining
        assertEquals(5, slots.size());
        assertEquals(LocalDateTime.of(2026, 11, 2, 10, 30), slots.get(0).getStart());
    }

    @Test
    @DisplayName("Fully Booked Day: When all slots are already booked, returns empty list")
    void testGenerateSlots_FullyBookedDay() {
        when(doctorScheduleRepository.findByDoctorIdAndDayOfWeek(doctor.getId(), DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule));

        // Create 8 booked appointments for all slots
        List<Appointment> allBooked = List.of(
                Appointment.builder().slotStart(LocalDateTime.of(2026, 11, 2, 9, 0)).build(),
                Appointment.builder().slotStart(LocalDateTime.of(2026, 11, 2, 9, 30)).build(),
                Appointment.builder().slotStart(LocalDateTime.of(2026, 11, 2, 10, 0)).build(),
                Appointment.builder().slotStart(LocalDateTime.of(2026, 11, 2, 10, 30)).build(),
                Appointment.builder().slotStart(LocalDateTime.of(2026, 11, 2, 11, 0)).build(),
                Appointment.builder().slotStart(LocalDateTime.of(2026, 11, 2, 11, 30)).build(),
                Appointment.builder().slotStart(LocalDateTime.of(2026, 11, 2, 12, 0)).build(),
                Appointment.builder().slotStart(LocalDateTime.of(2026, 11, 2, 12, 30)).build()
        );

        when(appointmentRepository.findByDoctorIdAndStatusAndSlotStartBetween(eq(doctor.getId()), eq(AppointmentStatus.BOOKED), any(), any()))
                .thenReturn(allBooked);

        LocalDateTime currentTime = LocalDateTime.of(2026, 11, 2, 8, 0);

        List<SlotDto> slots = slotGeneratorService.generateAvailableSlots(doctor, targetDate, currentTime);

        assertTrue(slots.isEmpty());
    }

    @Test
    @DisplayName("Alignment Check: Valid slot passes, misaligned slot (09:10) throws InvalidSlotException")
    void testValidateSlotAlignment() {
        // 09:00 aligns with 30-min intervals
        slotGeneratorService.validateSlotAlignment(doctor, schedule, LocalDateTime.of(2026, 11, 2, 9, 0));

        // 09:10 does not align -> throws InvalidSlotException
        assertThrows(InvalidSlotException.class, () ->
                slotGeneratorService.validateSlotAlignment(doctor, schedule, LocalDateTime.of(2026, 11, 2, 9, 10))
        );

        // Outside working hours (08:30) -> throws InvalidSlotException
        assertThrows(InvalidSlotException.class, () ->
                slotGeneratorService.validateSlotAlignment(doctor, schedule, LocalDateTime.of(2026, 11, 2, 8, 30))
        );
    }
}

