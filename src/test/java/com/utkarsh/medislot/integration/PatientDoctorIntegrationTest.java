package com.utkarsh.medislot.integration;

import com.utkarsh.medislot.dto.DoctorRequest;
import com.utkarsh.medislot.dto.DoctorResponse;
import com.utkarsh.medislot.dto.PatientRequest;
import com.utkarsh.medislot.exception.PatientEmailAlreadyExistsException;
import com.utkarsh.medislot.repository.AppointmentRepository;
import com.utkarsh.medislot.repository.DoctorRepository;
import com.utkarsh.medislot.repository.DoctorScheduleRepository;
import com.utkarsh.medislot.repository.PatientRepository;
import com.utkarsh.medislot.service.DoctorService;
import com.utkarsh.medislot.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class PatientDoctorIntegrationTest {

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        doctorScheduleRepository.deleteAll();
        doctorRepository.deleteAll();
        patientRepository.deleteAll();
    }

    @Test
    @DisplayName("Patient email is unique")
    void duplicatePatientEmailIsRejected() {
        PatientRequest request = PatientRequest.builder()
                .name("Jane Doe")
                .phone("555-0100")
                .email("jane@example.com")
                .build();

        patientService.registerPatient(request);

        assertThrows(PatientEmailAlreadyExistsException.class, () -> patientService.registerPatient(request));
    }

    @Test
    @DisplayName("GET /doctors is paginated")
    void doctorsAreReturnedAsPages() {
        for (int i = 1; i <= 3; i++) {
            doctorService.createDoctor(DoctorRequest.builder()
                    .name("Dr. " + i)
                    .specialization("General Medicine")
                    .consultationMinutes(30)
                    .build());
        }

        Page<DoctorResponse> firstPage = doctorService.getAllDoctors(PageRequest.of(0, 2));

        assertEquals(2, firstPage.getContent().size());
        assertEquals(3, firstPage.getTotalElements());
        assertEquals(2, firstPage.getTotalPages());
    }
}
