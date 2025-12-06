package com.healthlink.domain.user.repository;

import com.healthlink.domain.user.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    
    Optional<Doctor> findByPmdcId(String pmdcId);
    
    boolean existsByPmdcId(String pmdcId);
    
    List<Doctor> findBySpecialization(String specialization);
    
    @Query("SELECT d FROM Doctor d WHERE d.approvalStatus = 'APPROVED' AND d.isActive = true AND d.deletedAt IS NULL")
    List<Doctor> findAllVerifiedAndApproved();
    
    @Query("SELECT d FROM Doctor d WHERE d.specialization LIKE %:specialization% AND d.approvalStatus = 'APPROVED' AND d.deletedAt IS NULL")
    List<Doctor> searchBySpecialization(@Param("specialization") String specialization);
    
    @Query("SELECT d FROM Doctor d WHERE d.averageRating >= :minRating AND d.approvalStatus = 'APPROVED' AND d.isActive = true AND d.deletedAt IS NULL ORDER BY d.averageRating DESC")
    List<Doctor> findByMinimumRating(@Param("minRating") Double minRating);
    
    @Query("SELECT d FROM Doctor d WHERE (LOWER(d.specialization) LIKE LOWER(CONCAT('%', :query, '%'))) AND d.approvalStatus = 'APPROVED' AND d.isActive = true AND d.deletedAt IS NULL")
    List<Doctor> searchDoctors(@Param("query") String query);
}
