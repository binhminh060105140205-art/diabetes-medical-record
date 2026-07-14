USE [DiabetesMedicalRecordDB];
GO

-- Run once after full-schema-and-seed.sql. Safe to run again.
IF OBJECT_ID(N'dbo.DeviceReadings', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DeviceReadings (
        id INT IDENTITY(1,1) PRIMARY KEY,
        patient_id INT NOT NULL,
        device_type VARCHAR(50) NOT NULL,
        raw_json_data NVARCHAR(MAX) NOT NULL,
        parsed_glucose DECIMAL(6,2) NULL,
        parsed_heart_rate INT NULL,
        parsed_systolic_bp INT NULL,
        parsed_diastolic_bp INT NULL,
        parsed_weight DECIMAL(5,2) NULL,
        parsed_spo2 DECIMAL(4,1) NULL,
        measured_at DATETIME NOT NULL CONSTRAINT DF_DeviceReadings_MeasuredAt DEFAULT GETDATE(),
        created_at DATETIME NOT NULL CONSTRAINT DF_DeviceReadings_CreatedAt DEFAULT GETDATE(),
        is_abnormal BIT NOT NULL CONSTRAINT DF_DeviceReadings_Abnormal DEFAULT 0,
        abnormal_note NVARCHAR(500) NULL,
        CONSTRAINT FK_DeviceReadings_Patients FOREIGN KEY (patient_id) REFERENCES dbo.Patients(patient_id)
    );
    CREATE INDEX IX_DR_PatientId ON dbo.DeviceReadings(patient_id);
    CREATE INDEX IX_DR_MeasuredAt ON dbo.DeviceReadings(measured_at);
END
GO

IF OBJECT_ID(N'dbo.HealthAlerts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.HealthAlerts (
        alert_id INT IDENTITY(1,1) PRIMARY KEY,
        patient_id INT NOT NULL,
        indicator_type VARCHAR(50) NOT NULL,
        value DECIMAL(8,2) NOT NULL,
        threshold DECIMAL(8,2) NOT NULL,
        alert_level VARCHAR(20) NOT NULL CONSTRAINT DF_HealthAlerts_Level DEFAULT 'medium',
        alert_message NVARCHAR(500) NOT NULL,
        data_source VARCHAR(20) NOT NULL CONSTRAINT DF_HealthAlerts_Source DEFAULT 'manual',
        source_record_id INT NULL,
        is_acknowledged BIT NOT NULL CONSTRAINT DF_HealthAlerts_Acknowledged DEFAULT 0,
        acknowledged_at DATETIME NULL,
        created_at DATETIME NOT NULL CONSTRAINT DF_HealthAlerts_CreatedAt DEFAULT GETDATE(),
        CONSTRAINT FK_HealthAlerts_Patients FOREIGN KEY (patient_id) REFERENCES dbo.Patients(patient_id)
    );
    CREATE INDEX IX_HA_PatientId ON dbo.HealthAlerts(patient_id);
    CREATE INDEX IX_HA_CreatedAt ON dbo.HealthAlerts(created_at);
END
GO

IF OBJECT_ID(N'dbo.NextAppointment', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.NextAppointment (
        appointment_id INT IDENTITY(1,1) PRIMARY KEY,
        patient_id INT NOT NULL,
        appointment_date DATE NOT NULL,
        source VARCHAR(20) NOT NULL CONSTRAINT DF_NextAppointment_Source DEFAULT 'AUTO',
        created_at DATETIME NOT NULL CONSTRAINT DF_NextAppointment_CreatedAt DEFAULT GETDATE(),
        CONSTRAINT FK_NextAppointment_Patients FOREIGN KEY (patient_id) REFERENCES dbo.Patients(patient_id)
    );
    CREATE INDEX IX_NextAppointment_PatientDate ON dbo.NextAppointment(patient_id, appointment_date);
END
GO

IF COL_LENGTH('dbo.AIAdviceHistory', 'risk_level') IS NULL
    ALTER TABLE dbo.AIAdviceHistory ADD risk_level VARCHAR(20) NULL;
IF COL_LENGTH('dbo.AIAdviceHistory', 'recommendation_type') IS NULL
    ALTER TABLE dbo.AIAdviceHistory ADD recommendation_type VARCHAR(50) NULL;
IF COL_LENGTH('dbo.AIAdviceHistory', 'source_data_reference') IS NULL
    ALTER TABLE dbo.AIAdviceHistory ADD source_data_reference NVARCHAR(MAX) NULL;
IF COL_LENGTH('dbo.AIAdviceHistory', 'avg_glucose_7d') IS NULL
    ALTER TABLE dbo.AIAdviceHistory ADD avg_glucose_7d DECIMAL(5,2) NULL;
IF COL_LENGTH('dbo.AIAdviceHistory', 'avg_systolic_7d') IS NULL
    ALTER TABLE dbo.AIAdviceHistory ADD avg_systolic_7d INT NULL;
IF COL_LENGTH('dbo.PatientDailyLogs', 'heart_rate') IS NULL
    ALTER TABLE dbo.PatientDailyLogs ADD heart_rate INT NULL;
IF COL_LENGTH('dbo.PatientDailyLogs', 'spo2') IS NULL
    ALTER TABLE dbo.PatientDailyLogs ADD spo2 DECIMAL(4,1) NULL;
IF COL_LENGTH('dbo.PatientDailyLogs', 'meal_type') IS NULL
    ALTER TABLE dbo.PatientDailyLogs ADD meal_type VARCHAR(20) NULL;
GO
