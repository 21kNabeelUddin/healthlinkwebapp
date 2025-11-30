package com.healthlink.domain.user.repository;

import com.healthlink.domain.user.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    @Query("select o from Organization o where o.id = :userId")
    Optional<Organization> findByUserId(UUID userId);
}
