package com.healthlink.domain.record.dto;

import lombok.Data;
import java.util.List;

@Data
public class DrugInteractionRequest {
    private List<String> drugNames; // Plain names or identifiers
}
