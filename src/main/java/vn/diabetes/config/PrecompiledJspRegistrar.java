package vn.diabetes.config;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.stereotype.Component;

/** Uses JSP classes produced by the Render build and falls back to Jasper during local development. */
@Component
public class PrecompiledJspRegistrar implements ServletContextInitializer {
    private static final Map<String, String> PAGES = pages();

    @Override
    public void onStartup(ServletContext context) throws ServletException {
        for (Map.Entry<String, String> page : PAGES.entrySet()) {
            try {
                Class<? extends Servlet> servlet = Class.forName(page.getValue()).asSubclass(Servlet.class);
                var registration = context.addServlet("precompiled-" + page.getKey(), servlet);
                if (registration != null) {
                    registration.addMapping(page.getKey());
                }
            } catch (ClassNotFoundException ignored) {
                // spring-boot:run does not use the production precompile profile.
            }
        }
    }

    private static Map<String, String> pages() {
        Map<String, String> pages = new LinkedHashMap<>();
        pages.put("/index.jsp", "org.apache.jsp.index_jsp");
        pages.put("/error.jsp", "org.apache.jsp.error_jsp");
        String[] views = {
            "AdminCreateUser", "AdminDashboard", "AdminDoctorDetail", "ClinicWorkflow",
            "DeviceDataView", "DoctorDashboard", "DoctorPatientJournal", "doctorProfile",
            "editProfile", "footer", "header", "Login", "MedicalRecordForm",
            "PatientAppointmentsSimple", "PatientForm",
            "PatientJournal", "PatientList", "PatientTimeline", "PatientToday", "RecordDetail", "Register",
            "StaffDashboard", "topnav"
        };
        for (String view : views) {
            pages.put("/views/" + view + ".jsp", "org.apache.jsp.views." + view + "_jsp");
        }
        return pages;
    }
}
