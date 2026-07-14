/*
  AUTHORITATIVE LEGACY -> CURRENT MIGRATION
  - Preserves existing rows.
  - Can be run repeatedly.
  - Makes the supplied legacy schema compatible with the current Java code.
*/
USE [master];
GO
IF DB_ID(N'DiabetesMedicalRecordDB') IS NULL
    CREATE DATABASE [DiabetesMedicalRecordDB];
GO
USE [DiabetesMedicalRecordDB];
GO
SET XACT_ABORT ON;
SET NOCOUNT ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    /* ---------- Core tables: only create when the legacy DB does not have them ---------- */
    IF OBJECT_ID(N'dbo.Users', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.Users (
            user_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            username VARCHAR(50) NOT NULL UNIQUE,
            password VARCHAR(255) NOT NULL,
            full_name NVARCHAR(100) NOT NULL,
            phone VARCHAR(15) NULL,
            role VARCHAR(20) NOT NULL,
            status VARCHAR(20) NOT NULL CONSTRAINT DF_Users_Status DEFAULT 'ACTIVE',
            email VARCHAR(100) NULL,
            dob DATE NULL,
            gender NVARCHAR(10) NULL,
            address NVARCHAR(255) NULL,
            created_at DATETIME NOT NULL CONSTRAINT DF_Users_CreatedAt DEFAULT GETDATE(),
            cccd NVARCHAR(50) NULL
        );
    END;

    IF OBJECT_ID(N'dbo.Doctors', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.Doctors (
            doctor_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            user_id INT NOT NULL,
            specialty NVARCHAR(100) NOT NULL,
            license_no VARCHAR(50) NOT NULL,
            license_issue_date DATE NULL,
            license_expire_date DATE NULL,
            license_issued_by NVARCHAR(150) NULL,
            degree NVARCHAR(50) NULL,
            consultation_fee DECIMAL(18,2) NULL,
            face_image_path NVARCHAR(255) NULL,
            cccd_image_path NVARCHAR(255) NULL,
            license_image_path NVARCHAR(255) NULL,
            CONSTRAINT FK_Doctors_Users FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id)
        );
    END;

    IF OBJECT_ID(N'dbo.Patients', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.Patients (
            patient_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            user_id INT NULL,
            full_name NVARCHAR(100) NOT NULL,
            date_of_birth DATE NULL,
            gender NVARCHAR(10) NULL,
            phone VARCHAR(15) NULL,
            address NVARCHAR(255) NULL,
            health_insurance_no VARCHAR(20) NULL,
            created_by INT NULL,
            created_at DATETIME NOT NULL CONSTRAINT DF_Patients_CreatedAt DEFAULT GETDATE(),
            national_id VARCHAR(20) NULL,
            national_id_date DATE NULL,
            national_id_place NVARCHAR(150) NULL,
            CONSTRAINT FK_Patients_Users FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id)
        );
    END;

    IF OBJECT_ID(N'dbo.MedicalRecords', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.MedicalRecords (
            record_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            patient_id INT NOT NULL,
            doctor_id INT NULL,
            created_by_staff INT NULL,
            visit_date DATETIME NOT NULL CONSTRAINT DF_MedicalRecords_VisitDate DEFAULT GETDATE(),
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
            status VARCHAR(20) NOT NULL CONSTRAINT DF_MedicalRecords_Status DEFAULT 'DRAFT',
            created_at DATETIME NOT NULL CONSTRAINT DF_MedicalRecords_CreatedAt DEFAULT GETDATE(),
            CONSTRAINT FK_Records_Patients FOREIGN KEY (patient_id) REFERENCES dbo.Patients(patient_id),
            CONSTRAINT FK_Records_Doctors FOREIGN KEY (doctor_id) REFERENCES dbo.Doctors(doctor_id)
        );
    END;

    IF OBJECT_ID(N'dbo.HealthIndicators', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.HealthIndicators (
            indicator_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            record_id INT NOT NULL,
            entered_by_staff INT NULL,
            height DECIMAL(5,2) NULL, weight DECIMAL(5,2) NULL, bmi DECIMAL(4,2) NULL,
            systolic_bp INT NULL, diastolic_bp INT NULL, heart_rate INT NULL,
            temperature DECIMAL(4,1) NULL, blood_glucose DECIMAL(5,2) NULL,
            hba1c DECIMAL(4,2) NULL, cholesterol DECIMAL(5,2) NULL,
            triglyceride DECIMAL(5,2) NULL, hdl_c DECIMAL(5,2) NULL, ldl_c DECIMAL(5,2) NULL,
            measured_at DATETIME NOT NULL CONSTRAINT DF_HealthIndicators_MeasuredAt DEFAULT GETDATE(),
            CONSTRAINT FK_Indicators_Records FOREIGN KEY (record_id) REFERENCES dbo.MedicalRecords(record_id)
        );
    END;

    IF OBJECT_ID(N'dbo.AIWarnings', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.AIWarnings (
            warning_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            record_id INT NOT NULL,
            risk_level VARCHAR(20) NULL,
            warning_message NVARCHAR(MAX) NULL,
            suggested_action NVARCHAR(MAX) NULL,
            ai_score DECIMAL(5,2) NULL,
            generated_at DATETIME NOT NULL CONSTRAINT DF_AIWarnings_GeneratedAt DEFAULT GETDATE(),
            reviewed_by_doctor INT NULL,
            CONSTRAINT FK_Warnings_Records FOREIGN KEY (record_id) REFERENCES dbo.MedicalRecords(record_id)
        );
    END;

    IF OBJECT_ID(N'dbo.AIAdviceHistory', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.AIAdviceHistory (
            advice_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            patient_id INT NOT NULL,
            advice_date DATE NOT NULL CONSTRAINT DF_AIAdviceHistory_AdviceDate DEFAULT CAST(GETDATE() AS DATE),
            advice_content NVARCHAR(MAX) NULL,
            created_at DATETIME NOT NULL CONSTRAINT DF_AIAdviceHistory_CreatedAt DEFAULT GETDATE(),
            risk_level VARCHAR(20) NULL,
            recommendation_type VARCHAR(50) NULL,
            source_data_reference NVARCHAR(MAX) NULL,
            avg_glucose_7d DECIMAL(5,2) NULL,
            avg_systolic_7d INT NULL,
            CONSTRAINT FK_AIAdvice_Patients FOREIGN KEY (patient_id) REFERENCES dbo.Patients(patient_id)
        );
    END;

    IF OBJECT_ID(N'dbo.PatientDailyLogs', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.PatientDailyLogs (
            log_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            patient_id INT NOT NULL,
            log_date DATE NOT NULL CONSTRAINT DF_PatientDailyLogs_LogDate DEFAULT CAST(GETDATE() AS DATE),
            blood_glucose DECIMAL(5,2) NULL, systolic_bp INT NULL, diastolic_bp INT NULL,
            weight DECIMAL(5,2) NULL, note NVARCHAR(MAX) NULL,
            created_at DATETIME NOT NULL CONSTRAINT DF_PatientDailyLogs_CreatedAt DEFAULT GETDATE(),
            symptoms NVARCHAR(MAX) NULL, heart_rate INT NULL, spo2 DECIMAL(4,1) NULL,
            meal_type VARCHAR(20) NULL,
            CONSTRAINT FK_Logs_Patients FOREIGN KEY (patient_id) REFERENCES dbo.Patients(patient_id)
        );
    END;

    /* ---------- Columns missing from the supplied legacy schema ---------- */
    IF COL_LENGTH('dbo.Users', 'cccd') IS NULL ALTER TABLE dbo.Users ADD cccd NVARCHAR(50) NULL;

    IF COL_LENGTH('dbo.Doctors', 'face_image_path') IS NULL ALTER TABLE dbo.Doctors ADD face_image_path NVARCHAR(255) NULL;
    IF COL_LENGTH('dbo.Doctors', 'cccd_image_path') IS NULL ALTER TABLE dbo.Doctors ADD cccd_image_path NVARCHAR(255) NULL;
    IF COL_LENGTH('dbo.Doctors', 'license_image_path') IS NULL ALTER TABLE dbo.Doctors ADD license_image_path NVARCHAR(255) NULL;

    IF COL_LENGTH('dbo.Patients', 'national_id') IS NULL ALTER TABLE dbo.Patients ADD national_id VARCHAR(20) NULL;
    IF COL_LENGTH('dbo.Patients', 'national_id_date') IS NULL ALTER TABLE dbo.Patients ADD national_id_date DATE NULL;
    IF COL_LENGTH('dbo.Patients', 'national_id_place') IS NULL ALTER TABLE dbo.Patients ADD national_id_place NVARCHAR(150) NULL;

    IF COL_LENGTH('dbo.AIAdviceHistory', 'risk_level') IS NULL ALTER TABLE dbo.AIAdviceHistory ADD risk_level VARCHAR(20) NULL;
    IF COL_LENGTH('dbo.AIAdviceHistory', 'recommendation_type') IS NULL ALTER TABLE dbo.AIAdviceHistory ADD recommendation_type VARCHAR(50) NULL;
    IF COL_LENGTH('dbo.AIAdviceHistory', 'source_data_reference') IS NULL ALTER TABLE dbo.AIAdviceHistory ADD source_data_reference NVARCHAR(MAX) NULL;
    IF COL_LENGTH('dbo.AIAdviceHistory', 'avg_glucose_7d') IS NULL ALTER TABLE dbo.AIAdviceHistory ADD avg_glucose_7d DECIMAL(5,2) NULL;
    IF COL_LENGTH('dbo.AIAdviceHistory', 'avg_systolic_7d') IS NULL ALTER TABLE dbo.AIAdviceHistory ADD avg_systolic_7d INT NULL;

    IF COL_LENGTH('dbo.PatientDailyLogs', 'heart_rate') IS NULL ALTER TABLE dbo.PatientDailyLogs ADD heart_rate INT NULL;
    IF COL_LENGTH('dbo.PatientDailyLogs', 'spo2') IS NULL ALTER TABLE dbo.PatientDailyLogs ADD spo2 DECIMAL(4,1) NULL;
    IF COL_LENGTH('dbo.PatientDailyLogs', 'meal_type') IS NULL ALTER TABLE dbo.PatientDailyLogs ADD meal_type VARCHAR(20) NULL;

    /* Existing legacy constraints made these fields mandatory, while current forms allow them empty. */
    ALTER TABLE dbo.Patients ALTER COLUMN date_of_birth DATE NULL;
    ALTER TABLE dbo.Patients ALTER COLUMN gender NVARCHAR(10) NULL;
    ALTER TABLE dbo.MedicalRecords ALTER COLUMN doctor_id INT NULL;

    /* ---------- New functional tables ---------- */
    IF OBJECT_ID(N'dbo.DeviceReadings', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.DeviceReadings (
            id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            patient_id INT NOT NULL, device_type VARCHAR(50) NOT NULL,
            raw_json_data NVARCHAR(MAX) NOT NULL,
            parsed_glucose DECIMAL(6,2) NULL, parsed_heart_rate INT NULL,
            parsed_systolic_bp INT NULL, parsed_diastolic_bp INT NULL,
            parsed_weight DECIMAL(5,2) NULL, parsed_spo2 DECIMAL(4,1) NULL,
            measured_at DATETIME NOT NULL CONSTRAINT DF_DeviceReadings_MeasuredAt DEFAULT GETDATE(),
            created_at DATETIME NOT NULL CONSTRAINT DF_DeviceReadings_CreatedAt DEFAULT GETDATE(),
            is_abnormal BIT NOT NULL CONSTRAINT DF_DeviceReadings_Abnormal DEFAULT 0,
            abnormal_note NVARCHAR(500) NULL,
            CONSTRAINT FK_DeviceReadings_Patients FOREIGN KEY (patient_id) REFERENCES dbo.Patients(patient_id)
        );
    END;

    IF OBJECT_ID(N'dbo.HealthAlerts', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.HealthAlerts (
            alert_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            patient_id INT NOT NULL, indicator_type VARCHAR(50) NOT NULL,
            value DECIMAL(8,2) NOT NULL, threshold DECIMAL(8,2) NOT NULL,
            alert_level VARCHAR(20) NOT NULL CONSTRAINT DF_HealthAlerts_Level DEFAULT 'MEDIUM',
            alert_message NVARCHAR(500) NOT NULL,
            data_source VARCHAR(20) NOT NULL CONSTRAINT DF_HealthAlerts_Source DEFAULT 'manual',
            source_record_id INT NULL,
            is_acknowledged BIT NOT NULL CONSTRAINT DF_HealthAlerts_Acknowledged DEFAULT 0,
            acknowledged_at DATETIME NULL,
            created_at DATETIME NOT NULL CONSTRAINT DF_HealthAlerts_CreatedAt DEFAULT GETDATE(),
            CONSTRAINT FK_HealthAlerts_Patients FOREIGN KEY (patient_id) REFERENCES dbo.Patients(patient_id)
        );
    END;

    IF OBJECT_ID(N'dbo.NextAppointment', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.NextAppointment (
            appointment_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            patient_id INT NOT NULL, appointment_date DATE NOT NULL,
            source VARCHAR(20) NOT NULL CONSTRAINT DF_NextAppointment_Source DEFAULT 'AUTO',
            created_at DATETIME NOT NULL CONSTRAINT DF_NextAppointment_CreatedAt DEFAULT GETDATE(),
            CONSTRAINT FK_NextAppointment_Patients FOREIGN KEY (patient_id) REFERENCES dbo.Patients(patient_id)
        );
    END;

    /* ---------- Normalize legacy codes to values used by Java/JSP ---------- */
    UPDATE dbo.Users SET role = UPPER(LTRIM(RTRIM(role))) WHERE role IS NOT NULL;
    UPDATE dbo.Users SET status = 'ACTIVE'
      WHERE status IS NULL OR UPPER(LTRIM(RTRIM(status))) IN ('ACTIVE', N'HOẠT ĐỘNG', 'HOAT DONG');
    UPDATE dbo.Users SET status = 'INACTIVE'
      WHERE UPPER(LTRIM(RTRIM(status))) IN ('INACTIVE', N'KHÓA', N'BỊ KHÓA', 'LOCKED', 'DISABLED');

    UPDATE dbo.MedicalRecords SET status = 'COMPLETED'
      WHERE status IS NULL OR UPPER(LTRIM(RTRIM(status))) IN ('COMPLETED', N'HOÀN THÀNH', 'HOAN THANH');
    UPDATE dbo.MedicalRecords SET status = 'DRAFT'
      WHERE UPPER(LTRIM(RTRIM(status))) IN ('DRAFT', N'CHỜ KHÁM', N'ĐANG KHÁM', 'CHO KHAM', 'DANG KHAM');

    UPDATE dbo.AIWarnings SET risk_level = 'HIGH'
      WHERE UPPER(LTRIM(RTRIM(risk_level))) IN ('HIGH', N'CAO');
    UPDATE dbo.AIWarnings SET risk_level = 'MEDIUM'
      WHERE UPPER(LTRIM(RTRIM(risk_level))) IN ('MEDIUM', N'TRUNG BÌNH', 'TRUNG BINH');
    UPDATE dbo.AIWarnings SET risk_level = 'LOW'
      WHERE UPPER(LTRIM(RTRIM(risk_level))) IN ('LOW', N'THẤP', 'THAP');

    /* ---------- Performance and duplicate-prevention indexes ---------- */
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.Patients') AND name='IX_Patients_CreatedAt')
        CREATE INDEX IX_Patients_CreatedAt ON dbo.Patients(created_at DESC);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.MedicalRecords') AND name='IX_MR_PatientId')
        CREATE INDEX IX_MR_PatientId ON dbo.MedicalRecords(patient_id, visit_date DESC);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.MedicalRecords') AND name='IX_MR_DoctorStatus')
        CREATE INDEX IX_MR_DoctorStatus ON dbo.MedicalRecords(doctor_id, status, visit_date DESC);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.HealthIndicators') AND name='IX_HI_RecordId')
        CREATE INDEX IX_HI_RecordId ON dbo.HealthIndicators(record_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.PatientDailyLogs') AND name='IX_PDL_PatientDate')
        CREATE INDEX IX_PDL_PatientDate ON dbo.PatientDailyLogs(patient_id, log_date DESC);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.AIAdviceHistory') AND name='IX_AAH_PatientDate')
        CREATE INDEX IX_AAH_PatientDate ON dbo.AIAdviceHistory(patient_id, advice_date DESC);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.DeviceReadings') AND name='IX_DR_PatientMeasured')
        CREATE INDEX IX_DR_PatientMeasured ON dbo.DeviceReadings(patient_id, measured_at DESC);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.HealthAlerts') AND name='IX_HA_PatientAck')
        CREATE INDEX IX_HA_PatientAck ON dbo.HealthAlerts(patient_id, is_acknowledged, created_at DESC);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.NextAppointment') AND name='IX_NA_PatientDate')
        CREATE INDEX IX_NA_PatientDate ON dbo.NextAppointment(patient_id, appointment_date);

    COMMIT TRANSACTION;
    PRINT 'Legacy database integration completed successfully.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/* Verification: every value must be 1. */
SELECT required_object,
       CASE required_object
         WHEN 'Users' THEN IIF(OBJECT_ID('dbo.Users','U') IS NOT NULL,1,0)
         WHEN 'Doctors' THEN IIF(OBJECT_ID('dbo.Doctors','U') IS NOT NULL,1,0)
         WHEN 'Patients' THEN IIF(OBJECT_ID('dbo.Patients','U') IS NOT NULL,1,0)
         WHEN 'MedicalRecords' THEN IIF(OBJECT_ID('dbo.MedicalRecords','U') IS NOT NULL,1,0)
         WHEN 'HealthIndicators' THEN IIF(OBJECT_ID('dbo.HealthIndicators','U') IS NOT NULL,1,0)
         WHEN 'AIWarnings' THEN IIF(OBJECT_ID('dbo.AIWarnings','U') IS NOT NULL,1,0)
         WHEN 'AIAdviceHistory' THEN IIF(OBJECT_ID('dbo.AIAdviceHistory','U') IS NOT NULL,1,0)
         WHEN 'PatientDailyLogs' THEN IIF(OBJECT_ID('dbo.PatientDailyLogs','U') IS NOT NULL,1,0)
         WHEN 'DeviceReadings' THEN IIF(OBJECT_ID('dbo.DeviceReadings','U') IS NOT NULL,1,0)
         WHEN 'HealthAlerts' THEN IIF(OBJECT_ID('dbo.HealthAlerts','U') IS NOT NULL,1,0)
         WHEN 'NextAppointment' THEN IIF(OBJECT_ID('dbo.NextAppointment','U') IS NOT NULL,1,0)
       END AS is_ready
FROM (VALUES ('Users'),('Doctors'),('Patients'),('MedicalRecords'),('HealthIndicators'),
             ('AIWarnings'),('AIAdviceHistory'),('PatientDailyLogs'),('DeviceReadings'),
             ('HealthAlerts'),('NextAppointment')) v(required_object);

SELECT v.table_name, v.column_name, IIF(c.column_id IS NULL, 0, 1) AS is_ready
FROM (VALUES
    ('Users','cccd'),
    ('Doctors','face_image_path'),('Doctors','cccd_image_path'),('Doctors','license_image_path'),
    ('Patients','national_id'),('Patients','national_id_date'),('Patients','national_id_place'),
    ('AIAdviceHistory','risk_level'),('AIAdviceHistory','recommendation_type'),
    ('AIAdviceHistory','source_data_reference'),('AIAdviceHistory','avg_glucose_7d'),
    ('AIAdviceHistory','avg_systolic_7d'),('PatientDailyLogs','heart_rate'),
    ('PatientDailyLogs','spo2'),('PatientDailyLogs','meal_type')
) v(table_name,column_name)
LEFT JOIN sys.columns c ON c.object_id=OBJECT_ID('dbo.' + v.table_name) AND c.name=v.column_name
ORDER BY v.table_name, v.column_name;
GO
