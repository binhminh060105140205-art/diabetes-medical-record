package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class SimpleXlsxWriterTest {
    @Test
    void createsWorkbookWithTwoWorksheets() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        SimpleXlsxWriter.write(output, List.of(
                new SimpleXlsxWriter.Sheet("Tai khoan", List.of(List.of("ID", "Name"), List.of(1, "An"))),
                new SimpleXlsxWriter.Sheet("Benh nhan", List.of(List.of("ID", "Age"), List.of(2, 55)))));

        Set<String> entries = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) entries.add(entry.getName());
        }
        assertTrue(output.size() > 500);
        assertTrue(entries.contains("[Content_Types].xml"));
        assertTrue(entries.contains("xl/workbook.xml"));
        assertTrue(entries.contains("xl/worksheets/sheet1.xml"));
        assertTrue(entries.contains("xl/worksheets/sheet2.xml"));
        assertEquals(6, entries.size());
    }
}
