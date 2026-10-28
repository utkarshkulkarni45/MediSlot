package com.example.medislot.service;

import com.example.medislot.dto.DoctorRequest;
import com.example.medislot.dto.DoctorResponse;
import com.example.medislot.dto.DoctorScheduleRequest;
import com.example.medislot.dto.DoctorScheduleResponse;
import com.example.medislot.entity.Doctor;
import com.example.medislot.entity.DoctorSchedule;
import com.example.medislot.exception.InvalidSlotException;
import com.example.medislot.exception.ResourceNotFoundException;
import com.example.medislot.repository.DoctorRepository;
import com.example.medislot.repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;

    @Transactional
    public DoctorResponse createDoctor(DoctorRequest request) {
        Doctor doctor = Doctor.builder()
                .name(request.getName())
                .specialization(request.getSpecialization())
                .consultationMinutes(request.getConsultationMinutes())
                .build();
        Doctor saved = doctorRepository.save(doctor);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Doctor getDoctorEntity(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));
    }

    @Transactional
    public DoctorScheduleResponse addSchedule(Long doctorId, DoctorScheduleRequest request) {
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new InvalidSlotException("Schedule start time must be before end time");
        }

        Doctor doctor = getDoctorEntity(doctorId);

        DoctorSchedule schedule = doctorScheduleRepository.findByDoctorIdAndDayOfWeek(doctorId, request.getDayOfWeek())
                .map(existing -> {
                    // Update existing schedule for that day
                    existing.setStartTime(request.getStartTime());
                    existing.setEndTime(request.getEndTime());
                    return existing;
                })
                .orElseGet(() -> DoctorSchedule.builder()
                        .doctor(doctor)
                        .dayOfWeek(request.getDayOfWeek())
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .build());

        DoctorSchedule saved = doctorScheduleRepository.save(schedule);
        return mapToScheduleResponse(saved);
    }

    private DoctorResponse mapToResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .name(doctor.getName())
                .specialization(doctor.getSpecialization())
                .consultationMinutes(doctor.getConsultationMinutes())
                .build();
    }

    private DoctorScheduleResponse mapToScheduleResponse(DoctorSchedule schedule) {
        return DoctorScheduleResponse.builder()
                .id(schedule.getId())
                .doctorId(schedule.getDoctor().getId())
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .build();
    }
}

