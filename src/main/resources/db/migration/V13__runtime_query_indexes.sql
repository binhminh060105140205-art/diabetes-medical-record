-- Index the high-traffic lists used by patient, device and check-in screens.
CREATE INDEX IF NOT EXISTS ix_patients_name
    ON patients(full_name, patient_id);

CREATE INDEX IF NOT EXISTS ix_healthalerts_patient_created
    ON healthalerts(patient_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_queue_entries_queued_at
    ON queue_entries(queued_at, queue_number DESC);
