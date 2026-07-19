ALTER TABLE appointments
  ADD COLUMN preferred_date DATE,
  ADD COLUMN preferred_period VARCHAR(20);

UPDATE appointments
SET preferred_date = appointment_at::DATE,
    preferred_period = CASE
      WHEN appointment_at::TIME < TIME '12:00' THEN 'MORNING'
      ELSE 'AFTERNOON'
    END;

ALTER TABLE appointments
  ALTER COLUMN preferred_date SET NOT NULL,
  ALTER COLUMN preferred_period SET NOT NULL,
  ALTER COLUMN doctor_id DROP NOT NULL,
  ALTER COLUMN appointment_at DROP NOT NULL;

ALTER TABLE appointments DROP CONSTRAINT ck_appointment_status;
ALTER TABLE appointments ADD CONSTRAINT ck_appointment_status
  CHECK (status IN ('REQUESTED','BOOKED','CONFIRMED','CHECKED_IN','COMPLETED','CANCELLED','NO_SHOW'));

ALTER TABLE appointments ADD CONSTRAINT ck_appointment_preferred_period
  CHECK (preferred_period IN ('MORNING','AFTERNOON'));

ALTER TABLE appointments ADD CONSTRAINT ck_appointment_assignment
  CHECK (
    status IN ('REQUESTED','CANCELLED')
    OR (doctor_id IS NOT NULL AND appointment_at IS NOT NULL)
  );

CREATE INDEX ix_appointments_requested
  ON appointments(status, preferred_date, preferred_period, created_at)
  WHERE status = 'REQUESTED';
