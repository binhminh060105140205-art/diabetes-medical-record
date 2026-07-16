package vn.diabetes.service;
import java.time.LocalDate;
public interface PatientRegistrationGateway {int register(String username,String password,String fullName,String phone,String email,LocalDate dob,String gender,String address,String insuranceNo,Integer createdBy);}
