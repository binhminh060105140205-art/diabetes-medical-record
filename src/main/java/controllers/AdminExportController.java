package controllers;

import dal.AdminDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import models.User;
import util.SimpleXlsxWriter;

@WebServlet("/AdminExport")
public class AdminExportController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        User admin = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(admin, "ADMIN")) {
            ControllerSupport.redirectToLogin(request, response);
            return;
        }

        AdminDAO dao = new AdminDAO();
        List<List<Object>> users = new ArrayList<>();
        users.add(List.of("ID", "Full Name", "Email", "Role", "Status"));
        for (Map<String, Object> row : dao.exportUsers()) {
            users.add(List.of(row.get("user_id"), text(row.get("full_name")),
                    text(row.get("email")), text(row.get("role")), text(row.get("status"))));
        }

        List<List<Object>> patients = new ArrayList<>();
        patients.add(List.of("ID", "Patient", "Age", "Gender", "Phone"));
        for (Map<String, Object> row : dao.exportPatients()) {
            patients.add(List.of(row.get("patient_id"), text(row.get("full_name")),
                    age(row.get("date_of_birth")), gender(row.get("gender")),
                    text(row.get("phone"))));
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=diacare-report-"
                + LocalDate.now().toString().replace("-", "") + ".xlsx");
        response.setHeader("Cache-Control", "no-store");
        SimpleXlsxWriter.write(response.getOutputStream(), List.of(
                new SimpleXlsxWriter.Sheet("Tai khoan", users),
                new SimpleXlsxWriter.Sheet("Benh nhan", patients)));
    }

    private Object age(Object value) {
        if (value == null) return "";
        LocalDate birthDate = value instanceof Date date ? date.toLocalDate() : (LocalDate) value;
        return Math.max(0, Period.between(birthDate, LocalDate.now()).getYears());
    }

    private String gender(Object value) {
        String gender = text(value);
        return switch (gender.toUpperCase()) {
            case "MALE" -> "Nam";
            case "FEMALE" -> "Nữ";
            default -> gender;
        };
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value); }
}
