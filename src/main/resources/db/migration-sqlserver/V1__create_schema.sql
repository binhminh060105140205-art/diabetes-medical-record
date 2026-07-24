SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;

CREATE TABLE users (
    user_id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_users PRIMARY KEY,
    username NVARCHAR(50) NOT NULL CONSTRAINT uq_users_username UNIQUE,
    password NVARCHAR(255) NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    phone NVARCHAR(15) NULL,
    role NVARCHAR(20) NOT NULL,
    status NVARCHAR(20) NOT NULL CONSTRAINT df_users_status DEFAULT N'ACTIVE',
    email NVARCHAR(100) NULL,
    dob DATE NULL,
    gender NVARCHAR(10) NULL,
    address NVARCHAR(255) NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_users_created_at DEFAULT SYSDATETIME(),
    cccd NVARCHAR(50) NULL,
    failed_login_attempts INT NOT NULL CONSTRAINT df_users_failed_login DEFAULT 0,
    lock_until DATETIME2(3) NULL,
    normalized_cccd AS NULLIF(LTRIM(RTRIM(cccd)), N'') PERSISTED
);

CREATE UNIQUE INDEX uq_users_cccd ON users(normalized_cccd)
    WHERE cccd IS NOT NULL AND cccd <> N'';
CREATE INDEX ix_users_role_created ON users(role, created_at DESC);
CREATE INDEX ix_users_created ON users(created_at DESC);
CREATE INDEX ix_users_lock_until ON users(lock_until) WHERE lock_until IS NOT NULL;

CREATE TABLE doctors (
    doctor_id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_doctors PRIMARY KEY,
    user_id INT NOT NULL CONSTRAINT uq_doctors_user UNIQUE,
    specialty NVARCHAR(100) NOT NULL,
    license_no NVARCHAR(50) NOT NULL CONSTRAINT uq_doctors_license UNIQUE,
    license_issue_date DATE NULL,
    license_expire_date DATE NULL,
    license_issued_by NVARCHAR(150) NULL,
    degree NVARCHAR(50) NULL,
    consultation_fee DECIMAL(18,2) NULL,
    face_image_path NVARCHAR(255) NULL,
    cccd_image_path NVARCHAR(255) NULL,
    license_image_path NVARCHAR(255) NULL,
    diabetes_focus NVARCHAR(10) NOT NULL CONSTRAINT df_doctors_diabetes_focus DEFAULT N'BOTH',
    cccd_back_image_path NVARCHAR(255) NULL,
    CONSTRAINT fk_doctors_users FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT ck_doctors_diabetes_focus CHECK (diabetes_focus IN (N'TYPE_1', N'TYPE_2', N'BOTH')),
    CONSTRAINT ck_doctors_specialty_diabetes CHECK (specialty = N'Nội tiết - Đái tháo đường')
);

CREATE INDEX ix_doctors_diabetes_focus ON doctors(diabetes_focus, doctor_id);

