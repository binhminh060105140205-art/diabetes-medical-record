-- ============================================================
--  UPGRADE V3 — DiabetesMedicalRecordDB
--  Chạy script này trên SQL Server để nâng cấp từ V2 lên V3.
--  KHÔNG xóa dữ liệu cũ — chỉ thêm và sửa cấu trúc.
-- ============================================================
USE DiabetesMedicalRecordDB;
GO

PRINT '=== BẮT ĐẦU NÂNG CẤP DATABASE V3 ===';

-- ============================================================
-- BƯỚC 1: BACKUP và XÓA bảng AIWarnings (Doctor AI removed)
-- ============================================================
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='AIWarnings')
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='AIWarnings_Archive')
    BEGIN
        SELECT * INTO AIWarnings_Archive FROM AIWarnings;
        PRINT 'Đã backup AIWarnings → AIWarnings_Archive';
    END
    DROP TABLE AIWarnings;
    PRINT 'Đã xóa bảng AIWarnings';
END
GO

-- ============================================================
-- BƯỚC 2: TẠO bảng DeviceReadings
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='DeviceReadings')
BEGIN
    CREATE TABLE DeviceReadings (
        id                  INT IDENTITY(1,1) PRIMARY KEY,
        patient_id          INT NOT NULL,
        device_type         VARCHAR(50) NOT NULL,        -- 'glucometer','smartwatch','bp_monitor','scale'
        raw_json_data       NVARCHAR(MAX) NOT NULL,      -- JSON gốc từ thiết bị
        parsed_glucose      DECIMAL(6,2) NULL,           -- mg/dL
        parsed_heart_rate   INT NULL,                    -- bpm
        parsed_systolic_bp  INT NULL,                    -- mmHg
        parsed_diastolic_bp INT NULL,                    -- mmHg
        parsed_weight       DECIMAL(5,2) NULL,           -- kg
        parsed_spo2         DECIMAL(4,1) NULL,           -- %
        measured_at         DATETIME NOT NULL DEFAULT GETDATE(),
        created_at          DATETIME NOT NULL DEFAULT GETDATE(),
        is_abnormal         BIT NOT NULL DEFAULT 0,
        abnormal_note       NVARCHAR(500) NULL,
        CONSTRAINT FK_DeviceReadings_Patients FOREIGN KEY (patient_id) REFERENCES Patients(patient_id)
    );
    CREATE INDEX IX_DR_PatientId  ON DeviceReadings(patient_id);
    CREATE INDEX IX_DR_MeasuredAt ON DeviceReadings(measured_at);
    CREATE INDEX IX_DR_DeviceType ON DeviceReadings(device_type);
    PRINT 'Đã tạo bảng DeviceReadings';
END
GO

-- ============================================================
-- BƯỚC 3: TẠO bảng HealthAlerts
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='HealthAlerts')
BEGIN
    CREATE TABLE HealthAlerts (
        alert_id         INT IDENTITY(1,1) PRIMARY KEY,
        patient_id       INT NOT NULL,
        indicator_type   VARCHAR(50) NOT NULL,    -- 'glucose','blood_pressure','heart_rate','spo2'
        value            DECIMAL(8,2) NOT NULL,   -- Giá trị gây cảnh báo
        threshold        DECIMAL(8,2) NOT NULL,   -- Ngưỡng đã bị vượt
        alert_level      VARCHAR(20) NOT NULL DEFAULT 'medium',  -- 'low','medium','high'
        alert_message    NVARCHAR(500) NOT NULL,
        data_source      VARCHAR(20) NOT NULL DEFAULT 'manual',  -- 'device','manual'
        source_record_id INT NULL,
        is_acknowledged  BIT NOT NULL DEFAULT 0,
        acknowledged_at  DATETIME NULL,
        created_at       DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_HealthAlerts_Patients FOREIGN KEY (patient_id) REFERENCES Patients(patient_id)
    );
    CREATE INDEX IX_HA_PatientId ON HealthAlerts(patient_id);
    CREATE INDEX IX_HA_CreatedAt ON HealthAlerts(created_at);
    CREATE INDEX IX_HA_Level     ON HealthAlerts(alert_level);
    PRINT 'Đã tạo bảng HealthAlerts';
END
GO

-- ============================================================
-- BƯỚC 4: MỞ RỘNG AIAdviceHistory
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='AIAdviceHistory' AND COLUMN_NAME='risk_level')
BEGIN
    ALTER TABLE AIAdviceHistory ADD risk_level VARCHAR(20) NULL;
    PRINT 'Added risk_level to AIAdviceHistory';
