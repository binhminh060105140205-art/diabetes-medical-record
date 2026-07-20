package util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Small dependency-free XLSX writer for administrator exports. */
public final class SimpleXlsxWriter {
    private SimpleXlsxWriter() { }
    public record Sheet(String name, List<List<Object>> rows) { }

    public static void write(OutputStream output, List<Sheet> sheets) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            put(zip, "[Content_Types].xml", contentTypes(sheets.size()));
            put(zip, "_rels/.rels", rootRels());
            put(zip, "xl/workbook.xml", workbook(sheets));
            put(zip, "xl/_rels/workbook.xml.rels", workbookRels(sheets.size()));
            for (int i = 0; i < sheets.size(); i++) {
                put(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", sheet(sheets.get(i)));
            }
            zip.finish();
        }
    }

    private static void put(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String rootRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private static String workbook(List<Sheet> sheets) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        for (int i = 0; i < sheets.size(); i++) xml.append("<sheet name=\"").append(esc(sheets.get(i).name())).append("\" sheetId=\"").append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>");
        return xml.append("</sheets></workbook>").toString();
    }

    private static String workbookRels(int count) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 1; i <= count; i++) xml.append("<Relationship Id=\"rId").append(i).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(i).append(".xml\"/>");
        return xml.append("</Relationships>").toString();
    }

    private static String contentTypes(int count) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        for (int i = 1; i <= count; i++) xml.append("<Override PartName=\"/xl/worksheets/sheet")
                .append(i).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        return xml.append("</Types>").toString();
    }

    private static String sheet(Sheet sheet) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        for (int r = 0; r < sheet.rows().size(); r++) {
            List<Object> row = sheet.rows().get(r);
            xml.append("<row r=\"").append(r + 1).append("\">");
            for (int c = 0; c < row.size(); c++) appendCell(xml, ref(c, r + 1), row.get(c));
            xml.append("</row>");
        }
        return xml.append("</sheetData></worksheet>").toString();
    }

    private static void appendCell(StringBuilder xml, String ref, Object value) {
        if (value == null) xml.append("<c r=\"").append(ref).append("\"/>");
        else if (value instanceof Number number) xml.append("<c r=\"").append(ref).append("\"><v>").append(number).append("</v></c>");
        else xml.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(esc(String.valueOf(value))).append("</t></is></c>");
    }

    private static String ref(int column, int row) {
        StringBuilder letters = new StringBuilder();
        int value = column + 1;
        while (value > 0) {
            value--;
            letters.insert(0, (char) ('A' + value % 26));
            value /= 26;
        }
        return letters.append(row).toString();
    }

    private static String esc(String value) {
        return value.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
