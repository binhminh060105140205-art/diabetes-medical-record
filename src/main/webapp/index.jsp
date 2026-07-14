<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    Object userObj = session.getAttribute("user");
    if (userObj != null) {
        models.User u = (models.User) userObj;
        String role = u.getRole();
        if ("ADMIN".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/AdminDashboard");
        } else if ("STAFF".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/StaffDashboard");
        } else if ("DOCTOR".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/DoctorDashboard");
        } else if ("PATIENT".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/PatientDashboard");
        } else {
            response.sendRedirect(request.getContextPath() + "/Login");
        }
    } else {
        response.sendRedirect(request.getContextPath() + "/Login");
    }
%>
