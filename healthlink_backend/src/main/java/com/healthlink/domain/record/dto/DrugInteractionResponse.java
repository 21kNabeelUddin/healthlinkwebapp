package com.healthlink.domain.record.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DrugInteractionResponse {
    private List<String> interactingPairs; // e.g. "DrugA+DrugB"
    private List<String> warnings; // Human-readable warnings
    private boolean severeInteractionDetected;
}
