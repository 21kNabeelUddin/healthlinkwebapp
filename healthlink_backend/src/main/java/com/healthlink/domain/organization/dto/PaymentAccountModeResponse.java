package com.healthlink.domain.organization.dto;

import lombok.Builder;
import lombok.Data;
import com.healthlink.domain.user.entity.PaymentAccountMode;
import java.util.UUID;

@Data
@Builder
public class PaymentAccountModeResponse {
    private UUID organizationId;
    private PaymentAccountMode mode;
}