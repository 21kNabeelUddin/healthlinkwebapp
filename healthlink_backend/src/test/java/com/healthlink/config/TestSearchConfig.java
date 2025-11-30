package com.healthlink.config;

import com.healthlink.domain.search.document.DoctorDocument;
import com.healthlink.domain.search.repository.DoctorSearchRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@TestConfiguration
public class TestSearchConfig {

    @Bean
    @Primary
    public DoctorSearchRepository doctorSearchRepository() {
        DoctorSearchRepository repository = Mockito.mock(DoctorSearchRepository.class);

        lenient().when(repository.save(any(DoctorDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(repository.findById(anyString()))
                .thenReturn(Optional.empty());
        lenient().when(repository.findAll())
                .thenReturn(Collections.emptyList());
        lenient().when(repository.findAll(any(Sort.class)))
                .thenReturn(Collections.emptyList());
        lenient().when(repository.findByNameContainingOrSpecialtyContaining(anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        lenient().when(repository.findBySpecialtyAndCityOrderByAverageRatingDesc(anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        lenient().when(repository.findBySpecialtyOrderByAverageRatingDesc(anyString()))
                .thenReturn(Collections.emptyList());
        lenient().when(repository.findByCityOrderByAverageRatingDesc(anyString()))
                .thenReturn(Collections.emptyList());
        lenient().when(repository.findByAverageRatingGreaterThanEqualOrderByAverageRatingDesc(anyDouble()))
                .thenReturn(Collections.emptyList());
        lenient().when(repository.findByIsAvailableTrueOrderByAverageRatingDesc())
                .thenReturn(Collections.emptyList());

        return repository;
    }
}