CREATE TABLE diabetes_profiles (
  patient_id       INTEGER PRIMARY KEY REFERENCES patients(patient_id),
  diabetes_type    VARCHAR(10) NOT NULL DEFAULT 'UNKNOWN'
                     CHECK (diabetes_type IN ('TYPE_1','TYPE_2','UNKNOWN')),
  diagnosis_date   DATE,
  treatment_method VARCHAR(20)
                     CHECK (treatment_method IN ('INSULIN','ORAL_MEDICATION','LIFESTYLE','COMBINATION')),
  hba1c_target     NUMERIC(4,2),
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO diabetes_profiles(patient_id, diabetes_type)
SELECT patient_id, 'UNKNOWN' FROM patients;

ALTER TABLE doctors ADD COLUMN diabetes_focus VARCHAR(10) NOT NULL DEFAULT 'GENERAL'
  CHECK (diabetes_focus IN ('TYPE_1','TYPE_2','BOTH','GENERAL'));
