CREATE DATABASE DiabetesMedicalRecordDB;
GO
USE DiabetesMedicalRecordDB;
GO

-- 1. B?NG USERS
CREATE TABLE Users (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    phone VARCHAR(15),
    role VARCHAR(20) NOT NULL, -- 'admin', 'staff', 'doctor', 'patient'
    status NVARCHAR(20) DEFAULT 'ACTIVE',
    email VARCHAR(100),
    dob DATE,
    gender NVARCHAR(10),
    address NVARCHAR(255),
    created_at DATETIME DEFAULT GETDATE()
);

-- 2. B?NG DOCTORS
CREATE TABLE Doctors (
    doctor_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    specialty NVARCHAR(100) NOT NULL,
    license_no VARCHAR(50) NOT NULL UNIQUE,
    license_issue_date DATE,
    license_expire_date DATE,
    license_issued_by NVARCHAR(150),
    degree NVARCHAR(50),
    consultation_fee DECIMAL(18,2),
    CONSTRAINT FK_Doctors_Users FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

-- 3. B?NG PATIENTS
CREATE TABLE Patients (
    patient_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender NVARCHAR(10) NOT NULL,
    phone VARCHAR(15),
    address NVARCHAR(255),
    health_insurance_no VARCHAR(20),
    created_by INT,
    created_at DATETIME DEFAULT GETDATE(),
    national_id VARCHAR(20),
    national_id_date DATE,
    national_id_place NVARCHAR(150),
    CONSTRAINT FK_Patients_Users FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

-- 4. B?NG MEDICALRECORDS
CREATE TABLE MedicalRecords (
    record_id INT IDENTITY(1,1) PRIMARY KEY,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    created_by_staff INT,
    visit_date DATETIME DEFAULT GETDATE(),
    reason_for_visit NVARCHAR(255),
    symptoms NVARCHAR(MAX),
    medical_history NVARCHAR(MAX),
    lifestyle_habits NVARCHAR(MAX),
    clinical_exam NVARCHAR(MAX),
    complication_note NVARCHAR(MAX),
    final_diagnosis NVARCHAR(255),
    treatment_plan NVARCHAR(MAX),
    prescription_note NVARCHAR(MAX),
    advice NVARCHAR(MAX),
    follow_up_date DATE,
    doctor_note NVARCHAR(MAX),
    status NVARCHAR(50) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Records_Patients FOREIGN KEY (patient_id) REFERENCES Patients(patient_id),
    CONSTRAINT FK_Records_Doctors FOREIGN KEY (doctor_id) REFERENCES Doctors(doctor_id)
);

-- 5. B?NG HEALTHINDICATORS
CREATE TABLE HealthIndicators (
    indicator_id INT IDENTITY(1,1) PRIMARY KEY,
    record_id INT NOT NULL,
    entered_by_staff INT,
    height DECIMAL(5,2),
    weight DECIMAL(5,2),
    bmi DECIMAL(4,2),
    systolic_bp INT,
    diastolic_bp INT,
    heart_rate INT,
    temperature DECIMAL(4,1),
    blood_glucose DECIMAL(5,2),
    hba1c DECIMAL(4,2),
    cholesterol DECIMAL(5,2),
    triglyceride DECIMAL(5,2),
    hdl_c DECIMAL(5,2),
    ldl_c DECIMAL(5,2),
    measured_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Indicators_Records FOREIGN KEY (record_id) REFERENCES MedicalRecords(record_id)
);

-- 6. B?NG AIWARNINGS
CREATE TABLE AIWarnings (
    warning_id INT IDENTITY(1,1) PRIMARY KEY,
    record_id INT NOT NULL,
    risk_level NVARCHAR(50), -- 'Th?p', 'Trung bình', 'Cao'
    warning_message NVARCHAR(MAX),
    suggested_action NVARCHAR(MAX),
    ai_score DECIMAL(5,2),
    generated_at DATETIME DEFAULT GETDATE(),
    reviewed_by_doctor INT,
    CONSTRAINT FK_Warnings_Records FOREIGN KEY (record_id) REFERENCES MedicalRecords(record_id)
);

-- 7. B?NG AIADVICEHISTORY
CREATE TABLE AIAdviceHistory (
    advice_id INT IDENTITY(1,1) PRIMARY KEY,
    patient_id INT NOT NULL,
    advice_date DATETIME DEFAULT GETDATE(),
    advice_content NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_AIAdvice_Patients FOREIGN KEY (patient_id) REFERENCES Patients(patient_id)
);

-- 8. B?NG PATIENTDAILYLOGS
CREATE TABLE PatientDailyLogs (
    log_id INT IDENTITY(1,1) PRIMARY KEY,
    patient_id INT NOT NULL,
    log_date DATE DEFAULT CAST(GETDATE() AS DATE),
    blood_glucose DECIMAL(5,2),
    systolic_bp INT,
    diastolic_bp INT,
    weight DECIMAL(5,2),
    note NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    symptoms NVARCHAR(MAX),
    CONSTRAINT FK_Logs_Patients FOREIGN KEY (patient_id) REFERENCES Patients(patient_id)
);
GO

-- ==========================================
-- I. CHÈN D? LI?U B?NG USERS (M?t kh?u: 123456)
-- ==========================================
INSERT INTO Users (username, password, full_name, phone, role, status, email) VALUES 
('admin1', '123456', N'Nguy?n Qu?n Tr? 1', '0901111111', 'admin', 'ACTIVE', 'admin1@gmail.com'),
('admin2', '123456', N'Tr?n Qu?n Tr? 2', '0901111112', 'admin', 'ACTIVE', 'admin2@gmail.com');

INSERT INTO Users (username, password, full_name, phone, role, status, email) VALUES 
('staff1', '123456', N'Lê Ti?p Tân 1', '0902222221', 'staff', 'ACTIVE', 'staff1@gmail.com'),
('staff2', '123456', N'Ph?m Ti?p Tân 2', '0902222222', 'staff', 'ACTIVE', 'staff2@gmail.com'),
('staff3', '123456', N'Hoàng Ti?p Tân 3', '0902222223', 'staff', 'ACTIVE', 'staff3@gmail.com');

INSERT INTO Users (username, password, full_name, phone, role, status, email) VALUES
('bacsi1', '123456', N'BS. Nguy?n V?n An', '0903333331', 'doctor', 'ACTIVE', 'an.nv@gmail.com'),
('bacsi2', '123456', N'BS. Tr?n Th? Bình', '0903333332', 'doctor', 'ACTIVE', 'binh.tt@gmail.com'),
('bacsi3', '123456', N'BS. Lê Hoàng C??ng', '0903333333', 'doctor', 'ACTIVE', 'cuong.lh@gmail.com'),
('bacsi4', '123456', N'BS. Ph?m Minh ??c', '0903333334', 'doctor', 'ACTIVE', 'duc.pm@gmail.com'),
('bacsi5', '123456', N'BS. ?? H?ng H?nh', '0903333335', 'doctor', 'ACTIVE', 'hanh.dh@gmail.com');

DECLARE @i INT = 1;
WHILE @i <= 30
BEGIN
    INSERT INTO Users (username, password, full_name, phone, role, status, email, dob, gender, address)
    VALUES (
        'benhnhan' + CAST(@i AS VARCHAR), 
        '123456', 
        N'B?nh Nhân Th? ' + CAST(@i AS NVARCHAR), 
        '0904' + RIGHT('000000' + CAST(@i AS VARCHAR), 6), 
        'patient', 
        'ACTIVE',
        'patient' + CAST(@i AS VARCHAR) + '@gmail.com',
        DATEADD(year, CAST(-20 - (@i * 1.5) AS INT), GETDATE()),
        CASE WHEN @i % 2 = 0 THEN N'Nam' ELSE N'N?' END,
        N'S? ' + CAST(@i AS NVARCHAR) + N' ???ng CMT8, Qu?n 3, TP.HCM'
    );
    SET @i = @i + 1;
END;

-- ==========================================
-- II. CHÈN D? LI?U B?NG DOCTORS
-- ==========================================
INSERT INTO Doctors (user_id, specialty, license_no, license_issue_date, license_expire_date, license_issued_by, degree, consultation_fee) VALUES 
(6, N'N?i ti?t - ?ái tháo ???ng', 'CC hành ngh? ??t chu?n 01', '2015-01-10', '2030-01-10', N'S? Y T? Hà N?i', N'Th?c s?', 200000.00),
(7, N'N?i t?ng h?p', 'CC hành ngh? ??t chu?n 02', '2016-05-12', '2031-05-12', N'S? Y T? TP.HCM', N'Bác s? Chuyên khoa I', 150000.00),
(8, N'Tim m?ch - Bi?n ch?ng n?i ti?t', 'CC hành ngh? ??t chu?n 03', '2012-08-20', '2027-08-20', N'B? Y T?', N'Ti?n s?', 300000.00),
(9, N'Dinh d??ng lâm sàng', 'CC hành ngh? ??t chu?n 04', '2018-11-01', '2033-11-01', N'S? Y T? ?à N?ng', N'Bác s?', 120000.00),
(10, N'N?i ti?t - ?ái tháo ???ng', 'CC hành ngh? ??t chu?n 05', '2020-03-15', '2035-03-15', N'S? Y T? C?n Th?', N'Th?c s?', 200000.00);

-- ==========================================
-- III. CHÈN D? LI?U B?NG PATIENTS
-- ==========================================
SET @i = 1;
WHILE @i <= 30
BEGIN
    INSERT INTO Patients (user_id, full_name, date_of_birth, gender, phone, address, health_insurance_no, created_by, national_id, national_id_date, national_id_place)
    SELECT 
        user_id, full_name, dob, gender, phone, address, 
        'GD479' + RIGHT('0000000' + CAST(@i AS VARCHAR), 7),
        3, 
        '03109300' + RIGHT('0000' + CAST(@i AS VARCHAR), 4),
        '2021-05-20',
        N'C?c C?nh sát QLHC v? tr?t t? xã h?i'
    FROM Users WHERE username = 'benhnhan' + CAST(@i AS VARCHAR);
    
    SET @i = @i + 1;
END;

-- ==========================================
-- IV. T?O D? LI?U L?CH S? KHÁM & CH? S? Y T? M?U
-- ==========================================
INSERT INTO MedicalRecords (patient_id, doctor_id, created_by_staff, visit_date, reason_for_visit, symptoms, final_diagnosis, treatment_plan, status) VALUES 
(1, 1, 3, '2026-05-10', N'Tái khám ??nh k?', N'Khát n??c nhi?u, ti?u ?êm', N'?ái tháo ???ng Type 2', N'Dùng Metformin 850mg ngày 2 viên', N'Hoàn thành'),
(2, 2, 3, '2026-05-15', N'Khám sàng l?c', N'M?t m?i sút cân không rõ nguyên nhân', N'Ti?n ?ái tháo ???ng', N'Thay ??i ch? ?? ?n và t?ng c??ng v?n ??ng', N'Hoàn thành'),
(3, 3, 4, '2026-06-01', N'Ki?m tra ???ng huy?t', N'Tê bì chân tay nh?', N'?ái tháo ???ng Type 2 - Có bi?n ch?ng th?n kinh ngo?i vi', N'Tiêm Insulin ph?i h?p thu?c u?ng', N'Hoàn thành'),
(4, 1, 4, '2026-06-10', N'Khám ??nh k?', N'Không có tri?u ch?ng l?', N'?ái tháo ???ng Type 2 ?n ??nh', N'Duy trì li?u c?', N'Hoàn thành'),
(5, 5, 5, '2026-06-20', N'Chóng m?t, m?t m?i', N'?au ??u, vã m? hôi', N'?ái tháo ???ng kèm T?ng huy?t áp', N'B? sung thu?c huy?t áp Amlodipine 5mg', N'Hoàn thành');

INSERT INTO HealthIndicators (record_id, entered_by_staff, height, weight, bmi, systolic_bp, diastolic_bp, heart_rate, blood_glucose, hba1c, cholesterol) VALUES 
(1, 3, 165.0, 70.5, 25.9, 130, 80, 75, 8.5, 7.2, 5.2),
(2, 3, 155.0, 52.0, 21.6, 115, 75, 72, 6.1, 6.0, 4.5),
(3, 4, 170.0, 80.0, 27.7, 140, 90, 82, 12.4, 8.5, 6.1),
(4, 4, 160.0, 58.0, 22.7, 120, 80, 70, 6.8, 6.4, 5.0),
(5, 5, 162.0, 65.0, 24.8, 150, 95, 88, 9.0, 7.5, 5.7);

INSERT INTO AIWarnings (record_id, risk_level, warning_message, suggested_action, ai_score, reviewed_by_doctor) VALUES 
(3, N'Cao', N'???ng huy?t (12.4) và HbA1c (8.5%) ?ang ? m?c báo ??ng nguy hi?m, kèm huy?t áp cao.', N'Yêu c?u bác s? ch? ??nh phác ?? tiêm Insulin kh?n c?p và rà soát bi?n ch?ng th?n.', 88.5, 3);

INSERT INTO PatientDailyLogs (patient_id, log_date, blood_glucose, systolic_bp, diastolic_bp, weight, symptoms) VALUES 
(1, '2026-06-21', 7.2, 125, 78, 70.2, N'Bình th??ng'),
(1, '2026-06-22', 6.9, 122, 80, 70.1, N'C? th? kh?e m?nh'),
(1, '2026-06-23', 8.1, 132, 82, 70.4, N'H?i ?au ??u sau ?n nhi?u tinh b?t');
GO
