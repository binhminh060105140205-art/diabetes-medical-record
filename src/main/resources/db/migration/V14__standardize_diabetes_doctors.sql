UPDATE doctors
SET specialty = 'Nội tiết - Đái tháo đường',
    diabetes_focus = CASE
      WHEN diabetes_focus IN ('TYPE_1', 'TYPE_2', 'BOTH') THEN diabetes_focus
      ELSE 'BOTH'
    END;

ALTER TABLE doctors ALTER COLUMN diabetes_focus SET DEFAULT 'BOTH';
ALTER TABLE doctors DROP CONSTRAINT IF EXISTS doctors_diabetes_focus_check;
ALTER TABLE doctors DROP CONSTRAINT IF EXISTS ck_doctors_diabetes_focus;
ALTER TABLE doctors ADD CONSTRAINT ck_doctors_diabetes_focus
  CHECK (diabetes_focus IN ('TYPE_1', 'TYPE_2', 'BOTH'));

ALTER TABLE doctors DROP CONSTRAINT IF EXISTS ck_doctors_specialty_diabetes;
ALTER TABLE doctors ADD CONSTRAINT ck_doctors_specialty_diabetes
  CHECK (specialty = 'Nội tiết - Đái tháo đường');
