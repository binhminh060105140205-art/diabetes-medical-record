package vn.diabetes.auth;

/** So sánh mật khẩu trực tiếp theo cấu hình demo local của dự án. */
public final class Passwords {
    private Passwords() {}

    public static boolean matches(String input, String stored) {
        return input != null && stored != null && input.equals(stored);
    }
}
