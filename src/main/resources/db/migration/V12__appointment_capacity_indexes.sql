-- Support the simplified request flow and capacity checks without scanning all appointments.
CREATE INDEX IF NOT EXISTS ix_appointments_patient_preferred_active
    ON appointments(patient_id, preferred_date)
    WHERE status IN ('REQUESTED','BOOKED','CONFIRMED','CHECKED_IN');

CREATE INDEX IF NOT EXISTS ix_appointments_doctor_preferred_active
    ON appointments(doctor_id, preferred_date, preferred_period, appointment_at)
    WHERE status NOT IN ('CANCELLED','NO_SHOW');

CREATE INDEX IF NOT EXISTS ix_appointments_latest_activity
    ON appointments(preferred_date DESC, appointment_at DESC);
