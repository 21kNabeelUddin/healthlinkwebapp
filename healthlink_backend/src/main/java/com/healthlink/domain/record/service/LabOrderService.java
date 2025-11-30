package com.healthlink.domain.record.service;

import com.healthlink.domain.record.dto.LabOrderResponse;
import com.healthlink.domain.record.entity.LabOrder;
import com.healthlink.domain.record.repository.LabOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LabOrderService {

    private final LabOrderRepository labOrderRepository;

    public LabOrderResponse createLabOrder(UUID patientId, String orderName, String description) {
        LabOrder labOrder = new LabOrder();
        labOrder.setPatientId(patientId);
        labOrder.setOrderName(orderName);
        labOrder.setDescription(description);
        LabOrder saved = labOrderRepository.save(labOrder);
        return toResponse(saved);
    }

    public List<LabOrderResponse> getLabOrdersByPatient(UUID patientId) {
        return labOrderRepository.findByPatientId(patientId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public LabOrderResponse attachResult(UUID id, String resultUrl) {
        LabOrder labOrder = labOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lab order not found"));
        labOrder.setResultUrl(resultUrl);
        LabOrder saved = labOrderRepository.save(labOrder);
        return toResponse(saved);
    }

    private LabOrderResponse toResponse(LabOrder labOrder) {
        return LabOrderResponse.builder()
                .id(labOrder.getId())
                .patientId(labOrder.getPatientId())
                .orderName(labOrder.getOrderName())
                .description(labOrder.getDescription())
                .orderedAt(labOrder.getOrderedAt())
                .resultUrl(labOrder.getResultUrl())
                .build();
    }
}
