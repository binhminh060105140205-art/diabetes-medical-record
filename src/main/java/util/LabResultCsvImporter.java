package util;

import models.LabResultImportRow;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Parses and validates the editable laboratory-result CSV template. */
public final class LabResultCsvImporter {
    private static final List<String> HEADERS = List.of(
            "record_id", "blood_glucose", "hba1c", "cholesterol",
            "triglyceride", "hdl_c", "ldl_c");
    private static final int MAX_ROWS = 200;

    private LabResultCsvImporter() {}

    public static List<LabResultImportRow> parse(InputStream input) throws IOException {
        if (input == null) throw new IllegalArgumentException("Chưa chọn file kết quả xét nghiệm.");
        byte[] bytes = input.readAllBytes();
        String content = new String(bytes, StandardCharsets.UTF_8);
        String firstDataLine = content.lines()
                .map(LabResultCsvImporter::stripBom)
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .findFirst()
                .orElse("");
        if (firstDataLine.contains("=")) return parseKeyValue(content);
        return parseCsv(new ByteArrayInputStream(bytes));
    }

    private static List<LabResultImportRow> parseCsv(InputStream input) throws IOException {
        if (input == null) throw new IllegalArgumentException("Chưa chọn file kết quả xét nghiệm.");

        List<LabResultImportRow> rows = new ArrayList<>();
        Set<Integer> recordIds = new HashSet<>();
        boolean headerRead = false;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            Character delimiter = null;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1) line = stripBom(line);
                if (line.length() > 4000) {
                    throw invalid(lineNumber, "Dòng quá dài, tối đa 4000 ký tự.");
                }
                if (line.isBlank() || line.trim().startsWith("#")) continue;

                if (!headerRead) {
                    delimiter = detectDelimiter(line);
                    List<String> header = normalize(parseLine(line, delimiter));
                    if (!HEADERS.equals(header)) {
                        throw invalid(lineNumber,
                                "Tiêu đề không đúng. Hãy dùng file mẫu lab-results-import.csv.");
                    }
                    headerRead = true;
                    continue;
                }

                if (rows.size() >= MAX_ROWS) {
                    throw invalid(lineNumber, "Mỗi file chỉ được import tối đa " + MAX_ROWS + " dòng.");
                }
                List<String> values = parseLine(line, delimiter);
                if (values.size() != HEADERS.size()) {
                    throw invalid(lineNumber, "Cần đủ 7 cột: record_id và 6 chỉ số xét nghiệm.");
                }
                LabResultImportRow row = parseRow(lineNumber, values);
                if (!recordIds.add(row.recordId())) {
                    throw invalid(lineNumber, "Mã bệnh án bị lặp trong file: " + row.recordId() + ".");
                }
                rows.add(row);
            }
        }

        if (!headerRead) throw new IllegalArgumentException("File chưa có dòng tiêu đề.");
        if (rows.isEmpty()) throw new IllegalArgumentException("File chưa có dòng kết quả để import.");
        return rows;
    }

    private static List<LabResultImportRow> parseKeyValue(String content) {
        Map<String, String> values = new LinkedHashMap<>();
        Map<String, Integer> lineNumbers = new LinkedHashMap<>();
        int lineNumber = 0;
        for (String rawLine : content.split("\\R", -1)) {
            lineNumber++;
            String line = lineNumber == 1 ? stripBom(rawLine) : rawLine;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int separator = trimmed.indexOf('=');
            if (separator <= 0) {
                throw invalid(lineNumber, "Dòng phải có dạng ten_chi_so = gia_tri.");
            }
            String key = trimmed.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            if (!HEADERS.contains(key)) {
                throw invalid(lineNumber, "Tên chỉ số không được hỗ trợ: " + key + ".");
            }
            if (values.containsKey(key)) {
                throw invalid(lineNumber, "Tên chỉ số bị lặp: " + key + ".");
            }
            values.put(key, cleanPlaceholder(trimmed.substring(separator + 1)));
            lineNumbers.put(key, lineNumber);
        }

        List<String> ordered = HEADERS.stream()
                .map(key -> values.getOrDefault(key, ""))
                .toList();
        int recordLine = lineNumbers.getOrDefault("record_id", 1);
        if (!values.containsKey("record_id")) {
            throw invalid(recordLine, "Thiếu record_id.");
        }
        return List.of(parseRow(recordLine, ordered));
    }

    private static String cleanPlaceholder(String value) {
        String trimmed = value.trim();
        return trimmed.matches("^\\(.*\\)$") ? "" : trimmed;
    }

    private static LabResultImportRow parseRow(int lineNumber, List<String> values) {
        int recordId = parseRecordId(lineNumber, values.get(0));
        Double bloodGlucose = decimal(lineNumber, "Đường huyết", values.get(1), 1, 40);
        Double hba1c = decimal(lineNumber, "HbA1c", values.get(2), 2, 20);
        Double cholesterol = decimal(lineNumber, "Cholesterol", values.get(3), 0.1, 30);
        Double triglyceride = decimal(lineNumber, "Triglyceride", values.get(4), 0.1, 30);
        Double hdlC = decimal(lineNumber, "HDL-C", values.get(5), 0.1, 15);
        Double ldlC = decimal(lineNumber, "LDL-C", values.get(6), 0.1, 20);
        LabResultImportRow row = new LabResultImportRow(lineNumber, recordId, bloodGlucose,
                hba1c, cholesterol, triglyceride, hdlC, ldlC);
        if (!row.hasAnyValue()) {
            throw invalid(lineNumber, "Cần nhập ít nhất một chỉ số xét nghiệm.");
        }
        return row;
    }

    private static int parseRecordId(int lineNumber, String raw) {
        String value = raw == null ? "" : raw.trim();
        try {
            int recordId = Integer.parseInt(value);
            if (recordId > 0) return recordId;
        } catch (NumberFormatException ignored) { }
        throw invalid(lineNumber, "record_id phải là mã bệnh án nguyên dương.");
    }

    private static Double decimal(int lineNumber, String label, String raw,
            double minimum, double maximum) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return null;
        try {
            double number = new BigDecimal(value.replace(',', '.')).doubleValue();
            if (!Double.isFinite(number) || number < minimum || number > maximum) {
                throw invalid(lineNumber, label + " phải từ " + minimum + " đến " + maximum + ".");
            }
            return number;
        } catch (NumberFormatException error) {
            throw invalid(lineNumber, label + " phải là số hợp lệ.");
        }
    }

    private static Character detectDelimiter(String line) {
        return count(line, ';') > count(line, ',') ? ';' : ',';
    }

    private static int count(String value, char expected) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) == expected) count++;
        return count;
    }

    private static List<String> normalize(List<String> values) {
        return values.stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .toList();
    }

    private static List<String> parseLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == delimiter && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        if (quoted) throw new IllegalArgumentException("File CSV có dấu ngoặc kép chưa đóng.");
        values.add(current.toString());
        return values;
    }

    private static String stripBom(String line) {
        return !line.isEmpty() && line.charAt(0) == '\uFEFF' ? line.substring(1) : line;
    }

    private static IllegalArgumentException invalid(int lineNumber, String message) {
        return new IllegalArgumentException("Dòng " + lineNumber + ": " + message);
    }
}
