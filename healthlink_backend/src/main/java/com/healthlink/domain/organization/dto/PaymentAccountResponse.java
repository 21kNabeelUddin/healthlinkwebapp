package com.healthlink.domain.organization.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class PaymentAccountResponse {
    private UUID id;
    private UUID doctorId;
    private UUID organizationId;
    private String accountDetails;
}