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

/** Imports the bundled result file into the medical record selected by staff. */
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

        try {
            int recordId = ControllerSupport.positiveId(
                    request.getParameter("recordId"), "Mã bệnh án");
            List<LabResultImportRow> rows;
            try (InputStream input = defaultResultsInput()) {
                rows = LabResultCsvImporter.parseForRecord(input, recordId);
            }
            int imported = new ClinicWorkflowDAO()
                    .importStructuredLabResults(rows, user.getUserId());
            ControllerSupport.flash(request, "workflowFlash",
                    "Đã import " + imported + " dòng kết quả từ file mặc định cho bệnh án #" + recordId
                    + ". Kết quả đang chờ bác sĩ xác nhận.");
        } catch (IllegalArgumentException error) {
            ControllerSupport.flash(request, "workflowFlash",
                    "Không thể import từ file mặc định: " + error.getMessage());
        } catch (IllegalStateException | IOException error) {
            ControllerSupport.flash(request, "workflowFlash",
                    "Không thể đọc file kết quả mặc định. Vui lòng kiểm tra file trong src và thử lại.");
        }
        response.sendRedirect(request.getContextPath() + "/ClinicWorkflow?view=labs");
    }

    InputStream defaultResultsInput() throws IOException {
        return new ClassPathResource(DEFAULT_RESULTS_RESOURCE).getInputStream();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
