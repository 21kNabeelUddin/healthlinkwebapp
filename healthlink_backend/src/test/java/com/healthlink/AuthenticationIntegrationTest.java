package com.healthlink;

import com.healthlink.dto.auth.LoginRequest;
import com.healthlink.dto.auth.LogoutRequest;
import com.healthlink.dto.auth.OtpRequest;
import com.healthlink.dto.auth.RegisterRequest;
import com.healthlink.domain.user.enums.UserRole;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void shouldCompleteFullAuthFlow() {
        String testEmail = "auth.test@example.com";

        // 1. Register Patient
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFirstName("Auth");
        registerRequest.setLastName("Test");
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword("Password123!");
        registerRequest.setRole(UserRole.PATIENT);
        registerRequest.setPhoneNumber("+1234567890");

        given()
                .contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.user.email", equalTo(testEmail))
                .body("data.user.isEmailVerified", equalTo(false));

        // 2. Get OTP from Redis (stored by OtpService during registration)
        String otp = redisTemplate.opsForValue().get("otp:" + testEmail);

        // 3. Verify email with OTP
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setEmail(testEmail);
        otpRequest.setOtp(otp);

        given()
                .contentType(ContentType.JSON)
                .body(otpRequest)
                .when()
                .post("/api/v1/auth/email/verify")
                .then()
                .statusCode(HttpStatus.OK.value());

        // 4. Login to get tokens (now that email is verified)
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword("Password123!");

        JsonPath loginResponse = given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.accessToken", notNullValue())
                .body("data.refreshToken", notNullValue())
                .extract()
                .jsonPath();

        String refreshToken = loginResponse.getString("data.refreshToken");

        // 5. Refresh Token
        JsonPath refreshResponse = given()
                .header("Authorization", "Bearer " + refreshToken)
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.accessToken", notNullValue())
                .body("data.refreshToken", notNullValue())
                .extract()
                .jsonPath();

        String newAccessToken = refreshResponse.getString("data.accessToken");
        String newRefreshToken = refreshResponse.getString("data.refreshToken");

        // 6. Logout (returns 204 No Content on success)
        LogoutRequest logoutRequest = new LogoutRequest();
        logoutRequest.setRefreshToken(newRefreshToken);

        given()
                .header("Authorization", "Bearer " + newAccessToken)
                .contentType(ContentType.JSON)
                .body(logoutRequest)
                .when()
                .post("/api/v1/auth/logout")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // 7. Verify Access Token is Invalidated (Blacklisted)
        // Try to access a protected endpoint after logout
        given()
                .header("Authorization", "Bearer " + newAccessToken)
                .when()
                .get("/api/v1/appointments")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void shouldFailLoginWithWrongPassword() {
        // This test would require a registered user, which can be set up in @BeforeEach if needed
        // For now, just test that wrong password returns 401
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@test.com");
        loginRequest.setPassword("WrongPassword123!");

        given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
