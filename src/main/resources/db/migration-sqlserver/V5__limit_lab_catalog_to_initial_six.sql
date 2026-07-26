UPDATE lab_orders
SET status = N'CANCELLED'
WHERE status <> N'CANCELLED'
  AND UPPER(test_code) NOT IN (
      N'GLU_FASTING',
      N'HBA1C',
      N'CHOLESTEROL',
      N'TRIGLYCERIDE',
      N'HDL_C',
      N'LDL_C'
  );
