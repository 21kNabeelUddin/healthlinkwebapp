package com.healthlink.domain.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class MedicalRecordRequest {
    private UUID patientId;
    @NotBlank
    @Size(max = 180)
    private String title;
    @Size(max = 500)
    private String summary;
    @NotBlank
    private String details;
    private String attachmentUrl;
}
