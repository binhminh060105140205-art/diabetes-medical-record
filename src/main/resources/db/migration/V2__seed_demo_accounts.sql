-- Demo accounts for acceptance testing. Passwords are stored only as BCrypt hashes.

-- 10 staff accounts: staff01 ... staff10
WITH seed(n, full_name, address) AS (
  VALUES
    (1, 'Nguyễn Thị Minh Anh', '12 Phố Huế, Hai Bà Trưng, Hà Nội'),
    (2, 'Trần Đức Hoàng', '28 Nguyễn Trãi, Thanh Xuân, Hà Nội'),
    (3, 'Lê Thu Hương', '45 Cầu Giấy, Cầu Giấy, Hà Nội'),
    (4, 'Phạm Quang Minh', '16 Lạc Long Quân, Tây Hồ, Hà Nội'),
    (5, 'Hoàng Ngọc Lan', '72 Nguyễn Văn Cừ, Long Biên, Hà Nội'),
    (6, 'Vũ Thành Nam', '31 Xã Đàn, Đống Đa, Hà Nội'),
    (7, 'Đặng Mai Phương', '19 Trần Duy Hưng, Cầu Giấy, Hà Nội'),
    (8, 'Bùi Quốc Khánh', '55 Quang Trung, Hà Đông, Hà Nội'),
    (9, 'Đỗ Thị Thanh Hà', '24 Minh Khai, Hai Bà Trưng, Hà Nội'),
    (10, 'Ngô Anh Tuấn', '63 Hoàng Quốc Việt, Bắc Từ Liêm, Hà Nội')
)
INSERT INTO users(username, password, full_name, phone, role, status, email, dob, gender, address, cccd)
SELECT 'staff' || to_char(n, 'FM00'),
       '$2a$12$JekzFIsJjxI3mD2AEk2qOuEOXINeURXGaX7ke66m/frI3/A6d2eee',
       full_name, '0912' || to_char(300000 + n, 'FM000000'), 'STAFF', 'ACTIVE',
       'staff' || to_char(n, 'FM00') || '@diabetes-demo.vn',
       DATE '1990-01-01' + (n * 173), CASE WHEN n % 2 = 0 THEN 'Nam' ELSE 'Nữ' END,
       address, '00109' || to_char(9000000 + n, 'FM0000000')
FROM seed
ON CONFLICT (username) DO NOTHING;

-- 4 doctor accounts: doctor01 ... doctor04
WITH seed(n, full_name, gender, specialty, license_no, degree, fee, address) AS (
  VALUES
    (1, 'BS. Nguyễn Văn An', 'Nam', 'Nội tiết - Đái tháo đường', 'HN-CCHN-2015-0101', 'Thạc sĩ', 250000.00, '18 Liễu Giai, Ba Đình, Hà Nội'),
    (2, 'BS. Trần Thu Bình', 'Nữ', 'Nội tổng hợp', 'HN-CCHN-2016-0248', 'Bác sĩ Chuyên khoa I', 200000.00, '35 Giải Phóng, Đống Đa, Hà Nội'),
    (3, 'BS. Lê Hoàng Cường', 'Nam', 'Tim mạch', 'HN-CCHN-2013-0876', 'Tiến sĩ', 300000.00, '22 Trần Thái Tông, Cầu Giấy, Hà Nội'),
    (4, 'BS. Phạm Minh Ngọc', 'Nữ', 'Dinh dưỡng lâm sàng', 'HN-CCHN-2018-1135', 'Thạc sĩ', 220000.00, '40 Tố Hữu, Nam Từ Liêm, Hà Nội')
)
INSERT INTO users(username, password, full_name, phone, role, status, email, dob, gender, address, cccd)
SELECT 'doctor' || to_char(n, 'FM00'),
       '$2a$12$CGtG.xgY9odR9P44E7frZePvENCSSfs03RfAleloK7Xl0.jABluzW',
       full_name, '0903' || to_char(400000 + n, 'FM000000'), 'DOCTOR', 'ACTIVE',
       'doctor' || to_char(n, 'FM00') || '@diabetes-demo.vn',
       DATE '1978-01-01' + (n * 521), gender, address,
       '00107' || to_char(8000000 + n, 'FM0000000')
FROM seed
ON CONFLICT (username) DO NOTHING;

WITH seed(n, specialty, license_no, degree, fee) AS (
  VALUES
    (1, 'Nội tiết - Đái tháo đường', 'HN-CCHN-2015-0101', 'Thạc sĩ', 250000.00),
    (2, 'Nội tổng hợp', 'HN-CCHN-2016-0248', 'Bác sĩ Chuyên khoa I', 200000.00),
    (3, 'Tim mạch', 'HN-CCHN-2013-0876', 'Tiến sĩ', 300000.00),
    (4, 'Dinh dưỡng lâm sàng', 'HN-CCHN-2018-1135', 'Thạc sĩ', 220000.00)
)
INSERT INTO doctors(user_id, specialty, license_no, license_issue_date, license_expire_date,
                    license_issued_by, degree, consultation_fee)