CREATE TABLE patients (
    patient_id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_patients PRIMARY KEY,
    user_id INT NULL CONSTRAINT uq_patients_user UNIQUE,
    full_name NVARCHAR(100) NOT NULL,
    date_of_birth DATE NULL,
    gender NVARCHAR(10) NULL,
    phone NVARCHAR(15) NULL,
    address NVARCHAR(255) NULL,
    health_insurance_no NVARCHAR(20) NULL,
    created_by INT NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_patients_created_at DEFAULT SYSDATETIME(),
    national_id NVARCHAR(20) NULL,
    national_id_date DATE NULL,
    national_id_place NVARCHAR(150) NULL,
    normalized_national_id AS NULLIF(LTRIM(RTRIM(national_id)), N'') PERSISTED,
    CONSTRAINT fk_patients_users FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE UNIQUE INDEX uq_patients_national_id ON patients(normalized_national_id)
    WHERE national_id IS NOT NULL AND national_id <> N'';
CREATE INDEX ix_patients_created ON patients(created_at DESC);
CREATE INDEX ix_patients_phone ON patients(phone);
CREATE INDEX ix_patients_name ON patients(full_name, patient_id);

CREATE TABLE appointments (
    appointment_id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_appointments PRIMARY KEY,
    patient_id INT NOT NULL,
    doctor_id INT NULL,
    appointment_at DATETIME2(3) NULL,
    reason NVARCHAR(255) NOT NULL,
    status NVARCHAR(30) NOT NULL CONSTRAINT df_appointments_status DEFAULT N'BOOKED',
    note NVARCHAR(500) NULL,
    created_by INT NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_appointments_created_at DEFAULT SYSDATETIME(),
    preferred_date DATE NOT NULL,
    preferred_period NVARCHAR(20) NOT NULL,
    active_slot AS CASE
        WHEN status IN (N'BOOKED', N'CONFIRMED', N'CHECKED_IN') THEN CONVERT(TINYINT, 1)
        ELSE NULL
    END PERSISTED,
    CONSTRAINT fk_appointments_patients FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT fk_appointments_doctors FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT fk_appointments_users FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT ck_appointment_status CHECK (status IN (
        N'REQUESTED', N'BOOKED', N'CONFIRMED', N'CHECKED_IN',
        N'COMPLETED', N'CANCELLED', N'NO_SHOW')),
    CONSTRAINT ck_appointment_preferred_period CHECK (preferred_period IN (N'MORNING', N'AFTERNOON')),
    CONSTRAINT ck_appointment_assignment CHECK (
        status IN (N'REQUESTED', N'CANCELLED') OR (doctor_id IS NOT NULL AND appointment_at IS NOT NULL))
);

CREATE INDEX ix_appointments_date ON appointments(appointment_at, status);
CREATE UNIQUE INDEX uq_doctor_appointment_slot
    ON appointments(doctor_id, appointment_at, active_slot)
    WHERE status IN (N'BOOKED', N'CONFIRMED', N'CHECKED_IN')
      AND doctor_id IS NOT NULL AND appointment_at IS NOT NULL;
CREATE INDEX ix_appointments_requested
    ON appointments(status, preferred_date, preferred_period, created_at)
    WHERE status = N'REQUESTED';
CREATE INDEX ix_appointments_patient_date ON appointments(patient_id, appointment_at DESC);
CREATE INDEX ix_appointments_doctor_date ON appointments(doctor_id, appointment_at DESC);
CREATE INDEX ix_appointments_patient_preferred_active ON appointments(patient_id, preferred_date, status);
CREATE INDEX ix_appointments_doctor_preferred_active
    ON appointments(doctor_id, preferred_date, preferred_period, appointment_at, status);
CREATE INDEX ix_appointments_latest_activity ON appointments(preferred_date DESC, appointment_at DESC);

CREATE TABLE encounters (
    encounter_id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_encounters PRIMARY KEY,
    appointment_id INT NULL CONSTRAINT uq_encounters_appointment UNIQUE,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    status NVARCHAR(30) NOT NULL CONSTRAINT df_encounters_status DEFAULT N'WAITING_TRIAGE',
    check_in_at DATETIME2(3) NOT NULL CONSTRAINT df_encounters_check_in DEFAULT SYSDATETIME(),
    triage_at DATETIME2(3) NULL,
    consultation_started_at DATETIME2(3) NULL,
    completed_at DATETIME2(3) NULL,
    room_no NVARCHAR(30) NULL,
    created_by INT NULL,
    CONSTRAINT fk_encounters_appointments FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id),
    CONSTRAINT fk_encounters_patients FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT fk_encounters_doctors FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT fk_encounters_users FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT ck_encounter_status CHECK (status IN (
        N'WAITING_TRIAGE', N'WAITING_DOCTOR', N'IN_CONSULTATION', N'WAITING_LAB',
        N'LAB_COMPLETED', N'COMPLETED', N'CANCELLED'))
);

CREATE INDEX ix_encounters_status ON encounters(status, check_in_at);
CREATE INDEX ix_encounters_doctor_status ON encounters(doctor_id, status, check_in_at DESC);

