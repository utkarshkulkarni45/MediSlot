package com.example.medislot.service;

import com.example.medislot.dto.AppointmentResponse;
import com.example.medislot.dto.BookAppointmentRequest;
import com.example.medislot.entity.Appointment;
import com.example.medislot.entity.AppointmentStatus;
import com.example.medislot.entity.Doctor;
import com.example.medislot.entity.DoctorSchedule;
import com.example.medislot.entity.Patient;
import com.example.medislot.exception.InvalidSlotException;
import com.example.medislot.exception.ResourceNotFoundException;
import com.example.medislot.exception.SlotAlreadyBookedException;
import com.example.medislot.repository.AppointmentRepository;
import com.example.medislot.repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final SlotGeneratorService slotGeneratorService;

    /**
     * Concurrency Control:
     * This method executes in an atomic database transaction (@Transactional).
     * 1. Validates slot is not in the past and aligns to doctor's schedule interval.
     * 2. Issues a PESSIMISTIC_WRITE lock (SELECT ... FOR UPDATE) via findConflictingBookingWithLock().
     *    - If an active booking exists, immediately throws SlotAlreadyBookedException.
     * 3. Persists the new appointment.
     * 4. As an ultimate safety net, the database unique constraint on (doctor_id, slot_start)
     *    prevents race conditions if parallel threads simultaneously pass read checks on empty rows.
     */
    @Transactional
    public AppointmentResponse bookAppointment(BookAppointmentRequest request) {
        LocalDateTime slotStart = request.getSlotStart();
        LocalDateTime now = LocalDateTime.now();

        // 1. Validate slot is not in past
        if (slotStart.isBefore(now)) {
            throw new InvalidSlotException("Cannot book an appointment slot in the past");
        }

        // 2. Fetch doctor and patient
        Doctor doctor = doctorService.getDoctorEntity(request.getDoctorId());
        Patient patient = patientService.getPatientEntity(request.getPatientId());

        // 3. Validate schedule exists for day of week and aligns to interval grid
        DoctorSchedule schedule = doctorScheduleRepository
                .findByDoctorIdAndDayOfWeek(doctor.getId(), slotStart.getDayOfWeek())
                .orElseThrow(() -> new InvalidSlotException("Doctor does not work on " + slotStart.getDayOfWeek()));

        slotGeneratorService.validateSlotAlignment(doctor, schedule, slotStart);

        // 4. Concurrency check: pessimistic write lock on active booking for that slot
        var existingBooking = appointmentRepository.findConflictingBookingWithLock(
                doctor.getId(),
                slotStart,
                AppointmentStatus.BOOKED
        );
        if (existingBooking.isPresent()) {
            throw new SlotAlreadyBookedException("Slot starting at " + slotStart + " is already booked for Dr. " + doctor.getName());
        }

        // 5. Calculate slot end time
        LocalDateTime slotEnd = slotStart.plusMinutes(doctor.getConsultationMinutes());

        // 6. Save appointment
        Appointment appointment = Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .slotStart(slotStart)
                .slotEnd(slotEnd)
                .status(AppointmentStatus.BOOKED)
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        return mapToResponse(saved);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new InvalidSlotException("Appointment is already cancelled");
        }

        // Free up slot so it can be re-booked
        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getPatientAppointments(Long patientId) {
        // Validate patient exists
        patientService.getPatientEntity(patientId);

        return appointmentRepository.findByPatientIdOrderBySlotStartDesc(patientId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AppointmentResponse mapToResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .doctorId(appointment.getDoctor().getId())
                .doctorName(appointment.getDoctor().getName())
                .patientId(appointment.getPatient().getId())
                .patientName(appointment.getPatient().getName())
                .slotStart(appointment.getSlotStart())
                .slotEnd(appointment.getSlotEnd())
                .status(appointment.getStatus())
                .createdAt(appointment.getCreatedAt())
                .build();
    }
}

