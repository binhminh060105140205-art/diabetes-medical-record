-- A medical record created from an encounter must keep that encounter's patient and doctor.
UPDATE MedicalRecords m
SET patient_id = e.patient_id,
    doctor_id = e.doctor_id
FROM encounters e
WHERE m.encounter_id = e.encounter_id
  AND (m.patient_id IS DISTINCT FROM e.patient_id
       OR m.doctor_id IS DISTINCT FROM e.doctor_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_medicalrecords_encounter
    ON MedicalRecords(encounter_id)
    WHERE encounter_id IS NOT NULL;