CREATE TABLE queue_entries (
    queue_id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_queue_entries PRIMARY KEY,
    encounter_id INT NOT NULL CONSTRAINT uq_queue_encounter UNIQUE,
    doctor_id INT NOT NULL,
    queue_number INT NOT NULL,
    priority NVARCHAR(20) NOT NULL CONSTRAINT df_queue_priority DEFAULT N'NORMAL',
    status NVARCHAR(20) NOT NULL CONSTRAINT df_queue_status DEFAULT N'WAITING',
    queued_at DATETIME2(3) NOT NULL CONSTRAINT df_queue_queued_at DEFAULT SYSDATETIME(),
    called_at DATETIME2(3) NULL,
    CONSTRAINT fk_queue_encounters FOREIGN KEY (encounter_id) REFERENCES encounters(encounter_id),
    CONSTRAINT fk_queue_doctors FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT ck_queue_priority CHECK (priority IN (N'NORMAL', N'PRIORITY', N'EMERGENCY')),
    CONSTRAINT ck_queue_status CHECK (status IN (N'WAITING', N'CALLED', N'IN_SERVICE', N'COMPLETED', N'SKIPPED'))
);

CREATE INDEX ix_queue_doctor ON queue_entries(doctor_id, status, queue_number);
CREATE INDEX ix_queue_entries_queued_at ON queue_entries(queued_at, queue_number DESC);

CREATE TABLE patient_allergies (
    allergy_id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_patient_allergies PRIMARY KEY,
    patient_id INT NOT NULL,
    allergen NVARCHAR(150) NOT NULL,
    reaction NVARCHAR(255) NULL,
    severity NVARCHAR(20) NOT NULL CONSTRAINT df_allergy_severity DEFAULT N'UNKNOWN',
    status NVARCHAR(20) NOT NULL CONSTRAINT df_allergy_status DEFAULT N'ACTIVE',
    noted_by INT NULL,
    noted_at DATETIME2(3) NOT NULL CONSTRAINT df_allergy_noted_at DEFAULT SYSDATETIME(),
    CONSTRAINT fk_allergies_patients FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT fk_allergies_users FOREIGN KEY (noted_by) REFERENCES users(user_id),
    CONSTRAINT uq_patient_allergen UNIQUE (patient_id, allergen),
    CONSTRAINT ck_allergy_severity CHECK (severity IN (N'MILD', N'MODERATE', N'SEVERE', N'UNKNOWN'))
);

CREATE INDEX ix_allergies_patient ON patient_allergies(patient_id, status);

CREATE TABLE patient_medical_histories (
    history_id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_patient_histories PRIMARY KEY,
    patient_id INT NOT NULL,
    history_type NVARCHAR(30) NOT NULL,
    condition_name NVARCHAR(150) NOT NULL,
    diagnosed_date DATE NULL,
    status NVARCHAR(20) NOT NULL CONSTRAINT df_history_status DEFAULT N'ACTIVE',
    note NVARCHAR(500) NULL,
    noted_by INT NULL,
    noted_at DATETIME2(3) NOT NULL CONSTRAINT df_history_noted_at DEFAULT SYSDATETIME(),
    CONSTRAINT fk_histories_patients FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT fk_histories_users FOREIGN KEY (noted_by) REFERENCES users(user_id),
    CONSTRAINT ck_history_type CHECK (history_type IN (N'PERSONAL', N'FAMILY', N'SURGICAL', N'LIFESTYLE'))
);

CREATE INDEX ix_history_patient ON patient_medical_histories(patient_id, history_type);

CREATE TABLE lab_orders (
    lab_order_id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_lab_orders PRIMARY KEY,
    encounter_id INT NOT NULL,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    test_code NVARCHAR(30) NOT NULL,
    test_name NVARCHAR(150) NOT NULL,
    status NVARCHAR(20) NOT NULL CONSTRAINT df_lab_status DEFAULT N'ORDERED',
    priority NVARCHAR(20) NOT NULL CONSTRAINT df_lab_priority DEFAULT N'ROUTINE',
    clinical_note NVARCHAR(500) NULL,
    ordered_at DATETIME2(3) NOT NULL CONSTRAINT df_lab_ordered_at DEFAULT SYSDATETIME(),
    result_value NVARCHAR(100) NULL,
    result_unit NVARCHAR(30) NULL,
    reference_range NVARCHAR(100) NULL,
    result_flag NVARCHAR(20) NULL,
    resulted_by INT NULL,
    resulted_at DATETIME2(3) NULL,
    active_test_code AS CASE
        WHEN status <> N'CANCELLED' THEN UPPER(test_code)
        ELSE NULL
    END PERSISTED,
    CONSTRAINT fk_lab_encounters FOREIGN KEY (encounter_id) REFERENCES encounters(encounter_id),
    CONSTRAINT fk_lab_patients FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT fk_lab_doctors FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT fk_lab_users FOREIGN KEY (resulted_by) REFERENCES users(user_id),
    CONSTRAINT ck_lab_status CHECK (status IN (N'ORDERED', N'COLLECTED', N'RESULTED', N'REVIEWED', N'CANCELLED')),
    CONSTRAINT ck_lab_priority CHECK (priority IN (N'ROUTINE', N'URGENT'))
);

