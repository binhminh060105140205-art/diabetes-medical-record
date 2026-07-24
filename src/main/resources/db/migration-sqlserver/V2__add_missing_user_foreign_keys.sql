SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_key_columns
    WHERE parent_object_id = OBJECT_ID(N'dbo.patients')
      AND parent_column_id = COLUMNPROPERTY(
              OBJECT_ID(N'dbo.patients'), N'created_by', 'ColumnId')
      AND referenced_object_id = OBJECT_ID(N'dbo.users')
      AND referenced_column_id = COLUMNPROPERTY(
              OBJECT_ID(N'dbo.users'), N'user_id', 'ColumnId')
)
BEGIN
    ALTER TABLE dbo.patients
        ADD CONSTRAINT fk_patients_created_by
        FOREIGN KEY (created_by) REFERENCES dbo.users(user_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_key_columns
    WHERE parent_object_id = OBJECT_ID(N'dbo.medicalrecords')
      AND parent_column_id = COLUMNPROPERTY(
              OBJECT_ID(N'dbo.medicalrecords'), N'created_by_staff', 'ColumnId')
      AND referenced_object_id = OBJECT_ID(N'dbo.users')
      AND referenced_column_id = COLUMNPROPERTY(
              OBJECT_ID(N'dbo.users'), N'user_id', 'ColumnId')
)
BEGIN
    ALTER TABLE dbo.medicalrecords
        ADD CONSTRAINT fk_records_created_by
        FOREIGN KEY (created_by_staff) REFERENCES dbo.users(user_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_key_columns
    WHERE parent_object_id = OBJECT_ID(N'dbo.healthindicators')
      AND parent_column_id = COLUMNPROPERTY(
              OBJECT_ID(N'dbo.healthindicators'), N'entered_by_staff', 'ColumnId')
      AND referenced_object_id = OBJECT_ID(N'dbo.users')
      AND referenced_column_id = COLUMNPROPERTY(
              OBJECT_ID(N'dbo.users'), N'user_id', 'ColumnId')
)
BEGIN
    ALTER TABLE dbo.healthindicators
        ADD CONSTRAINT fk_health_entered_by
        FOREIGN KEY (entered_by_staff) REFERENCES dbo.users(user_id);
END;
