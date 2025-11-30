package com.healthlink.domain.organization.service;

import com.healthlink.domain.organization.dto.PaymentAccountModeResponse;
import com.healthlink.domain.organization.dto.PaymentAccountResponse;
import com.healthlink.domain.organization.entity.PaymentAccount;
import com.healthlink.domain.organization.repository.PaymentAccountRepository;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Organization;
import com.healthlink.domain.user.entity.PaymentAccountMode;
import com.healthlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentAccountService {

    private final PaymentAccountRepository paymentAccountRepository;
    private final UserRepository userRepository;

    public PaymentAccountModeResponse getMode(UUID organizationId) {
        Organization org = (Organization) userRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        return PaymentAccountModeResponse.builder()
                .organizationId(org.getId())
                .mode(org.getPaymentAccountMode())
                .build();
    }

    public PaymentAccountModeResponse setMode(UUID organizationId, PaymentAccountMode mode) {
        Organization org = (Organization) userRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        org.setPaymentAccountMode(mode);
        return PaymentAccountModeResponse.builder()
                .organizationId(org.getId())
                .mode(org.getPaymentAccountMode())
                .build();
    }

    public PaymentAccountResponse setDoctorAccount(UUID doctorId, String accountDetails) {
        Doctor doc = (Doctor) userRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        PaymentAccount acct = paymentAccountRepository.findByDoctorId(doctorId).orElse(new PaymentAccount());
        acct.setDoctor(doc);
        acct.setAccountDetails(accountDetails);
        return toDto(paymentAccountRepository.save(acct));
    }

    public PaymentAccountResponse setCentralizedAccount(UUID organizationId, UUID doctorId, String accountDetails) {
        Organization org = (Organization) userRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        Doctor doc = (Doctor) userRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        PaymentAccount acct = paymentAccountRepository.findByOrganizationIdAndDoctorId(organizationId, doctorId)
                .orElse(new PaymentAccount());
        acct.setOrganization(org);
        acct.setDoctor(doc);
        acct.setAccountDetails(accountDetails);
        return toDto(paymentAccountRepository.save(acct));
    }

    public PaymentAccountResponse resolveAccountForDoctor(UUID organizationId, UUID doctorId) {
        Organization org = (Organization) userRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        if (org.getPaymentAccountMode() == PaymentAccountMode.CENTRALIZED) {
            return paymentAccountRepository.findByOrganizationIdAndDoctorId(organizationId, doctorId)
                    .map(this::toDto)
                    .orElse(null);
        }
        return paymentAccountRepository.findByDoctorId(doctorId)
                .map(this::toDto)
                .orElse(null);
    }

    private PaymentAccountResponse toDto(PaymentAccount acct) {
        return PaymentAccountResponse.builder()
                .id(acct.getId())
                .doctorId(acct.getDoctor() != null ? acct.getDoctor().getId() : null)
                .organizationId(acct.getOrganization() != null ? acct.getOrganization().getId() : null)
                .accountDetails(acct.getAccountDetails())
                .build();
    }
}