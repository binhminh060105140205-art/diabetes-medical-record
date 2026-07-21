package controllers;

import dal.ClinicWorkflowDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import models.LabResultImportRow;
import models.User;
import util.LabResultCsvImporter;

/** Imports an uploaded result file into the medical record selected by staff. */
@WebServlet("/LabResultImport")
@MultipartConfig(maxFileSize = 1024 * 1024, maxRequestSize = 2 * 1024 * 1024)
public class LabResultImportController extends HttpServlet {
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
            Part resultFile = request.getPart("resultFile");
            validateFile(resultFile);
            List<LabResultImportRow> rows;
            try (InputStream input = resultFile.getInputStream()) {
                rows = LabResultCsvImporter.parseForRecord(input, recordId);
            }
            int imported = new ClinicWorkflowDAO()
                    .importStructuredLabResults(rows, user.getUserId());
            ControllerSupport.flash(request, "workflowFlash",
                    "Đã import " + imported + " dòng kết quả cho bệnh án #" + recordId
                    + ". Kết quả đang chờ bác sĩ xác nhận.");
        } catch (IllegalArgumentException error) {
            ControllerSupport.flash(request, "workflowFlash",
                    "Không thể import: " + error.getMessage());
        } catch (IllegalStateException error) {
            ControllerSupport.flash(request, "workflowFlash",
                    "Không thể import dữ liệu lúc này. Vui lòng kiểm tra file và thử lại.");
        }
        response.sendRedirect(request.getContextPath() + "/ClinicWorkflow?view=labs");
    }

    private void validateFile(Part resultFile) {
        if (resultFile == null || resultFile.getSize() == 0) {
            throw new IllegalArgumentException("Chưa chọn file kết quả xét nghiệm.");
        }
        String fileName = resultFile.getSubmittedFileName();
        String normalized = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (!normalized.endsWith(".txt") && !normalized.endsWith(".csv")) {
            throw new IllegalArgumentException("File kết quả phải có định dạng .txt hoặc .csv.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