END
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='AIAdviceHistory' AND COLUMN_NAME='recommendation_type')
BEGIN
    ALTER TABLE AIAdviceHistory ADD recommendation_type VARCHAR(50) NULL;
    PRINT 'Added recommendation_type to AIAdviceHistory';
END
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='AIAdviceHistory' AND COLUMN_NAME='source_data_reference')
BEGIN
    ALTER TABLE AIAdviceHistory ADD source_data_reference NVARCHAR(MAX) NULL;
    PRINT 'Added source_data_reference to AIAdviceHistory';
END
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='AIAdviceHistory' AND COLUMN_NAME='avg_glucose_7d')
BEGIN
    ALTER TABLE AIAdviceHistory ADD avg_glucose_7d DECIMAL(5,2) NULL;
    PRINT 'Added avg_glucose_7d to AIAdviceHistory';
END
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='AIAdviceHistory' AND COLUMN_NAME='avg_systolic_7d')
BEGIN
    ALTER TABLE AIAdviceHistory ADD avg_systolic_7d INT NULL;
    PRINT 'Added avg_systolic_7d to AIAdviceHistory';
END
GO

-- ============================================================
-- BƯỚC 5: MỞ RỘNG PatientDailyLogs
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='PatientDailyLogs' AND COLUMN_NAME='heart_rate')
BEGIN
    ALTER TABLE PatientDailyLogs ADD heart_rate INT NULL;
    PRINT 'Added heart_rate to PatientDailyLogs';
END
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='PatientDailyLogs' AND COLUMN_NAME='spo2')
BEGIN
    ALTER TABLE PatientDailyLogs ADD spo2 DECIMAL(4,1) NULL;
    PRINT 'Added spo2 to PatientDailyLogs';
END
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='PatientDailyLogs' AND COLUMN_NAME='meal_type')
BEGIN
    ALTER TABLE PatientDailyLogs ADD meal_type VARCHAR(20) NULL;
    PRINT 'Added meal_type to PatientDailyLogs';
END
GO

-- ============================================================
-- BƯỚC 6: THÊM INDEXES HIỆU NĂNG
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_PDL_PatientId')
    CREATE INDEX IX_PDL_PatientId ON PatientDailyLogs(patient_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_PDL_LogDate')
    CREATE INDEX IX_PDL_LogDate ON PatientDailyLogs(log_date);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_MR_PatientId')
    CREATE INDEX IX_MR_PatientId ON MedicalRecords(patient_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_MR_DoctorId')
    CREATE INDEX IX_MR_DoctorId ON MedicalRecords(doctor_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_HI_RecordId')
    CREATE INDEX IX_HI_RecordId ON HealthIndicators(record_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_AAH_PatientId')
    CREATE INDEX IX_AAH_PatientId ON AIAdviceHistory(patient_id);
PRINT 'Đã thêm indexes hiệu năng';
GO

-- ============================================================
-- BƯỚC 7: DỮ LIỆU MẪU (chỉ chạy nếu chưa có)
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM DeviceReadings WHERE patient_id=1)
BEGIN
    INSERT INTO DeviceReadings(patient_id,device_type,raw_json_data,parsed_glucose,measured_at,is_abnormal,abnormal_note)
    VALUES(1,'glucometer',
        N'{"device_type":"glucometer","patient_id":1,"glucose_mgdl":185.0,"timestamp":"2026-06-25T08:15:00"}',
        185.0,'2026-06-25 08:15:00',1,N'Đường huyết >= 180 mg/dL');

    INSERT INTO DeviceReadings(patient_id,device_type,raw_json_data,parsed_heart_rate,parsed_spo2,measured_at,is_abnormal)
    VALUES(1,'smartwatch',
        N'{"device_type":"smartwatch","patient_id":1,"heart_rate":88,"spo2":97.5,"timestamp":"2026-06-25T10:00:00"}',
        88,97.5,'2026-06-25 10:00:00',0);

    INSERT INTO HealthAlerts(patient_id,indicator_type,value,threshold,alert_level,alert_message,data_source)
    VALUES(1,'glucose',185.0,180.0,'medium',N'⚠️ [glucometer] Đường huyết cao: 185.0 mg/dL (≥ 180).','device');

    PRINT 'Đã thêm dữ liệu mẫu DeviceReadings + HealthAlerts';
END
GO

PRINT '=== NÂNG CẤP DATABASE V3 HOÀN THÀNH ===';
GO
