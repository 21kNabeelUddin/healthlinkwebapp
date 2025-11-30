package com.healthlink;

import com.healthlink.domain.appointment.dto.CreateAppointmentRequest;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class AppointmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private UUID doctorId;
    private UUID facilityId;
    @SuppressWarnings("unused")
    private String doctorAccessToken;
    private String patientAccessToken;

    @BeforeEach
    void setUp() {
        // Register Doctor
        RegisterRequest doctorRequest = new RegisterRequest();
        doctorRequest.setFirstName("Gregory");
        doctorRequest.setLastName("House");
        doctorRequest.setEmail("house@hospital.com");
        doctorRequest.setPassword("Vicodin123!");
        doctorRequest.setRole(UserRole.DOCTOR);
        doctorRequest.setPhoneNumber("+1555666777");
        doctorRequest.setPmdcId("PMDC-12345");
        doctorRequest.setSpecialization("Diagnostics");

        given()
                .contentType(ContentType.JSON)
                .body(doctorRequest)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Auto-approve doctor and verify email for testing
        Doctor doctor = (Doctor) userRepository.findByEmail("house@hospital.com").orElseThrow();
        doctorId = doctor.getId();
        doctor.setApprovalStatus(ApprovalStatus.APPROVED);
        doctor.setIsEmailVerified(true);
        doctor.setSlotDurationMinutes(30); // Set slot duration for appointments
        userRepository.save(doctor);

        // Create a facility for the doctor
        Facility facility = new Facility();
        facility.setName("House Clinic");
        facility.setDoctorOwner(doctor);
        facility.setAddress("123 Medical Lane, Lahore");
        facility.setActive(true);
        facility = facilityRepository.save(facility);
        facilityId = facility.getId();

        // Login as doctor to get access token
        LoginRequest doctorLogin = new LoginRequest();
        doctorLogin.setEmail("house@hospital.com");
        doctorLogin.setPassword("Vicodin123!");

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

        doctorAccessToken = doctorLoginResponse.getString("data.accessToken");

        // Register Patient (requires OTP verification)
        RegisterRequest patientRequest = new RegisterRequest();
        patientRequest.setFirstName("Sick");
        patientRequest.setLastName("Patient");
        patientRequest.setEmail("sick@home.com");
        patientRequest.setPassword("Medicine123!");
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
        String patientOtp = redisTemplate.opsForValue().get("otp:sick@home.com");
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setEmail("sick@home.com");
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
        patientLogin.setEmail("sick@home.com");
        patientLogin.setPassword("Medicine123!");

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
    }

    @Test
    void shouldBookAppointment() {
        // Book Appointment with all required fields
        CreateAppointmentRequest bookRequest = new CreateAppointmentRequest();
        bookRequest.setDoctorId(doctorId);
        bookRequest.setFacilityId(facilityId);
        bookRequest.setAppointmentTime(LocalDateTime.now().plusDays(1));
        bookRequest.setReasonForVisit("Checkup");

        given()
                .header("Authorization", "Bearer " + patientAccessToken)
                .contentType(ContentType.JSON)
                .body(bookRequest)
                .when()
                .post("/api/v1/appointments")
                .then()
                .log().body()
                .statusCode(HttpStatus.OK.value())
                .body("data.id", notNullValue())
                .body("data.status", equalTo("PENDING_PAYMENT"));
    }

    @Test
    void shouldRejectAppointmentWithMissingFacilityId() {
        // Book Appointment without facility ID - should fail validation
        CreateAppointmentRequest bookRequest = new CreateAppointmentRequest();
        bookRequest.setDoctorId(doctorId);
        // facilityId intentionally omitted
        bookRequest.setAppointmentTime(LocalDateTime.now().plusDays(1));
        bookRequest.setReasonForVisit("Checkup");

        given()
                .header("Authorization", "Bearer " + patientAccessToken)
                .contentType(ContentType.JSON)
                .body(bookRequest)
                .when()
                .post("/api/v1/appointments")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void shouldRejectUnauthenticatedAppointmentBooking() {
        // Try to book without auth token
        CreateAppointmentRequest bookRequest = new CreateAppointmentRequest();
        bookRequest.setDoctorId(doctorId);
        bookRequest.setFacilityId(facilityId);
        bookRequest.setAppointmentTime(LocalDateTime.now().plusDays(1));

        given()
                .contentType(ContentType.JSON)
                .body(bookRequest)
                .when()
                .post("/api/v1/appointments")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
