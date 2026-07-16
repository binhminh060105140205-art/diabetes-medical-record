-- Student/demo deployment: remove BCrypt cost from the bundled acceptance accounts.
UPDATE users SET password = 'Staff@123' WHERE username ~ '^staff(0[1-9]|10)$';
UPDATE users SET password = 'Doctor@123' WHERE username ~ '^doctor0[1-4]$';
UPDATE users SET password = 'Patient@123' WHERE username ~ '^patient(0[0-4][0-9]|050)$';
