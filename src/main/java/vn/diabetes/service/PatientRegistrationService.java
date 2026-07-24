package vn.diabetes.service;

import java.time.LocalDate;
import vn.diabetes.validation.Validators;

/** Shared registration use case for public signup and staff reception. */
public class PatientRegistrationService {
    private final PatientRegistrationGateway gateway;

    public PatientRegistrationService(PatientRegistrationGateway gateway) {
        this.gateway = gateway;
    }

    public Result register(Command command) {
        String username = Validators.username(command.username());
        String passwordLabel = command.createdBy() == null ? "Mật khẩu" : "Mật khẩu tạm thời";
        String password = Validators.password(command.password(), passwordLabel);
        if (command.confirmPassword() != null && !password.equals(command.confirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu nhập lại không khớp.");
        }

        String fullName = Validators.fullName(command.fullName());
        String phone = Validators.phone(command.phone());
        String email = Validators.email(command.email(), command.createdBy() == null);
        LocalDate dateOfBirth = Validators.dateOfBirth(command.dateOfBirth(), true);
        String gender = Validators.gender(command.gender());
        String address = Validators.requiredAddress(command.address());
        String insurance = Validators.insurance(command.insuranceNo());

        int patientId = gateway.register(username, password, fullName, phone,
                email, dateOfBirth, gender, address, insurance, command.createdBy());
        return new Result(patientId, username, password, fullName, email);
    }

    public record Command(String username, String password, String confirmPassword,
            String fullName, String phone, String email, String dateOfBirth,
            String gender, String address, String insuranceNo, Integer createdBy) {}

    public record Result(int patientId, String username, String temporaryPassword,
            String fullName, String email) {}
}
