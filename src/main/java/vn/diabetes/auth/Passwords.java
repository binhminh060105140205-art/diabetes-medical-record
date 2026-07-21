package vn.diabetes.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class Passwords {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private Passwords() {}

    public static String encode(String raw) { return ENCODER.encode(raw); }
    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null) return false;
        return isEncoded(stored) ? ENCODER.matches(raw, stored) : raw.equals(stored);
    }
    public static boolean isEncoded(String value) { return value != null && value.startsWith("$2"); }
    public static boolean needsRehash(String value) {
        return value != null && !value.isBlank() && !isEncoded(value);
    }
}
