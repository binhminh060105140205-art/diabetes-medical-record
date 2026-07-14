package vn.diabetes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan(basePackages = "controllers")
public class DiabetesMedicalRecordApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiabetesMedicalRecordApplication.class, args);
    }
}
