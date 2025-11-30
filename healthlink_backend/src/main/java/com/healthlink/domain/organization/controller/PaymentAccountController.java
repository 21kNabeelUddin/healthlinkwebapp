package com.healthlink.domain.organization.controller;

import com.healthlink.domain.organization.dto.PaymentAccountModeResponse;
import com.healthlink.domain.organization.dto.PaymentAccountResponse;
import com.healthlink.domain.organization.service.PaymentAccountService;
import com.healthlink.domain.user.entity.PaymentAccountMode;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-accounts")
@RequiredArgsConstructor
public class PaymentAccountController {

    private final PaymentAccountService paymentAccountService;

    @GetMapping("/mode/{organizationId}")
    @PreAuthorize("hasAnyRole('ORGANIZATION','ADMIN','DOCTOR','STAFF')")
    public PaymentAccountModeResponse getMode(@PathVariable UUID organizationId) {
        return paymentAccountService.getMode(organizationId);
    }

    @PutMapping("/mode/{organizationId}")
    @PreAuthorize("hasAnyRole('ORGANIZATION','ADMIN')")
    public PaymentAccountModeResponse setMode(@PathVariable UUID organizationId,
                                              @RequestParam PaymentAccountMode mode) {
        return paymentAccountService.setMode(organizationId, mode);
    }

    @PutMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public PaymentAccountResponse setDoctorAccount(@PathVariable UUID doctorId,
                                                   @RequestParam @NotBlank String accountDetails) {
        return paymentAccountService.setDoctorAccount(doctorId, accountDetails);
    }

    @PutMapping("/centralized/{organizationId}/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('ORGANIZATION','ADMIN')")
    public PaymentAccountResponse setCentralizedAccount(@PathVariable UUID organizationId,
                                                        @PathVariable UUID doctorId,
                                                        @RequestParam @NotBlank String accountDetails) {
        return paymentAccountService.setCentralizedAccount(organizationId, doctorId, accountDetails);
    }

    @GetMapping("/resolve/{organizationId}/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','STAFF','ORGANIZATION','ADMIN')")
    public PaymentAccountResponse resolve(@PathVariable UUID organizationId,
                                          @PathVariable UUID doctorId) {
        return paymentAccountService.resolveAccountForDoctor(organizationId, doctorId);
    }
}