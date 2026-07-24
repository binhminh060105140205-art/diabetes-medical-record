package vn.diabetes.validation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;

public final class Validators {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{4,30}$");
    private static final Pattern PHONE = Pattern.compile("^(0[0-9]{9}|\\+84[0-9]{9})$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern INSURANCE = Pattern.compile("^[A-Za-z0-9]{10,20}$");
    private static final Pattern CCCD = Pattern.compile("^[0-9]{12}$");

    private Validators() {}

    public static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static String required(String value, String label) {
        String cleaned = clean(value);
        if (cleaned.isEmpty()) throw new IllegalArgumentException(label + " là bắt buộc.");
        return cleaned;
    }

    public static String username(String value) {
        String cleaned = required(value, "Tên đăng nhập");
        if (!USERNAME.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("Tên đăng nhập gồm 4–30 chữ, số hoặc dấu gạch dưới.");
        }
        return cleaned;
    }

    public static String password(String value, String label) {
        if (value == null || value.length() < 8 || value.length() > 72) {
            throw new IllegalArgumentException(label + " phải có từ 8 đến 72 ký tự.");
        }
        return value;
    }

    public static String fullName(String value) {
        String cleaned = required(value, "Họ tên");
        if (cleaned.length() < 2 || cleaned.length() > 100) {
            throw new IllegalArgumentException("Họ tên phải có từ 2 đến 100 ký tự.");
        }
        return cleaned;
    }

    public static String phone(String value) {
        String cleaned = required(value, "Số điện thoại");
        if (!PHONE.matcher(cleaned).matches()) throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        return cleaned;
    }

    public static String email(String value, boolean required) {
        String cleaned = clean(value);
        if (required && cleaned.isEmpty()) throw new IllegalArgumentException("Email là bắt buộc.");
        if (!cleaned.isEmpty() && (!EMAIL.matcher(cleaned).matches() || cleaned.length() > 100)) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        return cleaned;
    }

    public static String gender(String value) {
        String cleaned = required(value, "Giới tính");
        if (!Set.of("Nam", "Nữ", "Khác").contains(cleaned)) throw new IllegalArgumentException("Giới tính không hợp lệ.");
        return cleaned;
    }

    public static LocalDate dateOfBirth(String value, boolean required) {
        String cleaned = clean(value);
        if (cleaned.isEmpty()) {
            if (required) throw new IllegalArgumentException("Ngày sinh là bắt buộc.");
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(cleaned);
            if (date.isAfter(LocalDate.now()) || date.isBefore(LocalDate.of(1900, 1, 1))) {
                throw new IllegalArgumentException("Ngày sinh không hợp lệ.");
            }
            return date;
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ.");
        }
    }

    public static String address(String value) {
        String cleaned = max(value, 255, "Địa chỉ");
        if (!cleaned.isEmpty() && cleaned.length() < 5) {
            throw new IllegalArgumentException("Địa chỉ phải có từ 5 đến 255 ký tự.");
        }
        return cleaned;
    }

    public static String requiredAddress(String value) {
        return address(required(value, "Địa chỉ"));
    }

    public static String insurance(String value) {
        String cleaned = clean(value);
        if (!cleaned.isEmpty() && !INSURANCE.matcher(cleaned).matches()) throw new IllegalArgumentException("Số BHYT không hợp lệ.");
        return cleaned;
    }

    public static String cccd(String value) {
        String cleaned = clean(value);
        if (!cleaned.isEmpty() && !CCCD.matcher(cleaned).matches()) throw new IllegalArgumentException("CCCD phải gồm đúng 12 chữ số.");
        return cleaned;
    }

    public static String role(String value) {
        String cleaned = required(value, "Vai trò").toUpperCase();
        if (!Set.of("ADMIN", "STAFF", "DOCTOR", "PATIENT").contains(cleaned)) throw new IllegalArgumentException("Vai trò không hợp lệ.");
        return cleaned;
    }

    public static String max(String value, int length, String label) {
        String cleaned = clean(value);
        if (cleaned.length() > length) throw new IllegalArgumentException(label + " tối đa " + length + " ký tự.");
        return cleaned;
    }
}
