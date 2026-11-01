package com.utkarsh.medislot.service;

import com.utkarsh.medislot.dto.DoctorRequest;
import com.utkarsh.medislot.dto.DoctorResponse;
import com.utkarsh.medislot.dto.DoctorScheduleRequest;
import com.utkarsh.medislot.dto.DoctorScheduleResponse;
import com.utkarsh.medislot.entity.Doctor;
import com.utkarsh.medislot.entity.DoctorSchedule;
import com.utkarsh.medislot.exception.InvalidSlotException;
import com.utkarsh.medislot.exception.ResourceNotFoundException;
import com.utkarsh.medislot.repository.DoctorRepository;
import com.utkarsh.medislot.repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Page<DoctorResponse> getAllDoctors(Pageable pageable) {
        return doctorRepository.findAll(pageable).map(this::mapToResponse);
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