CREATE INDEX ix_lab_encounter ON lab_orders(encounter_id, status);
CREATE INDEX ix_lab_doctor_status ON lab_orders(doctor_id, status, ordered_at DESC);
CREATE UNIQUE INDEX uq_lab_active_test_per_encounter
    ON lab_orders(encounter_id, active_test_code)
    WHERE status <> N'CANCELLED';

CREATE TABLE medicalrecords (
    record_id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_medicalrecords PRIMARY KEY,
    patient_id INT NOT NULL,
    doctor_id INT NULL,
    created_by_staff INT NULL,
    visit_date DATETIME2(3) NOT NULL CONSTRAINT df_records_visit_date DEFAULT SYSDATETIME(),
    reason_for_visit NVARCHAR(255) NULL,
    symptoms NVARCHAR(MAX) NULL,
    medical_history NVARCHAR(MAX) NULL,
    lifestyle_habits NVARCHAR(MAX) NULL,
    clinical_exam NVARCHAR(MAX) NULL,
    complication_note NVARCHAR(MAX) NULL,
    final_diagnosis NVARCHAR(255) NULL,
    treatment_plan NVARCHAR(MAX) NULL,
    prescription_note NVARCHAR(MAX) NULL,
    advice NVARCHAR(MAX) NULL,
    follow_up_date DATE NULL,
    doctor_note NVARCHAR(MAX) NULL,
    status NVARCHAR(20) NOT NULL CONSTRAINT df_records_status DEFAULT N'DRAFT',
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_records_created_at DEFAULT SYSDATETIME(),
    encounter_id INT NULL,
    CONSTRAINT fk_records_patients FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT fk_records_doctors FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT fk_records_encounters FOREIGN KEY (encounter_id) REFERENCES encounters(encounter_id),
    CONSTRAINT uq_medicalrecords_encounter UNIQUE (encounter_id)
);

CREATE INDEX ix_records_patient ON medicalrecords(patient_id, visit_date DESC);
CREATE INDEX ix_records_doctor_status ON medicalrecords(doctor_id, status, visit_date DESC);

CREATE TABLE healthindicators (
    indicator_id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_healthindicators PRIMARY KEY,
    record_id INT NOT NULL CONSTRAINT uq_healthindicator_record UNIQUE,
    entered_by_staff INT NULL,
    height DECIMAL(5,2) NULL,
    weight DECIMAL(5,2) NULL,
    bmi DECIMAL(4,2) NULL,
    systolic_bp INT NULL,
    diastolic_bp INT NULL,
    heart_rate INT NULL,
    temperature DECIMAL(4,1) NULL,
    blood_glucose DECIMAL(5,2) NULL,
    hba1c DECIMAL(4,2) NULL,
    cholesterol DECIMAL(5,2) NULL,
    triglyceride DECIMAL(5,2) NULL,
    hdl_c DECIMAL(5,2) NULL,
    ldl_c DECIMAL(5,2) NULL,
    measured_at DATETIME2(3) NOT NULL CONSTRAINT df_health_measured_at DEFAULT SYSDATETIME(),
    CONSTRAINT fk_health_records FOREIGN KEY (record_id) REFERENCES medicalrecords(record_id)
);

CREATE TABLE prescriptionitems (
    prescription_item_id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_prescriptionitems PRIMARY KEY,
    record_id INT NOT NULL,
    medicine_name NVARCHAR(150) NOT NULL,
    dosage NVARCHAR(100) NOT NULL,
    frequency NVARCHAR(100) NULL,
    duration_days INT NULL,
    CONSTRAINT fk_prescriptions_records FOREIGN KEY (record_id)
        REFERENCES medicalrecords(record_id) ON DELETE CASCADE,
    CONSTRAINT ck_prescription_duration CHECK (duration_days BETWEEN 1 AND 365)
);

