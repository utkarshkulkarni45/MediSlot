package com.example.medislot.controller;

import com.example.medislot.dto.DoctorRequest;
import com.example.medislot.dto.DoctorResponse;
import com.example.medislot.dto.DoctorScheduleRequest;
import com.example.medislot.dto.DoctorScheduleResponse;
import com.example.medislot.dto.SlotDto;
import com.example.medislot.entity.Doctor;
import com.example.medislot.service.DoctorService;
import com.example.medislot.service.SlotGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final SlotGeneratorService slotGeneratorService;

    @PostMapping
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody DoctorRequest request) {
        DoctorResponse response = doctorService.createDoctor(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }

    @PostMapping("/{id}/schedules")
    public ResponseEntity<DoctorScheduleResponse> addSchedule(
            @PathVariable Long id,
            @Valid @RequestBody DoctorScheduleRequest request
    ) {
        DoctorScheduleResponse response = doctorService.addSchedule(id, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<List<SlotDto>> getAvailableSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Doctor doctor = doctorService.getDoctorEntity(id);
        List<SlotDto> slots = slotGeneratorService.generateAvailableSlots(doctor, date, LocalDateTime.now());
        return ResponseEntity.ok(slots);
    }
}

