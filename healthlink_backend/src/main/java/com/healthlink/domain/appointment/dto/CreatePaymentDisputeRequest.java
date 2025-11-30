package com.healthlink.domain.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class CreatePaymentDisputeRequest {
    @NotNull
    private UUID verificationId;

    @Size(max = 1000)
    private String notes;
}
