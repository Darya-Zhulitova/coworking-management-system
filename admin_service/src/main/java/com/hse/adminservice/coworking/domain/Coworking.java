package com.hse.adminservice.coworking.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coworkings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coworking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, length = 255)
    private String workingHoursLabel;

    @Column(length = 255)
    private String heroTitle;

    @Column(length = 2000)
    private String heroText;

    @Lob
    @Column(nullable = false)
    private String imageUrlsJson;

    @Column(nullable = false)
    private Integer schedule;

    @Column(nullable = false)
    private Long ownerId;

    @Column(name = "auto_approve_membership", nullable = false)
    private Boolean autoApproveMembership;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Boolean archived;

    @Column(nullable = false)
    private Long configurationVersion;

    private LocalDateTime archivedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
