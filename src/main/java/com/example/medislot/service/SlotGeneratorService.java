package com.example.medislot.service;

import com.example.medislot.dto.SlotDto;
import com.example.medislot.entity.Appointment;
import com.example.medislot.entity.AppointmentStatus;
import com.example.medislot.entity.Doctor;
import com.example.medislot.entity.DoctorSchedule;
import com.example.medislot.exception.InvalidSlotException;
import com.example.medislot.repository.AppointmentRepository;
import com.example.medislot.repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotGeneratorService {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final AppointmentRepository appointmentRepository;

    /**
     * Generates all available slots for a doctor on a specific date.
     * Takes working hours, chops them into consultation intervals,
     * strips existing active bookings, and strips past slots if date is today.
     */
    public List<SlotDto> generateAvailableSlots(Doctor doctor, LocalDate date, LocalDateTime now) {
        // 1. Fetch doctor's schedule for that day of week
        var scheduleOpt = doctorScheduleRepository.findByDoctorIdAndDayOfWeek(doctor.getId(), date.getDayOfWeek());
        if (scheduleOpt.isEmpty()) {
            // Doctor does not work on this day
            return Collections.emptyList();
        }

        DoctorSchedule schedule = scheduleOpt.get();
        int intervalMinutes = doctor.getConsultationMinutes();
        if (intervalMinutes <= 0) {
            throw new InvalidSlotException("Doctor consultation minutes must be greater than 0");
        }

        // 2. Fetch all booked appointments on this date to filter them out
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        List<Appointment> bookedAppointments = appointmentRepository.findByDoctorIdAndStatusAndSlotStartBetween(
                doctor.getId(),
                AppointmentStatus.BOOKED,
                dayStart,
                dayEnd
        );

        Set<LocalDateTime> bookedStarts = bookedAppointments.stream()
                .map(Appointment::getSlotStart)
                .collect(Collectors.toSet());

        // 3. Generate slots using discrete interval increments
        List<SlotDto> availableSlots = new ArrayList<>();
        LocalTime current = schedule.getStartTime();
        LocalTime scheduleEnd = schedule.getEndTime();

        // Continue generating while slot fits cleanly within working hours
        while (!current.plusMinutes(intervalMinutes).isAfter(scheduleEnd)) {
            LocalTime slotEnd = current.plusMinutes(intervalMinutes);
            LocalDateTime slotStartDateTime = date.atTime(current);
            LocalDateTime slotEndDateTime = date.atTime(slotEnd);

            // Filter out past slots if the target date is today or past
            boolean isPast = slotStartDateTime.isBefore(now);
            // Filter out already booked slots
            boolean isBooked = bookedStarts.contains(slotStartDateTime);

            if (!isPast && !isBooked) {
                availableSlots.add(SlotDto.builder()
                        .start(slotStartDateTime)
                        .end(slotEndDateTime)
                        .build());
            }

            current = slotEnd;
        }

        return availableSlots;
    }

    /**
     * Validates that a requested slot start falls strictly on the doctor's schedule interval grid
     * and is within working hours.
     */
    public void validateSlotAlignment(Doctor doctor, DoctorSchedule schedule, LocalDateTime slotStart) {
        LocalTime requestedTime = slotStart.toLocalTime();
        LocalTime scheduleStart = schedule.getStartTime();
        LocalTime scheduleEnd = schedule.getEndTime();
        int interval = doctor.getConsultationMinutes();

        // 1. Must be within working hours
        if (requestedTime.isBefore(scheduleStart) || requestedTime.plusMinutes(interval).isAfter(scheduleEnd)) {
            throw new InvalidSlotException("Requested slot is outside doctor working hours (" + scheduleStart + " - " + scheduleEnd + ")");
        }

        // 2. Must align exactly with the interval grid from schedule start
        long minutesFromStart = java.time.Duration.between(scheduleStart, requestedTime).toMinutes();
        if (minutesFromStart % interval != 0) {
            throw new InvalidSlotException("Requested slot " + requestedTime + " does not align to the doctor's " + interval + "-minute consultation grid");
        }
    }
}

