package com.example.medislot.repository;

import com.example.medislot.entity.Appointment;
import com.example.medislot.entity.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Fetch active (BOOKED) appointments for a doctor between a date range (for generating available slots)
    List<Appointment> findByDoctorIdAndStatusAndSlotStartBetween(
            Long doctorId,
            AppointmentStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    // List all appointments for a patient
    List<Appointment> findByPatientIdOrderBySlotStartDesc(Long patientId);

    // Concurrency control: Acquire PESSIMISTIC_WRITE lock (SELECT ... FOR UPDATE) on conflicting booked slot
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.slotStart = :slotStart AND a.status = :status")
    Optional<Appointment> findConflictingBookingWithLock(
            @Param("doctorId") Long doctorId,
            @Param("slotStart") LocalDateTime slotStart,
            @Param("status") AppointmentStatus status
    );
}

