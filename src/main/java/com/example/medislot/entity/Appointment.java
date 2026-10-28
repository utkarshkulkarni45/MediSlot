package com.example.medislot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "appointments",
    uniqueConstraints = {
        // Database safety net: prevents duplicate bookings for active bookings (doctor_id, slot_start, active_status)
        @UniqueConstraint(name = "uk_doctor_slot_active", columnNames = {"doctor_id", "slot_start", "active_status"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "slot_start", nullable = false)
    private LocalDateTime slotStart;

    @Column(name = "slot_end", nullable = false)
    private LocalDateTime slotEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    // Active status marker: equals status value when BOOKED, null when CANCELLED
    // In SQL databases, UNIQUE constraints treat NULLs as distinct, allowing re-booking of cancelled slots
    @Column(name = "active_status")
    private String activeStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Optimistic locking / audit tracking token
    @Version
    private Long version;

    @PrePersist
    @jakarta.persistence.PreUpdate
    protected void onPersistOrUpdate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == AppointmentStatus.BOOKED) {
            this.activeStatus = AppointmentStatus.BOOKED.name();
        } else {
            this.activeStatus = null;
        }
    }
}

