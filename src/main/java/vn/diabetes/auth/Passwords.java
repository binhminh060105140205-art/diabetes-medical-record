package vn.diabetes.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class Passwords {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);
    private Passwords() {}

    public static String encode(String raw) { return ENCODER.encode(raw); }
    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null) return false;
        return stored.startsWith("$2") ? ENCODER.matches(raw, stored) : raw.equals(stored);
    }
    public static boolean isEncoded(String value) { return value != null && value.startsWith("$2"); }
}
