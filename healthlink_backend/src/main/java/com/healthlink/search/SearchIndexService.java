package com.healthlink.search;

import com.healthlink.domain.record.repository.MedicalRecordRepository;
import com.healthlink.domain.record.repository.PrescriptionRepository;
import com.healthlink.domain.record.repository.LabOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SearchIndexService {
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final LabOrderRepository labOrderRepository;

    /**
     * Simple aggregated search (title/orderName only) with case-insensitive contains. In production,
     * replace with Elasticsearch indexing for relevance scoring and advanced querying.
     */
    public List<SearchHit> search(String query, java.util.UUID patientScope, boolean includeLabOrders) {
        String q = query.toLowerCase();
        Stream<SearchHit> records = medicalRecordRepository.findByPatientId(patientScope).stream()
                .filter(r -> r.getTitle() != null && r.getTitle().toLowerCase().contains(q))
                .map(r -> new SearchHit("MEDICAL_RECORD", r.getId().toString(), r.getTitle()));
        Stream<SearchHit> prescriptions = prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientScope).stream()
                .filter(p -> p.getTitle() != null && p.getTitle().toLowerCase().contains(q))
                .map(p -> new SearchHit("PRESCRIPTION", p.getId().toString(), p.getTitle()));
        Stream<SearchHit> labs = includeLabOrders ? labOrderRepository.findByPatientId(patientScope).stream()
                .filter(l -> l.getOrderName() != null && l.getOrderName().toLowerCase().contains(q))
                .map(l -> new SearchHit("LAB_ORDER", l.getId().toString(), l.getOrderName())) : Stream.empty();
        return Stream.concat(Stream.concat(records, prescriptions), labs).limit(200).toList();
    }

    public record SearchHit(String type, String id, String title) {}
}
