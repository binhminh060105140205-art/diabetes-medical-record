package controllers;

import dal.ClinicWorkflowDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import models.LabResultImportRow;
import models.User;
import org.springframework.core.io.ClassPathResource;
import util.LabResultCsvImporter;

/** Imports the bundled result file into the selected medical record. */
@WebServlet("/LabResultImport")
public class LabResultImportController extends HttpServlet {
    static final String DEFAULT_RESULTS_RESOURCE =
            "lab-results/default-lab-results.txt";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        User user = ControllerSupport.currentUser(request);
        if (!ControllerSupport.hasRole(user, "ADMIN", "STAFF")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        int recordId = ControllerSupport.positiveIdOrZero(request.getParameter("recordId"));
        String redirect = recordId > 0
                ? request.getContextPath() + "/MedicalRecordForm?recordId=" + recordId + "&tab=3"
                : request.getContextPath() + "/ClinicWorkflow?view=labs";
        try {
            recordId = ControllerSupport.positiveId(request.getParameter("recordId"), "Mã bệnh án");
            try (InputStream input = defaultResultsInput()) {
                List<LabResultImportRow> rows = LabResultCsvImporter.parseForRecord(input, recordId);
                int imported = new ClinicWorkflowDAO()
                        .importStructuredLabResults(rows, user.getUserId());
                ControllerSupport.flash(request, "recordSuccess",
                        "Đã import " + imported + " chỉ số vào bệnh án #" + recordId
                                + ". Kết quả đang chờ bác sĩ xác nhận.");
            }
        } catch (IllegalArgumentException error) {
            ControllerSupport.flash(request, "recordFlash",
                    "Không thể import kết quả: " + error.getMessage());
        } catch (IllegalStateException | IOException error) {
            ControllerSupport.flash(request, "recordFlash",
                    "Không thể đọc file kết quả mẫu. Vui lòng tải lại trang và thử lại.");
        }
        response.sendRedirect(redirect);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/ClinicWorkflow?view=labs");
    }

    InputStream defaultResultsInput() throws IOException {
        return new ClassPathResource(DEFAULT_RESULTS_RESOURCE).getInputStream();
    }
}
