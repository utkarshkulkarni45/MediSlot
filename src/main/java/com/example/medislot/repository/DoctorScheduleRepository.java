package com.example.medislot.repository;

import com.example.medislot.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    // Retrieve all weekly schedules configured for a doctor
    List<DoctorSchedule> findByDoctorId(Long doctorId);

    // Retrieve doctor's working hours on a specific day of the week
    Optional<DoctorSchedule> findByDoctorIdAndDayOfWeek(Long doctorId, DayOfWeek dayOfWeek);
}

