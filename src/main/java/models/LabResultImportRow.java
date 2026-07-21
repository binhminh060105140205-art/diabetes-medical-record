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
}
