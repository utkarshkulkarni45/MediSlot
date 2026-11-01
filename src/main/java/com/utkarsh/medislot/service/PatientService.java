package com.utkarsh.medislot.service;

import com.utkarsh.medislot.dto.PatientRequest;
import com.utkarsh.medislot.dto.PatientResponse;
import com.utkarsh.medislot.entity.Patient;
import com.utkarsh.medislot.exception.PatientEmailAlreadyExistsException;
import com.utkarsh.medislot.exception.ResourceNotFoundException;
import com.utkarsh.medislot.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    @Transactional
    public PatientResponse registerPatient(PatientRequest request) {
        if (patientRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new PatientEmailAlreadyExistsException(
                    "A patient with email " + request.getEmail() + " already exists");
        }

        Patient patient = Patient.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();
        Patient saved = patientRepository.save(patient);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Patient getPatientEntity(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
    }

    private PatientResponse mapToResponse(Patient patient) {
        return PatientResponse.builder()
                .id(patient.getId())
                .name(patient.getName())
                .phone(patient.getPhone())
                .email(patient.getEmail())
                .build();
    }
}

