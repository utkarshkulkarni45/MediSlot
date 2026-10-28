package com.example.medislot.controller;

import com.example.medislot.dto.AppointmentResponse;
import com.example.medislot.dto.PatientRequest;
import com.example.medislot.dto.PatientResponse;
import com.example.medislot.service.AppointmentService;
import com.example.medislot.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<PatientResponse> registerPatient(@Valid @RequestBody PatientRequest request) {
        PatientResponse response = patientService.registerPatient(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/appointments")
    public ResponseEntity<List<AppointmentResponse>> getPatientAppointments(@PathVariable Long id) {
        List<AppointmentResponse> responses = appointmentService.getPatientAppointments(id);
        return ResponseEntity.ok(responses);
    }
}

