package vn.diabetes.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class Passwords {
    // Only reads old BCrypt accounts once; new and migrated accounts use plain text.
    private static final BCryptPasswordEncoder LEGACY_ENCODER = new BCryptPasswordEncoder();
    private Passwords() {}

    public static String encode(String raw) { return raw; }
    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null) return false;
        return stored.startsWith("$2") ? LEGACY_ENCODER.matches(raw, stored) : raw.equals(stored);
    }
    public static boolean isEncoded(String value) { return value != null && value.startsWith("$2"); }
    public static boolean needsRehash(String value) {
        return isEncoded(value);
    }
}
