-- 1. Thêm cột cccd nếu chưa có
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[Users]') AND name = 'cccd')
BEGIN
    ALTER TABLE Users ADD cccd NVARCHAR(50);
END
GO

-- 2. Cập nhật dữ liệu CCCD giả cho các tài khoản hiện tại
UPDATE Users SET cccd = '001099000001' WHERE username = 'admin';
UPDATE Users SET cccd = '001099000002' WHERE username = 'admin2';

UPDATE Users SET cccd = '030099000001' WHERE username = 'staff1';
UPDATE Users SET cccd = '030099000002' WHERE username = 'staff2';
UPDATE Users SET cccd = '030099000003' WHERE username = 'staff3';

UPDATE Users SET cccd = '034099000001' WHERE username = 'bacsi1';
UPDATE Users SET cccd = '034099000002' WHERE username = 'bacsi2';
UPDATE Users SET cccd = '034099000003' WHERE username = 'bacsi3';
UPDATE Users SET cccd = '034099000004' WHERE username = 'bacsi4';
UPDATE Users SET cccd = '034099000005' WHERE username = 'bacsi5';

UPDATE Users SET cccd = '079099000001' WHERE username = 'benhnhan1';
UPDATE Users SET cccd = '079099000002' WHERE username = 'benhnhan2';
UPDATE Users SET cccd = '079099000003' WHERE username = 'benhnhan3';
UPDATE Users SET cccd = '079099000004' WHERE username = 'benhnhan4';
UPDATE Users SET cccd = '079099000005' WHERE username = 'benhnhan5';
UPDATE Users SET cccd = '079099000006' WHERE username = 'benhnhan6';
UPDATE Users SET cccd = '079099000007' WHERE username = 'benhnhan7';
UPDATE Users SET cccd = '079099000008' WHERE username = 'benhnhan8';
UPDATE Users SET cccd = '079099000009' WHERE username = 'benhnhan9';

-- Với các user còn lại chưa được cập nhật CCCD
UPDATE Users SET cccd = '0000000000' + CAST(user_id AS NVARCHAR(20)) WHERE cccd IS NULL;
GO
