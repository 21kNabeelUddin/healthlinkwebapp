package com.healthlink.domain.user.repository;

import com.healthlink.domain.user.entity.User;
import com.healthlink.domain.user.enums.ApprovalStatus;
import com.healthlink.domain.user.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    List<User> findByRoleAndApprovalStatus(UserRole role, ApprovalStatus approvalStatus);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.approvalStatus = 'PENDING' AND u.deletedAt IS NULL")
    List<User> findPendingApprovalsByRole(@Param("role") UserRole role);

    List<User> findByApprovalStatus(ApprovalStatus status);

    @Query("SELECT u FROM User u WHERE u.approvalStatus = :status AND u.role = :role AND u.deletedAt IS NULL")
    List<User> findByApprovalStatusAndRole(@Param("status") ApprovalStatus status, @Param("role") UserRole role);

    // Analytics
    // Organization analytics (explicit queries because User has no direct
    // organization relation)
    @Query("SELECT COUNT(d) FROM Doctor d WHERE d.deletedAt IS NULL AND d.isActive = true AND EXISTS (SELECT f.id FROM Facility f WHERE f.organization.id = :organizationId AND f.doctorOwner = d)")
    Integer countDoctorsByOrganization(@Param("organizationId") UUID organizationId);

    @Query("SELECT COUNT(s) FROM Staff s WHERE s.deletedAt IS NULL AND s.isActive = true AND s.addedByOrgId = :organizationId")
    Integer countStaffByOrganization(@Param("organizationId") UUID organizationId);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.isActive = true")
    List<User> findAllActive();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.deletedAt IS NULL")
    Long countByRole(@Param("role") UserRole role);

    @Query("SELECT s FROM Staff s WHERE s.addedByDoctorId = :doctorId AND s.deletedAt IS NULL AND s.isActive = true")
    List<User> findStaffByDoctorId(@Param("doctorId") UUID doctorId);
}
