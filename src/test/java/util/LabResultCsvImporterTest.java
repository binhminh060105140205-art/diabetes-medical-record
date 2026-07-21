package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LabResultCsvImporterTest {
    private static final String HEADER =
            "record_id,blood_glucose,hba1c,cholesterol,triglyceride,hdl_c,ldl_c\n";

    @Test
    void importsValidStructuredValues() throws Exception {
        var rows = parse("# ghi chu\n" + HEADER + "25,5.6,6.5,5.0,1.7,1.3,2.6\n");

        assertEquals(1, rows.size());
        assertEquals(25, rows.get(0).recordId());
        assertEquals(5.6, rows.get(0).bloodGlucose());
        assertEquals(6.5, rows.get(0).hba1c());
    }

    @Test
    void supportsSemicolonFilesAndDecimalCommas() throws Exception {
        String csv = "record_id;blood_glucose;hba1c;cholesterol;triglyceride;hdl_c;ldl_c\n"
                + "26;5,8;6,7;;;;\n";

        var rows = parse(csv);
        assertEquals(5.8, rows.get(0).bloodGlucose());
        assertEquals(6.7, rows.get(0).hba1c());
    }

    @Test
    void importsReadableKeyValueTemplate() throws Exception {
        String text = "# hồ sơ xét nghiệm\n"
                + "record_id = 27\n"
                + "blood_glucose = (nhập)\n"
                + "hba1c = 7,1\n"
                + "cholesterol = 5.2\n";

        var rows = parse(text);
        assertEquals(27, rows.get(0).recordId());
        assertEquals(7.1, rows.get(0).hba1c());
        assertEquals(5.2, rows.get(0).cholesterol());
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertThrows(IllegalArgumentException.class,
                () -> parse(HEADER + "25,900,6.5,,,,\n"));
    }

    @Test
    void rejectsDuplicateRecordIds() {
        assertThrows(IllegalArgumentException.class,
                () -> parse(HEADER + "25,5.6,,,,,\n25,,6.5,,,,\n"));
    }

    @Test
    void rejectsRowsWithoutResults() {
        assertThrows(IllegalArgumentException.class,
                () -> parse(HEADER + "25,,,,,,\n"));
    }

    private java.util.List<models.LabResultImportRow> parse(String csv) throws Exception {
        return LabResultCsvImporter.parse(new ByteArrayInputStream(
                csv.getBytes(StandardCharsets.UTF_8)));
    }
}
