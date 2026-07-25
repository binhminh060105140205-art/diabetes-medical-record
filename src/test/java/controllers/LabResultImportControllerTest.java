package controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import org.junit.jupiter.api.Test;
import util.LabResultCsvImporter;

class LabResultImportControllerTest {
    @Test
    void bundledDefaultFileIsPresentAndImportable() throws Exception {
        LabResultImportController controller = new LabResultImportController();

        try (InputStream input = controller.defaultResultsInput()) {
            var rows = LabResultCsvImporter.parseForRecord(input, 51);

            assertEquals(1, rows.size());
            assertEquals(51, rows.get(0).recordId());
            assertEquals(6.2, rows.get(0).bloodGlucose());
            assertEquals(6.8, rows.get(0).hba1c());
        }
    }
}
