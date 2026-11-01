package com.utkarsh.medislot.controller;

import com.utkarsh.medislot.dto.DoctorRequest;
import com.utkarsh.medislot.dto.DoctorResponse;
import com.utkarsh.medislot.dto.DoctorScheduleRequest;
import com.utkarsh.medislot.dto.DoctorScheduleResponse;
import com.utkarsh.medislot.dto.SlotDto;
import com.utkarsh.medislot.entity.Doctor;
import com.utkarsh.medislot.service.DoctorService;
import com.utkarsh.medislot.service.SlotGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<Page<DoctorResponse>> getAllDoctors(
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(doctorService.getAllDoctors(pageable));
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
        List<SlotDto> slots = slotGeneratorService.generateAvailableSlots(doctor, date);
        return ResponseEntity.ok(slots);
    }
}

