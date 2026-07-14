package controllers;

import dal.UserDAO;
import dal.PatientDAO;
import models.User;
import models.Patient;

import java.io.IOException;
import java.text.SimpleDateFormat;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "EditProfileController", urlPatterns = {"/EditProfile"})
public class EditProfileController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/Login");
            return;
        }
        try {
            UserDAO dao = new UserDAO();
            User user = dao.getById(currentUser.getUserId());
            request.setAttribute("profileUser", user);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("profileUser", currentUser);
        }
        request.getRequestDispatcher("views/editProfile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/Login");
            return;
        }

        request.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");
        String email = request.getParameter("email");
        String dobStr = request.getParameter("dob");
        String gender = request.getParameter("gender");
        String cccd = request.getParameter("cccd");
        String password = request.getParameter("password");

        try {
            UserDAO dao = new UserDAO();
            User userToUpdate = dao.getById(currentUser.getUserId());
            
            if (userToUpdate != null) {
                userToUpdate.setUsername(username);
                userToUpdate.setFullName(fullName);
                userToUpdate.setPhone(phone);
                userToUpdate.setEmail(email);
                
                if (dobStr != null && !dobStr.trim().isEmpty()) {
                    java.util.Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(dobStr);
                    userToUpdate.setDob(parsedDate);
                } else {
                    userToUpdate.setDob(null);
                }
                
                userToUpdate.setGender(gender);
                userToUpdate.setCccd(cccd);

                if (password != null && !password.trim().isEmpty()) {
                    userToUpdate.setPassword(password);
                }

                dao.updateProfile(userToUpdate);

                if ("PATIENT".equalsIgnoreCase(userToUpdate.getRole())) {
                    try {
                        PatientDAO pDao = new PatientDAO();
                        Patient p = pDao.getByUserId(userToUpdate.getUserId());
                        if (p != null) {
                            p.setFullName(fullName);
                            p.setPhone(phone);
                            if (dobStr != null && !dobStr.trim().isEmpty()) {
                                p.setDateOfBirth(java.time.LocalDate.parse(dobStr));
                            } else {
                                p.setDateOfBirth(null);
                            }
                            p.setGender(gender);
                            p.setNationalId(cccd);
                            pDao.updateBasicProfile(p);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                session.setAttribute("user", userToUpdate);
                request.setAttribute("successMessage", "Cập nhật thông tin thành công!");
                request.setAttribute("profileUser", userToUpdate);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Đã xảy ra lỗi khi cập nhật: " + e.getMessage());
            request.setAttribute("profileUser", currentUser);
        }

        request.getRequestDispatcher("views/editProfile.jsp").forward(request, response);
    }
}