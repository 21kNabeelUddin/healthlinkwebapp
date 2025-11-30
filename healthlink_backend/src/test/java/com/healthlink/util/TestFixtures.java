package com.healthlink.util;

import com.healthlink.dto.auth.LoginRequest;
import com.healthlink.dto.auth.OtpRequest;
import com.healthlink.dto.auth.RegisterRequest;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Patient;
import com.healthlink.domain.user.enums.ApprovalStatus;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.repository.UserRepository;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@Component
public class TestFixtures {

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public TestFixtures(UserRepository userRepository, RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    public String registerAndLoginPatient(String email, String password) {
        // Register
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Test");
        request.setLastName("Patient");
        request.setEmail(email);
        request.setPassword(password);
        request.setRole(UserRole.PATIENT);
        request.setPhoneNumber("+1555000000");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Verify Email
        String otp = redisTemplate.opsForValue().get("otp:" + email);
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setEmail(email);
        otpRequest.setOtp(otp);

        given()
                .contentType(ContentType.JSON)
                .body(otpRequest)
                .when()
                .post("/api/v1/auth/email/verify")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Login
        return login(email, password);
    }

    public String registerAndLoginDoctor(String email, String password) {
        // Register
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Test");
        request.setLastName("Doctor");
        request.setEmail(email);
        request.setPassword(password);
        request.setRole(UserRole.DOCTOR);
        request.setPhoneNumber("+1555111111");
        request.setPmdcId("PMDC-" + UUID.randomUUID().toString().substring(0, 5));
        request.setSpecialization("General");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Auto-approve and verify
        Doctor doctor = (Doctor) userRepository.findByEmail(email).orElseThrow();
        doctor.setApprovalStatus(ApprovalStatus.APPROVED);
        doctor.setIsEmailVerified(true);
        doctor.setSlotDurationMinutes(30);
        userRepository.save(doctor);

        // Login
        return login(email, password);
    }

    public String login(String email, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        JsonPath response = given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.accessToken", notNullValue())
                .extract()
                .jsonPath();

        return response.getString("data.accessToken");
    }

    public Patient getPatient(String email) {
        return (Patient) userRepository.findByEmail(email).orElseThrow();
    }

    public Doctor getDoctor(String email) {
        return (Doctor) userRepository.findByEmail(email).orElseThrow();
    }
}
