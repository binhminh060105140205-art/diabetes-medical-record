package util;

import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lưu / đọc file ảnh của bác sĩ (ảnh khuôn mặt, ảnh CCCD, ảnh chứng chỉ hành nghề)
 * trong thư mục uploads bên ngoài file WAR để dữ liệu không bị mất khi clean hoặc build lại.
 */
public class FileStorageUtil {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB

    /** Kept for serving legacy uploads; new doctor records no longer collect a face photo. */
    public static final String TYPE_FACE = "face";
    public static final String TYPE_CCCD = "cccd";
    public static final String TYPE_CCCD_BACK = "cccd-back";
    public static final String TYPE_LICENSE = "license";

    private static File getRootDir() {
        String configured = System.getProperty("app.upload-dir");
        File root = configured == null || configured.isBlank()
                ? new File(System.getProperty("user.dir"), "uploads")
                : new File(configured);
        if (!root.exists()) root.mkdirs();
        return root;
    }

    private static File getDoctorDir(int doctorId) {
        File dir = new File(getRootDir(), String.valueOf(doctorId));
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static String extractExtension(Part part) {
        String submitted = part.getSubmittedFileName();
        if (submitted == null || !submitted.contains(".")) return null;
        return submitted.substring(submitted.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public static void validateDoctorImage(Part part, String label, boolean required) {
        if (part == null || part.getSize() == 0) {
            if (required) throw new IllegalArgumentException(label + " là bắt buộc.");
            return;
        }
        if (part.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(label + " vượt quá 5MB.");
        }
        String contentType = part.getContentType();
        String ext = extractExtension(part);
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")
                || ext == null || !ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException(
                    label + " chỉ chấp nhận JPG, JPEG, PNG hoặc WEBP.");
        }
    }

    /**
     * Lưu ảnh upload cho bác sĩ. Trả về tên file đã lưu (chỉ cần lưu tên này vào DB),
     * hoặc null nếu part rỗng / không hợp lệ.
     *
     * @param part     phần file lấy từ request.getPart(...)
     * @param doctorId id bác sĩ
     * @param type     "face" | "cccd" | "license"
     */
    public static String saveDoctorImage(Part part, int doctorId, String type) throws IOException {
        if (part == null || part.getSize() == 0) return null;
        validateDoctorImage(part, "Ảnh hồ sơ bác sĩ", false);
        String ext = extractExtension(part);

        File targetFile = new File(getDoctorDir(doctorId), type + "." + ext);

        // Xóa các file cùng loại nhưng khác đuôi từ lần upload trước (tránh rác + ảnh cũ lẫn ảnh mới)
        for (String otherExt : ALLOWED_EXT) {
            if (!otherExt.equals(ext)) {
                File stale = new File(getDoctorDir(doctorId), type + "." + otherExt);
                if (stale.exists()) stale.delete();
            }
        }

        try (InputStream in = part.getInputStream();
             OutputStream out = Files.newOutputStream(targetFile.toPath())) {
            in.transferTo(out);
        }

        return targetFile.getName();
    }

    /**
     * Lấy file ảnh đã lưu của bác sĩ theo loại. Trả về null nếu chưa có ảnh nào.
     */
    public static File resolveDoctorImage(int doctorId, String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) return null;
        File f = new File(getDoctorDir(doctorId), storedFileName);
        return f.exists() ? f : null;
    }

    /** Removes files created for an account whose database creation must be rolled back. */
    public static void deleteDoctorImages(int doctorId) {
        File directory = getDoctorDir(doctorId);
        for (String type : List.of(TYPE_FACE, TYPE_CCCD, TYPE_CCCD_BACK, TYPE_LICENSE)) {
            for (String extension : ALLOWED_EXT) {
                try {
                    Files.deleteIfExists(new File(directory, type + "." + extension).toPath());
                } catch (IOException ignored) {
                    // Best-effort cleanup; the database account is rolled back separately.
                }
            }
        }
        File[] remaining = directory.listFiles();
        if (remaining != null && remaining.length == 0) {
            try {
                Files.deleteIfExists(directory.toPath());
            } catch (IOException ignored) {
                // An empty directory is harmless and can be reused by a later upload.
            }
        }
    }
}
