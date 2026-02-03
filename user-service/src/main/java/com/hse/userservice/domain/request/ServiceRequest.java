package com.hse.userservice.domain.request;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_service_requests")
@Getter
@Setter
@NoArgsConstructor
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long membershipId;

    private Long placeId;

    private Long bookingId;

    @Column(nullable = false, length = 64)
    private String category;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ServiceRequestStatus status = ServiceRequestStatus.NEW;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}