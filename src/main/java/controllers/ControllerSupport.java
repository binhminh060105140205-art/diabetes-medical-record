package controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import models.User;

/** Shared HTTP/session helpers for the legacy servlet controllers. */
final class ControllerSupport {
    static final String DOCTOR_ID_SESSION_KEY = "clinicDoctorId";
    static final String PATIENT_ID_SESSION_KEY = "clinicPatientId";

    private ControllerSupport() {}

    static User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }

    static boolean hasRole(User user, String... roles) {
        if (user == null || user.getRole() == null) return false;
        for (String role : roles) {
            if (role.equals(user.getRole())) return true;
        }
        return false;
    }

    static void redirectToLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/Login");
    }

    static int positiveId(String value, String label) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) return parsed;
        } catch (NumberFormatException | NullPointerException ignored) {
            // Converted to the same validation error below.
        }
        throw new IllegalArgumentException(label + " không hợp lệ");
    }

    static int positiveIdOrZero(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : 0;
        } catch (NumberFormatException | NullPointerException ignored) {
            return 0;
        }
    }

    static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    static String requiredParameter(HttpServletRequest request, String name) {
        String value = clean(request.getParameter(name));
        if (value.isEmpty()) throw new IllegalArgumentException(name + " là bắt buộc");
        return value;
    }

    static void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }
}
