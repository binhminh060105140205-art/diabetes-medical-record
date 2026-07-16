-- Indexes for the routes executed immediately after login and during navigation.
CREATE INDEX IF NOT EXISTS ix_users_role_created
    ON users(role, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_users_created
    ON users(created_at DESC);

CREATE INDEX IF NOT EXISTS ix_patients_phone
    ON patients(phone);

CREATE INDEX IF NOT EXISTS ix_appointments_patient_date
    ON appointments(patient_id, appointment_at DESC);

CREATE INDEX IF NOT EXISTS ix_appointments_doctor_date
    ON appointments(doctor_id, appointment_at DESC)
    WHERE status NOT IN ('CANCELLED', 'NO_SHOW');

CREATE INDEX IF NOT EXISTS ix_encounters_doctor_status
    ON encounters(doctor_id, status, check_in_at DESC);

CREATE INDEX IF NOT EXISTS ix_lab_doctor_status
    ON lab_orders(doctor_id, status, ordered_at DESC);
