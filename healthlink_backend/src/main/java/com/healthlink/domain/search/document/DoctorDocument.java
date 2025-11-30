package com.healthlink.domain.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "doctors")
public class DoctorDocument {

    @Id
    private String id; // UUID as string

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String photoUrl;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Keyword)
    private String specialty;

    @Field(type = FieldType.Text)
    private String qualifications;

    @Field(type = FieldType.Text)
    private String bio;

    @Field(type = FieldType.Integer)
    private Integer experienceYears;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Keyword)
    private String area;

    @Field(type = FieldType.Double)
    private Double averageRating;

    @Field(type = FieldType.Integer)
    private Integer totalReviews;

    @Field(type = FieldType.Keyword)
    private BigDecimal consultationFee;

    @Field(type = FieldType.Keyword)
    private List<String> facilityNames;

    @Field(type = FieldType.Keyword)
    private List<String> languages;

    @Field(type = FieldType.Keyword)
    private List<String> services;

    @Field(type = FieldType.Boolean)
    private Boolean isAvailable; // Has availability in next 7 days

    @Field(type = FieldType.Boolean)
    private Boolean isAvailableForTelemedicine;

    @Field(type = FieldType.Keyword)
    private String organizationName;

    @Field(type = FieldType.Double)
    private Double minConsultationFee;

    @Field(type = FieldType.Double)
    private Double maxConsultationFee;

    @Field(type = FieldType.Keyword)
    private String feeCurrency;

    @Field(type = FieldType.Nested, includeInParent = true)
    private List<FacilitySummaryDocument> facilities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacilitySummaryDocument {
        @Field(type = FieldType.Keyword)
        private String id;

        @Field(type = FieldType.Text)
        private String name;

        @Field(type = FieldType.Text)
        private String address;

        @Field(type = FieldType.Keyword)
        private String city;

        @Field(type = FieldType.Keyword)
        private String phoneNumber;

        @Field(type = FieldType.Double)
        private Double latitude;

        @Field(type = FieldType.Double)
        private Double longitude;
    }
}