CREATE INDEX idx_prescription_record ON prescriptionitems(record_id);

CREATE TABLE patientdailylogs (
    log_id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_patientdailylogs PRIMARY KEY,
    patient_id INT NOT NULL,
    log_date DATE NOT NULL CONSTRAINT df_daily_log_date DEFAULT CAST(GETDATE() AS DATE),
    blood_glucose DECIMAL(5,2) NULL,
    systolic_bp INT NULL,
    diastolic_bp INT NULL,
    weight DECIMAL(5,2) NULL,
    note NVARCHAR(MAX) NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_daily_created_at DEFAULT SYSDATETIME(),
    symptoms NVARCHAR(MAX) NULL,
    heart_rate INT NULL,
    spo2 DECIMAL(4,1) NULL,
    meal_type NVARCHAR(20) NULL,
    CONSTRAINT fk_daily_patients FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT uq_dailylog_patient_date UNIQUE (patient_id, log_date)
);

CREATE INDEX ix_daily_patient_date ON patientdailylogs(patient_id, log_date DESC);

CREATE TABLE devicereadings (
    id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_devicereadings PRIMARY KEY,
    patient_id INT NOT NULL,
    device_type NVARCHAR(50) NOT NULL,
    raw_json_data NVARCHAR(MAX) NOT NULL,
    parsed_glucose DECIMAL(6,2) NULL,
    parsed_heart_rate INT NULL,
    parsed_systolic_bp INT NULL,
    parsed_diastolic_bp INT NULL,
    parsed_weight DECIMAL(5,2) NULL,
    parsed_spo2 DECIMAL(4,1) NULL,
    measured_at DATETIME2(3) NOT NULL CONSTRAINT df_device_measured_at DEFAULT SYSDATETIME(),
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_device_created_at DEFAULT SYSDATETIME(),
    is_abnormal BIT NOT NULL CONSTRAINT df_device_abnormal DEFAULT 0,
    abnormal_note NVARCHAR(500) NULL,
    CONSTRAINT fk_device_patients FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
);

CREATE INDEX ix_device_patient_date ON devicereadings(patient_id, measured_at DESC);

CREATE TABLE healthalerts (
    alert_id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_healthalerts PRIMARY KEY,
    patient_id INT NOT NULL,
    indicator_type NVARCHAR(50) NOT NULL,
    value DECIMAL(8,2) NOT NULL,
    threshold DECIMAL(8,2) NOT NULL,
    alert_level NVARCHAR(20) NOT NULL CONSTRAINT df_alert_level DEFAULT N'medium',
    alert_message NVARCHAR(500) NOT NULL,
    data_source NVARCHAR(20) NOT NULL CONSTRAINT df_alert_source DEFAULT N'manual',
    source_record_id INT NULL,
    is_acknowledged BIT NOT NULL CONSTRAINT df_alert_ack DEFAULT 0,
    acknowledged_at DATETIME2(3) NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_alert_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT fk_alert_patients FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
);

CREATE INDEX ix_alert_patient_ack ON healthalerts(patient_id, is_acknowledged, created_at DESC);
CREATE INDEX ix_healthalerts_patient_created ON healthalerts(patient_id, created_at DESC);

CREATE TABLE nextappointment (
    appointment_id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_nextappointment PRIMARY KEY,
    patient_id INT NOT NULL,
    appointment_date DATE NOT NULL,
    source NVARCHAR(20) NOT NULL CONSTRAINT df_nextappointment_source DEFAULT N'AUTO',
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_nextappointment_created DEFAULT SYSDATETIME(),
    CONSTRAINT fk_nextappointment_patients FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
);

