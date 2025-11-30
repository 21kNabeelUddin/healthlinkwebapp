package com.healthlink;

import com.healthlink.domain.appointment.entity.Appointment;
import com.healthlink.domain.appointment.entity.AppointmentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.organization.entity.Facility;
import com.healthlink.domain.organization.repository.FacilityRepository;
import com.healthlink.domain.record.dto.PrescriptionRequest;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Patient;
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
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class PrescriptionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @SuppressWarnings("unused")
    private UUID doctorId;
    @SuppressWarnings("unused")
    private UUID facilityId;
    private UUID appointmentId;
    private UUID patientId;
    private String doctorAccessToken;
    private String patientAccessToken;

    @BeforeEach
    void setUp() {
        // Register Doctor
        RegisterRequest doctorRequest = new RegisterRequest();
        doctorRequest.setFirstName("Stephen");
        doctorRequest.setLastName("Strange");
        doctorRequest.setEmail("strange@marvel.com");
        doctorRequest.setPassword("Magic123!");
        doctorRequest.setRole(UserRole.DOCTOR);
        doctorRequest.setPhoneNumber("+1555666777");
        doctorRequest.setPmdcId("PMDC-99999");
        doctorRequest.setSpecialization("Neurosurgery");

        given()
                .contentType(ContentType.JSON)
                .body(doctorRequest)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Auto-approve doctor and verify email for testing
        Doctor doctor = (Doctor) userRepository.findByEmail("strange@marvel.com").orElseThrow();
        doctorId = doctor.getId();
        doctor.setApprovalStatus(ApprovalStatus.APPROVED);
        doctor.setIsEmailVerified(true);
        userRepository.save(doctor);

        // Login as doctor to get access token
        LoginRequest doctorLogin = new LoginRequest();
        doctorLogin.setEmail("strange@marvel.com");
        doctorLogin.setPassword("Magic123!");

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

        // Register Patient
        RegisterRequest patientRequest = new RegisterRequest();
        patientRequest.setFirstName("Peter");
        patientRequest.setLastName("Parker");
        patientRequest.setEmail("peter@spiderman.com");
        patientRequest.setPassword("Web12345!");
        patientRequest.setRole(UserRole.PATIENT);
        patientRequest.setPhoneNumber("+1555666777");

        given()
                .contentType(ContentType.JSON)
                .body(patientRequest)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value());

        patientId = userRepository.findByEmail("peter@spiderman.com").orElseThrow().getId();

        // Get OTP from Redis and verify email
        String patientOtp = redisTemplate.opsForValue().get("otp:peter@spiderman.com");
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setEmail("peter@spiderman.com");
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
        patientLogin.setEmail("peter@spiderman.com");
        patientLogin.setPassword("Web12345!");

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

    private void createFacilityAndAppointment() {
        // Get the doctor and patient entities
        Doctor doctor = (Doctor) userRepository.findByEmail("strange@marvel.com").orElseThrow();
        Patient patient = (Patient) userRepository.findByEmail("peter@spiderman.com").orElseThrow();

        // Create a facility directly in DB
        Facility facility = new Facility();
        facility.setName("HealthLink Test Clinic");
        facility.setDoctorOwner(doctor);
        facility.setAddress("123 Test Street");
        facility.setActive(true);
        facility = facilityRepository.save(facility);
        facilityId = facility.getId();

        // Create an appointment directly in DB (for prescription to reference)
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setFacility(facility);
        appointment.setAppointmentTime(LocalDateTime.now().plusDays(1));
        appointment.setEndTime(LocalDateTime.now().plusDays(1).plusMinutes(30));
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setNotes("Initial consultation");
        appointment.setReasonForVisit("Spider bite treatment consultation");
        appointment = appointmentRepository.save(appointment);
        appointmentId = appointment.getId();
    }

    @Test
    void shouldCreateAndGetPrescription() {
        createFacilityAndAppointment();

        // 1. Create Prescription (Doctor)
        PrescriptionRequest request = new PrescriptionRequest();
        request.setPatientId(patientId);
        request.setAppointmentId(appointmentId);  // Required field
        request.setTitle("Spider bite treatment");
        request.setBody("Apply antivenom daily. Rest well.");
        request.setMedications(List.of("Antivenom"));

        JsonPath createResponse = given()
                .header("Authorization", "Bearer " + doctorAccessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/prescriptions")
                .then()
                .log().body()
                .statusCode(HttpStatus.OK.value())
                .body("data.id", notNullValue())
                .body("data.title", equalTo("Spider bite treatment"))
                .extract()
                .jsonPath();

        UUID prescriptionId = UUID.fromString(createResponse.getString("data.id"));

        // 2. Get Prescription (Patient)
        given()
                .header("Authorization", "Bearer " + patientAccessToken)
                .when()
                .get("/api/v1/prescriptions/" + prescriptionId)
                .then()
                .log().body()
                .statusCode(HttpStatus.OK.value())
                .body("data.title", equalTo("Spider bite treatment"));
    }
}
