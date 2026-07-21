package controllers;

import dal.ClinicWorkflowDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import models.LabResultImportRow;
import models.User;
import util.LabResultCsvImporter;

/** Imports editable CSV results into existing laboratory orders for staff review. */
@WebServlet("/LabResultImport")
public class LabResultImportController extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "ADMIN", "STAFF")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            InputStream input = request.getServletContext().getResourceAsStream(
                    "/static/templates/lab-results-import.txt");
            if (input == null) {
                throw new IllegalArgumentException("Không tìm thấy file xét nghiệm trong project.");
            }
            List<LabResultImportRow> rows;
            try (input) {
                rows = LabResultCsvImporter.parse(input);
            }
            int imported = new ClinicWorkflowDAO()
                    .importStructuredLabResults(rows, user.getUserId());
            ControllerSupport.flash(request, "workflowFlash",
                    "Đã import " + imported
                    + " bệnh án. Kết quả đang chờ bác sĩ xác nhận.");
        } catch (IllegalArgumentException error) {
            ControllerSupport.flash(request, "workflowFlash",
                    "Không thể import: " + error.getMessage());
        } catch (IllegalStateException error) {
            ControllerSupport.flash(request, "workflowFlash",
                    "Không thể import dữ liệu lúc này. Vui lòng kiểm tra file và thử lại.");
        }
        response.sendRedirect(request.getContextPath() + "/ClinicWorkflow?view=labs");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
