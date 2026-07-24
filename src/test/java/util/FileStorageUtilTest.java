package util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileStorageUtilTest {
    @TempDir
    Path uploadRoot;

    @AfterEach
    void clearUploadProperty() {
        System.clearProperty("app.upload-dir");
    }

    @Test
    void deletesOnlyManagedDoctorImages() throws Exception {
        System.setProperty("app.upload-dir", uploadRoot.toString());
        Path doctorDirectory = Files.createDirectories(uploadRoot.resolve("27"));
        Path front = Files.writeString(doctorDirectory.resolve("cccd.jpg"), "front");
        Path back = Files.writeString(doctorDirectory.resolve("cccd-back.png"), "back");
        Path license = Files.writeString(doctorDirectory.resolve("license.webp"), "license");
        Path unrelated = Files.writeString(doctorDirectory.resolve("note.txt"), "keep");

        FileStorageUtil.deleteDoctorImages(27);

        assertFalse(Files.exists(front));
        assertFalse(Files.exists(back));
        assertFalse(Files.exists(license));
        assertTrue(Files.exists(unrelated));
    }

    @Test
    void resolvingMissingImageDoesNotCreateDoctorDirectory() {
        System.setProperty("app.upload-dir", uploadRoot.toString());

        assertNull(FileStorageUtil.resolveDoctorImage(27, "cccd.jpg"));
        assertFalse(Files.exists(uploadRoot.resolve("27")));
    }
}
