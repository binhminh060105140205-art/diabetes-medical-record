UPDATE lab_orders
SET test_name = CASE UPPER(test_code)
  WHEN 'GLU' THEN 'Đường huyết'
  WHEN 'GLU_FASTING' THEN 'Đường huyết lúc đói'
  WHEN 'HBA1C' THEN 'HbA1c'
  WHEN 'LIPID' THEN 'Bộ xét nghiệm mỡ máu'
  WHEN 'CRE' THEN 'Creatinin - đánh giá chức năng thận'
  WHEN 'CREATININE' THEN 'Creatinin - đánh giá chức năng thận'
  WHEN 'UACR' THEN 'Tỷ lệ albumin và creatinin niệu'
  WHEN 'EGFR' THEN 'Mức lọc cầu thận ước tính'
  WHEN 'URINE_ALBUMIN' THEN 'Albumin niệu'
  ELSE test_name
END
WHERE UPPER(test_code) IN (
  'GLU', 'GLU_FASTING', 'HBA1C', 'LIPID', 'CRE',
  'CREATININE', 'UACR', 'EGFR', 'URINE_ALBUMIN'
);
