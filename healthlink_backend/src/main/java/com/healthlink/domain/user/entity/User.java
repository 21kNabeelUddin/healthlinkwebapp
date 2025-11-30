package com.healthlink.domain.user.entity;

import com.healthlink.common.entity.BaseEntity;
import com.healthlink.domain.user.enums.ApprovalStatus;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.security.encryption.Encrypted;
import com.healthlink.security.encryption.FieldEncryptionConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base User entity - parent for all user types
 * Uses Single Table Inheritance strategy with soft delete support
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_role", columnList = "role"),
        @Index(name = "idx_user_approval_status", columnList = "approval_status"),
        @Index(name = "idx_user_deleted_at", columnList = "deleted_at")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@org.hibernate.annotations.FilterDef(name = "deletedFilter", defaultCondition = "deleted_at IS NULL")
@org.hibernate.annotations.Filter(name = "deletedFilter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class User extends BaseEntity {

    @Email(message = "Invalid email format")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$", message = "Email must be valid")
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "username", unique = true, length = 100)
    private String username; // For admin accounts (password-based login)

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", length = 200)
    private String fullName; // Alternative to firstName+lastName for admin accounts

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "first_name", length = 100)
    @Convert(converter = FieldEncryptionConverter.class)
    @Encrypted
    private String firstName;

    @Column(name = "last_name", length = 100)
    @Convert(converter = FieldEncryptionConverter.class)
    @Encrypted
    private String lastName;

    @Column(name = "phone_number", length = 255)
    @Convert(converter = FieldEncryptionConverter.class)
    @Encrypted
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage = "en"; // en or ur

    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;

    @Column(name = "two_factor_secret", length = 255)
    private String twoFactorSecret;

    @Column(name = "tokens_revoked_at")
    private Instant tokensRevokedAt; // Forced logout marker for security

    @Column(name = "deleted_by")
    private UUID deletedBy;

    // Account lockout tracking
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private Instant accountLockedUntil;

    public String getFullName() {
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return email; // fallback
    }

    public boolean canLogin() {
        if (!isActive || !isEmailVerified || approvalStatus != ApprovalStatus.APPROVED) {
            return false;
        }
        // Check if account is locked
        if (accountLockedUntil != null && Instant.now().isBefore(accountLockedUntil)) {
            return false;
        }
        return true;
    }

    public void markTokensRevoked() {
        this.tokensRevokedAt = Instant.now();
    }

    public void softDelete(UUID deletedByUserId) {
        setDeletedAt(LocalDateTime.now());
        this.deletedBy = deletedByUserId;
        this.isActive = false;
    }

    public boolean isDeleted() {
        return getDeletedAt() != null;
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        // Lock account after 5 failed attempts for 30 minutes
        if (this.failedLoginAttempts >= 5) {
            this.accountLockedUntil = Instant.now().plusSeconds(1800); // 30 minutes
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }
}
