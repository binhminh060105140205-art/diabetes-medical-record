INSERT INTO diabetes_profiles(patient_id, diabetes_type)
SELECT patient_id, 'UNKNOWN' FROM patients
ON CONFLICT (patient_id) DO NOTHING;

-- Chỉ bác sĩ nội tiết/đái tháo đường được ưu tiên cho cả Type 1 và Type 2.
-- Các chuyên khoa hỗ trợ vẫn ở GENERAL và vẫn có thể được chọn.
UPDATE doctors
SET diabetes_focus='BOTH'
WHERE diabetes_focus='GENERAL'
  AND (LOWER(specialty) LIKE '%nội tiết%' OR LOWER(specialty) LIKE '%đái tháo đường%');

CREATE INDEX IF NOT EXISTS ix_diabetes_profiles_type
  ON diabetes_profiles(diabetes_type, patient_id);

CREATE INDEX IF NOT EXISTS ix_doctors_diabetes_focus
  ON doctors(diabetes_focus, doctor_id);
