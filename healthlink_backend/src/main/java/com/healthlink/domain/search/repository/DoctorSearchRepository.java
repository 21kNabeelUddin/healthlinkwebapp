package com.healthlink.domain.search.repository;

import com.healthlink.domain.search.document.DoctorDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorSearchRepository extends ElasticsearchRepository<DoctorDocument, String> {
    
    List<DoctorDocument> findByNameContainingOrSpecialtyContaining(String name, String specialty);

    List<DoctorDocument> findBySpecialtyAndCityOrderByAverageRatingDesc(String specialty, String city);
    
    List<DoctorDocument> findBySpecialtyOrderByAverageRatingDesc(String specialty);
    
    List<DoctorDocument> findByCityOrderByAverageRatingDesc(String city);
    
    List<DoctorDocument> findByAverageRatingGreaterThanEqualOrderByAverageRatingDesc(Double minRating);
    
    List<DoctorDocument> findByIsAvailableTrueOrderByAverageRatingDesc();
}
