package vn.diabetes.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class Passwords {
    // Cost 10 keeps BCrypt secure while avoiding ~2s verification on Render Free CPU.
    private static final int STRENGTH = 10;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(STRENGTH);
    private Passwords() {}

    public static String encode(String raw) { return ENCODER.encode(raw); }
    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null) return false;
        return stored.startsWith("$2") ? ENCODER.matches(raw, stored) : raw.equals(stored);
    }
    public static boolean isEncoded(String value) { return value != null && value.startsWith("$2"); }
    public static boolean needsRehash(String value) {
        if (!isEncoded(value) || value.length() < 7) return true;
        try { return Integer.parseInt(value.substring(4, 6)) != STRENGTH; }
        catch (NumberFormatException ignored) { return true; }
    }
}
