package com.healthlink.domain.organization.controller;

import com.healthlink.domain.organization.dto.FacilityRequest;
import com.healthlink.domain.organization.dto.FacilityResponse;
import com.healthlink.domain.organization.service.FacilityService;
import com.healthlink.security.annotation.PhiAccess;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.healthlink.security.model.CustomUserDetails;

@RestController
@RequestMapping("/api/v1/facilities")
@RequiredArgsConstructor
public class FacilitiesController {

    private final FacilityService facilityService;

    @PostMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('ORGANIZATION','ADMIN')")
    public FacilityResponse createForOrganization(@PathVariable UUID organizationId,
                                                   @Valid @RequestBody FacilityRequest request) {
        return facilityService.createForOrganization(organizationId, request);
    }

    @PostMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public FacilityResponse createForDoctor(@PathVariable UUID doctorId,
                                            @Valid @RequestBody FacilityRequest request,
                                            Authentication auth) {
        // IDOR: if doctor role ensure they are owner
        if (auth.getPrincipal() instanceof CustomUserDetails cud && cud.getId().equals(doctorId)) {
            return facilityService.createForDoctor(doctorId, request);
        } else if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return facilityService.createForDoctor(doctorId, request);
        }
        throw new RuntimeException("Unauthorized");
    }

    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('ORGANIZATION','ADMIN')")
    @PhiAccess(reason = "facility_list_org", entityType = FacilityResponse.class, idParam = "organizationId")
    public List<FacilityResponse> listOrg(@PathVariable UUID organizationId) {
        return facilityService.listForOrganization(organizationId);
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public List<FacilityResponse> listDoctor(@PathVariable UUID doctorId, Authentication auth) {
        // Allow patients to view clinics for any doctor (for booking purposes)
        // Doctors can only view their own clinics, admins can view any
        if (auth.getPrincipal() instanceof CustomUserDetails cud) {
            boolean isPatient = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isOwnDoctor = cud.getId().equals(doctorId) && 
                                  auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
            
            if (isPatient || isAdmin || isOwnDoctor) {
                return facilityService.listForDoctor(doctorId);
            }
        }
        throw new RuntimeException("Unauthorized");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ORGANIZATION','ADMIN')")
    public FacilityResponse update(@PathVariable UUID id, @Valid @RequestBody FacilityRequest request) {
        return facilityService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ORGANIZATION','ADMIN')")
    public void deactivate(@PathVariable UUID id) {
        facilityService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('DOCTOR','ORGANIZATION','ADMIN')")
    public void activate(@PathVariable UUID id) {
        facilityService.activate(id);
    }
}