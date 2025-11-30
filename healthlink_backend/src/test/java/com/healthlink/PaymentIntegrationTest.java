package com.healthlink;

import com.healthlink.domain.appointment.dto.CreateAppointmentRequest;
import com.healthlink.domain.appointment.dto.InitiatePaymentRequest;
import com.healthlink.domain.appointment.entity.PaymentMethod;
import com.healthlink.domain.organization.entity.Facility;
import com.healthlink.domain.organization.repository.FacilityRepository;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.enums.ApprovalStatus;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.dto.auth.LoginRequest;
import com.healthlink.dto.auth.OtpRequest;
import com.healthlink.dto.auth.RegisterRequest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class PaymentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private UUID doctorId;
    private UUID facilityId;
    private UUID appointmentId;
    private String patientAccessToken;

    @BeforeEach
    void setUp() {
        // Register Doctor
        RegisterRequest doctorRequest = new RegisterRequest();
        doctorRequest.setFirstName("Rich");
        doctorRequest.setLastName("Doctor");
        doctorRequest.setEmail("rich@hospital.com");
        doctorRequest.setPassword("Money123!");
        doctorRequest.setRole(UserRole.DOCTOR);
        doctorRequest.setPhoneNumber("+1555666777");
        doctorRequest.setPmdcId("PMDC-67890");
        doctorRequest.setSpecialization("Cardiology");

        given()
                .contentType(ContentType.JSON)
                .body(doctorRequest)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Auto-approve doctor and verify email for testing
        Doctor doctor = (Doctor) userRepository.findByEmail("rich@hospital.com").orElseThrow();
        doctorId = doctor.getId();
        doctor.setApprovalStatus(ApprovalStatus.APPROVED);
        doctor.setIsEmailVerified(true);
        doctor.setSlotDurationMinutes(30);
        doctor.setConsultationFee(new BigDecimal("100.00"));
        userRepository.save(doctor);

        // Create a facility for the doctor
        Facility facility = new Facility();
        facility.setName("Rich Clinic");
        facility.setDoctorOwner(doctor);
        facility.setAddress("456 Money Lane, Karachi");
        facility.setActive(true);
        facility = facilityRepository.save(facility);
        facilityId = facility.getId();

        // Login as doctor to get access token
        LoginRequest doctorLogin = new LoginRequest();
        doctorLogin.setEmail("rich@hospital.com");
        doctorLogin.setPassword("Money123!");

        JsonPath doctorLoginResponse = given()
                .contentType(ContentType.JSON)
                .body(doctorLogin)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.accessToken", notNullValue())
                .extract()
                .jsonPath();

        // Doctor access token retrieved but not used in this test
        doctorLoginResponse.getString("data.accessToken");

        // Register Patient
        RegisterRequest patientRequest = new RegisterRequest();
        patientRequest.setFirstName("Poor");
        patientRequest.setLastName("Patient");
        patientRequest.setEmail("poor@home.com");
        patientRequest.setPassword("Debt12345!");
        patientRequest.setRole(UserRole.PATIENT);
        patientRequest.setPhoneNumber("+1555666777");

        given()
                .contentType(ContentType.JSON)
                .body(patientRequest)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Get OTP from Redis and verify email
        String patientOtp = redisTemplate.opsForValue().get("otp:poor@home.com");
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setEmail("poor@home.com");
        otpRequest.setOtp(patientOtp);

        given()
                .contentType(ContentType.JSON)
                .body(otpRequest)
                .when()
                .post("/api/v1/auth/email/verify")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Login as patient to get access token
        LoginRequest patientLogin = new LoginRequest();
        patientLogin.setEmail("poor@home.com");
        patientLogin.setPassword("Debt12345!");

        JsonPath patientLoginResponse = given()
                .contentType(ContentType.JSON)
                .body(patientLogin)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.accessToken", notNullValue())
                .extract()
                .jsonPath();

        patientAccessToken = patientLoginResponse.getString("data.accessToken");

        // Book Appointment
        CreateAppointmentRequest bookRequest = new CreateAppointmentRequest();
        bookRequest.setDoctorId(doctorId);
        bookRequest.setFacilityId(facilityId);
        bookRequest.setAppointmentTime(LocalDateTime.now().plusDays(1));
        bookRequest.setReasonForVisit("Consultation");

        JsonPath appointmentResponse = given()
                .header("Authorization", "Bearer " + patientAccessToken)
                .contentType(ContentType.JSON)
                .body(bookRequest)
                .when()
                .post("/api/v1/appointments")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.id", notNullValue())
                .extract()
                .jsonPath();

        appointmentId = UUID.fromString(appointmentResponse.getString("data.id"));
    }

    @Test
    void shouldInitiatePayment() {
        // Initiate Payment (Patient)
        InitiatePaymentRequest initRequest = new InitiatePaymentRequest();
        initRequest.setAppointmentId(appointmentId);
        initRequest.setAmount(new BigDecimal("100.00"));
        initRequest.setMethod(PaymentMethod.MOBILE_WALLET);

        given()
                .header("Authorization", "Bearer " + patientAccessToken)
                .contentType(ContentType.JSON)
                .body(initRequest)
                .when()
                .post("/api/v1/payments/initiate")
                .then()
                .log().body()
                .statusCode(HttpStatus.OK.value())
                .body("data.status", equalTo("PENDING_VERIFICATION"));
    }
}
