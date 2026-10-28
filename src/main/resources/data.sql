-- Seed Doctors
INSERT INTO doctors (id, name, specialization, consultation_minutes)
VALUES (1, 'Dr. Sarah Adams', 'Cardiology', 30)
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO doctors (id, name, specialization, consultation_minutes)
VALUES (2, 'Dr. Robert Chen', 'Dermatology', 15)
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO doctors (id, name, specialization, consultation_minutes)
VALUES (3, 'Dr. Elena Patel', 'General Medicine', 20)
ON DUPLICATE KEY UPDATE name=name;

-- Seed Schedules (Monday through Friday)
-- Dr. Adams (Cardiology, 30m): Mon, Wed, Fri 09:00 - 13:00
INSERT INTO doctor_schedules (id, doctor_id, day_of_week, start_time, end_time)
VALUES (1, 1, 'MONDAY', '09:00:00', '13:00:00')
ON DUPLICATE KEY UPDATE start_time=start_time;

INSERT INTO doctor_schedules (id, doctor_id, day_of_week, start_time, end_time)
VALUES (2, 1, 'WEDNESDAY', '09:00:00', '13:00:00')
ON DUPLICATE KEY UPDATE start_time=start_time;

INSERT INTO doctor_schedules (id, doctor_id, day_of_week, start_time, end_time)
VALUES (3, 1, 'FRIDAY', '09:00:00', '13:00:00')
ON DUPLICATE KEY UPDATE start_time=start_time;

-- Dr. Chen (Dermatology, 15m): Tue, Thu 14:00 - 17:00
INSERT INTO doctor_schedules (id, doctor_id, day_of_week, start_time, end_time)
VALUES (4, 2, 'TUESDAY', '14:00:00', '17:00:00')
ON DUPLICATE KEY UPDATE start_time=start_time;

INSERT INTO doctor_schedules (id, doctor_id, day_of_week, start_time, end_time)
VALUES (5, 2, 'THURSDAY', '14:00:00', '17:00:00')
ON DUPLICATE KEY UPDATE start_time=start_time;

-- Dr. Patel (General Medicine, 20m): Mon to Fri 10:00 - 14:00
INSERT INTO doctor_schedules (id, doctor_id, day_of_week, start_time, end_time)
VALUES (6, 3, 'MONDAY', '10:00:00', '14:00:00')
ON DUPLICATE KEY UPDATE start_time=start_time;

INSERT INTO doctor_schedules (id, doctor_id, day_of_week, start_time, end_time)
VALUES (7, 3, 'TUESDAY', '10:00:00', '14:00:00')
ON DUPLICATE KEY UPDATE start_time=start_time;

INSERT INTO doctor_schedules (id, doctor_id, day_of_week, start_time, end_time)
VALUES (8, 3, 'WEDNESDAY', '10:00:00', '14:00:00')
ON DUPLICATE KEY UPDATE start_time=start_time;

-- Seed Patients for immediate testing
INSERT INTO patients (id, name, phone, email)
VALUES (1, 'Alice Smith', '555-0101', 'alice.smith@example.com')
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO patients (id, name, phone, email)
VALUES (2, 'Bob Johnson', '555-0102', 'bob.johnson@example.com')
ON DUPLICATE KEY UPDATE name=name;