SELECT u.user_id, s.specialty, s.license_no,
       DATE '2015-01-15' + (s.n * 120), DATE '2030-01-15' + (s.n * 120),
       'Sở Y tế Hà Nội', s.degree, s.fee
FROM seed s
JOIN users u ON u.username = 'doctor' || to_char(s.n, 'FM00')
WHERE NOT EXISTS (SELECT 1 FROM doctors d WHERE d.user_id = u.user_id)
ON CONFLICT (license_no) DO NOTHING;

-- 50 patient accounts: patient001 ... patient050
WITH seed AS (
  SELECT n,
         (ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng','Vũ','Đặng','Bùi','Đỗ','Ngô'])[((n - 1) % 10) + 1]
           || ' ' ||
         (ARRAY['Văn','Thị','Đức','Thu','Quang'])[((n - 1) % 5) + 1]
           || ' ' ||
         (ARRAY['An','Bình','Cường','Dung','Hà','Hải','Hạnh','Hùng','Lan','Linh','Mai','Minh','Nam','Ngọc','Phương','Quân','Sơn','Trang','Tuấn','Yến'])[((n - 1) % 20) + 1]
           || ' ' || to_char(n, 'FM00') AS full_name,
         (ARRAY['Ba Đình','Hoàn Kiếm','Hai Bà Trưng','Đống Đa','Cầu Giấy','Thanh Xuân','Hà Đông','Long Biên','Tây Hồ','Nam Từ Liêm'])[((n - 1) % 10) + 1] AS district
  FROM generate_series(1, 50) AS g(n)
)
INSERT INTO users(username, password, full_name, phone, role, status, email, dob, gender, address, cccd)
SELECT 'patient' || to_char(n, 'FM000'),
       '$2a$12$UhiDHDBbiU8kjWVbctz7POgXvwGX1sN0cspLi9ocPd2VZ6qM9qdhe',
       full_name, '098' || to_char(5000000 + n, 'FM0000000'), 'PATIENT', 'ACTIVE',
       'patient' || to_char(n, 'FM000') || '@diabetes-demo.vn',
       DATE '1965-01-01' + (n * 137), CASE WHEN n % 2 = 0 THEN 'Nam' ELSE 'Nữ' END,
       (10 + n) || ' đường Nguyễn Trãi, ' || district || ', Hà Nội',
       '00106' || to_char(7000000 + n, 'FM0000000')
FROM seed
ON CONFLICT (username) DO NOTHING;

WITH seed AS (
  SELECT n,
         (ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng','Vũ','Đặng','Bùi','Đỗ','Ngô'])[((n - 1) % 10) + 1]
           || ' ' ||
         (ARRAY['Văn','Thị','Đức','Thu','Quang'])[((n - 1) % 5) + 1]
           || ' ' ||
         (ARRAY['An','Bình','Cường','Dung','Hà','Hải','Hạnh','Hùng','Lan','Linh','Mai','Minh','Nam','Ngọc','Phương','Quân','Sơn','Trang','Tuấn','Yến'])[((n - 1) % 20) + 1]
           || ' ' || to_char(n, 'FM00') AS full_name,
         (ARRAY['Ba Đình','Hoàn Kiếm','Hai Bà Trưng','Đống Đa','Cầu Giấy','Thanh Xuân','Hà Đông','Long Biên','Tây Hồ','Nam Từ Liêm'])[((n - 1) % 10) + 1] AS district
  FROM generate_series(1, 50) AS g(n)
)
INSERT INTO patients(user_id, full_name, date_of_birth, gender, phone, address,
                     health_insurance_no, created_by, national_id, national_id_date, national_id_place)
SELECT u.user_id, s.full_name, DATE '1965-01-01' + (s.n * 137),
       CASE WHEN s.n % 2 = 0 THEN 'Nam' ELSE 'Nữ' END,
       '098' || to_char(5000000 + s.n, 'FM0000000'),
       (10 + s.n) || ' đường Nguyễn Trãi, ' || s.district || ', Hà Nội',
       'HN4' || to_char(790000000000 + s.n, 'FM000000000000'),
       (SELECT user_id FROM users WHERE username = 'admin' LIMIT 1),
       '00106' || to_char(7000000 + s.n, 'FM0000000'),
       DATE '2021-01-01' + (s.n * 11), 'Cục Cảnh sát QLHC về trật tự xã hội'
FROM seed s
JOIN users u ON u.username = 'patient' || to_char(s.n, 'FM000')
WHERE NOT EXISTS (SELECT 1 FROM patients p WHERE p.user_id = u.user_id);
