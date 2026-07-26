package models;

/** One structured laboratory result row imported from the editable CSV template. */
public record LabResultImportRow(
        int lineNumber,
        int recordId,
        Double bloodGlucose,
        Double hba1c,
        Double cholesterol,
        Double triglyceride,
        Double hdlC,
        Double ldlC) {

    public boolean hasAnyValue() {
        return bloodGlucose != null || hba1c != null || cholesterol != null
                || triglyceride != null || hdlC != null || ldlC != null;
    }

    public int valueCount() {
        int count = 0;
        if (bloodGlucose != null) count++;
        if (hba1c != null) count++;
        if (cholesterol != null) count++;
        if (triglyceride != null) count++;
        if (hdlC != null) count++;
        if (ldlC != null) count++;
        return count;
    }
}
