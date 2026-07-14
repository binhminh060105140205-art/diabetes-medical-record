-- ============================================================
--  MIGRATION SQL — Chạy file này để cập nhật database
--  DiabetesMedicalRecordDB
-- ============================================================
USE DiabetesMedicalRecordDB;
GO

-- 1. Thêm cột symptoms vào PatientDailyLogs nếu chưa có
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME='PatientDailyLogs' AND COLUMN_NAME='symptoms'
)
BEGIN
    ALTER TABLE PatientDailyLogs ADD symptoms NVARCHAR(500) NULL;
    PRINT 'Added symptoms column to PatientDailyLogs';
END

-- 2. Tạo bảng PatientDailyLogs nếu chưa có
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='PatientDailyLogs')
BEGIN
    CREATE TABLE PatientDailyLogs (
        log_id        INT IDENTITY(1,1) PRIMARY KEY,
        patient_id    INT NOT NULL,
        log_date      DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
        blood_glucose FLOAT          NULL,
        systolic_bp   INT            NULL,
        diastolic_bp  INT            NULL,
        weight        FLOAT          NULL,
        symptoms      NVARCHAR(500)  NULL,
        note          NVARCHAR(500)  NULL,
        created_at    DATETIME       DEFAULT GETDATE(),
        FOREIGN KEY (patient_id) REFERENCES Patients(patient_id)
    );
    PRINT 'Created PatientDailyLogs table';
END

-- 3. Tạo bảng AIAdviceHistory nếu chưa có
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='AIAdviceHistory')
BEGIN
    CREATE TABLE AIAdviceHistory (
        advice_id      INT IDENTITY(1,1) PRIMARY KEY,
        patient_id     INT NOT NULL,
        advice_date    DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
        advice_content NVARCHAR(MAX)  NULL,
        created_at     DATETIME       DEFAULT GETDATE(),
        FOREIGN KEY (patient_id) REFERENCES Patients(patient_id)
    );
    PRINT 'Created AIAdviceHistory table';
END

-- 4. Tách HealthIndicators: thêm entered_by_doctor
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME='HealthIndicators' AND COLUMN_NAME='entered_by_doctor'
)
BEGIN
    ALTER TABLE HealthIndicators ADD entered_by_doctor INT NULL;
    PRINT 'Added entered_by_doctor column to HealthIndicators';
END

-- 5. Thêm national_id fields vào Patients nếu chưa có
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME='Patients' AND COLUMN_NAME='national_id'
)
BEGIN
    ALTER TABLE Patients ADD national_id VARCHAR(20) NULL;
    ALTER TABLE Patients ADD national_id_date DATE NULL;
    ALTER TABLE Patients ADD national_id_place NVARCHAR(200) NULL;
    PRINT 'Added national_id fields to Patients';
END

PRINT '=== Migration completed successfully ===';
GO
