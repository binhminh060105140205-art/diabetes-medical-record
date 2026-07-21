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
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import models.LabResultImportRow;
import models.User;
import util.LabResultCsvImporter;

/** Imports editable CSV results into existing laboratory orders for staff review. */
@WebServlet("/LabResultImport")
@MultipartConfig(maxFileSize = 1024 * 1024, maxRequestSize = 1200 * 1024)
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
            String source = ControllerSupport.clean(request.getParameter("source"));
            InputStream input;
            if ("template".equals(source)) {
                input = request.getServletContext().getResourceAsStream(
                        "/static/templates/lab-results-import.txt");
                if (input == null) {
                    throw new IllegalArgumentException("Không tìm thấy file mẫu trong project.");
                }
            } else {
                Part file = request.getPart("labFile");
                validateFile(file);
                input = file.getInputStream();
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
        } catch (ServletException error) {
            ControllerSupport.flash(request, "workflowFlash",
                    "File tải lên không hợp lệ hoặc vượt quá 1 MB.");
        }
        response.sendRedirect(request.getContextPath() + "/ClinicWorkflow?view=labs");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private void validateFile(Part file) {
        if (file == null || file.getSize() == 0) {
            throw new IllegalArgumentException("Vui lòng chọn file CSV kết quả xét nghiệm.");
        }
        String submitted = file.getSubmittedFileName();
        String fileName = submitted == null ? "" : Path.of(submitted).getFileName().toString();
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".csv") && !lowerName.endsWith(".txt")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file .csv hoặc .txt theo đúng file mẫu.");
        }
    }
}