CREATE TABLE audit_logs (
    audit_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_audit_logs PRIMARY KEY,
    user_id INT NULL,
    action NVARCHAR(50) NOT NULL,
    entity_type NVARCHAR(50) NOT NULL,
    entity_id NVARCHAR(50) NULL,
    details NVARCHAR(MAX) NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_audit_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT fk_audit_users FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE diabetes_profiles (
    patient_id INT NOT NULL CONSTRAINT pk_diabetes_profiles PRIMARY KEY,
    diabetes_type NVARCHAR(10) NOT NULL CONSTRAINT df_diabetes_type DEFAULT N'UNKNOWN',
    diagnosis_date DATE NULL,
    treatment_method NVARCHAR(20) NULL,
    hba1c_target DECIMAL(4,2) NULL,
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_diabetes_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT fk_diabetes_patients FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT ck_diabetes_type CHECK (diabetes_type IN (N'TYPE_1', N'TYPE_2', N'UNKNOWN')),
    CONSTRAINT ck_diabetes_treatment CHECK (treatment_method IS NULL OR treatment_method IN (
        N'INSULIN', N'ORAL_MEDICATION', N'LIFESTYLE', N'COMBINATION'))
);

CREATE INDEX ix_diabetes_profiles_type ON diabetes_profiles(diabetes_type, patient_id);

CREATE TABLE patient_ai_daily_advice (
    advice_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_patient_ai_advice PRIMARY KEY,
    patient_id INT NOT NULL,
    advice_date DATE NOT NULL CONSTRAINT df_patient_ai_date DEFAULT CAST(GETDATE() AS DATE),
    summary NVARCHAR(MAX) NOT NULL,
    advice_items_json NVARCHAR(MAX) NOT NULL,
    severity NVARCHAR(10) NOT NULL,
    doctor_recommendation BIT NOT NULL CONSTRAINT df_patient_ai_doctor DEFAULT 0,
    source_hash NVARCHAR(64) NOT NULL,
    model NVARCHAR(80) NOT NULL,
    fallback_used BIT NOT NULL CONSTRAINT df_patient_ai_fallback DEFAULT 0,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_patient_ai_created DEFAULT SYSDATETIME(),
    CONSTRAINT fk_patient_ai_patients FOREIGN KEY (patient_id)
        REFERENCES patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT uq_patient_ai_advice_day UNIQUE (patient_id, advice_date),
    CONSTRAINT ck_patient_ai_severity CHECK (severity IN (N'low', N'medium', N'high'))
);

CREATE INDEX ix_patient_ai_advice_patient_date
    ON patient_ai_daily_advice(patient_id, advice_date DESC);

CREATE TABLE login_history (
    login_history_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_login_history PRIMARY KEY,
    user_id INT NULL,
    username NVARCHAR(50) NULL,
    full_name NVARCHAR(100) NULL,
    role NVARCHAR(20) NULL,
    event_type NVARCHAR(20) NOT NULL,
    ip_address NVARCHAR(100) NULL,
    user_agent NVARCHAR(500) NULL,
    session_id NVARCHAR(120) NULL,
    occurred_at DATETIME2(3) NOT NULL CONSTRAINT df_login_occurred_at DEFAULT SYSDATETIME(),
    CONSTRAINT fk_login_users FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT ck_login_history_event CHECK (event_type IN (N'LOGIN', N'LOGOUT'))
);

CREATE INDEX ix_login_history_occurred_at ON login_history(occurred_at DESC);
CREATE INDEX ix_login_history_user ON login_history(user_id, occurred_at DESC);

CREATE TABLE admin_trash (
    trash_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_admin_trash PRIMARY KEY,
    entity_type NVARCHAR(30) NOT NULL,
    entity_id INT NOT NULL,
    display_name NVARCHAR(150) NOT NULL,
    details NVARCHAR(MAX) NULL,
    deleted_by INT NULL,
    deleted_at DATETIME2(3) NOT NULL CONSTRAINT df_trash_deleted_at DEFAULT SYSDATETIME(),
    restored_by INT NULL,
    restored_at DATETIME2(3) NULL,
    purged_by INT NULL,
    purged_at DATETIME2(3) NULL,
    CONSTRAINT fk_trash_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(user_id),
    CONSTRAINT fk_trash_restored_by FOREIGN KEY (restored_by) REFERENCES users(user_id),
    CONSTRAINT fk_trash_purged_by FOREIGN KEY (purged_by) REFERENCES users(user_id)
);

CREATE INDEX ix_admin_trash_active ON admin_trash(deleted_at DESC)
    WHERE restored_at IS NULL AND purged_at IS NULL;
