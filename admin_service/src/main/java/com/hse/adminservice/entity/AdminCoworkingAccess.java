package com.hse.adminservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "admin_coworking_access", uniqueConstraints = @UniqueConstraint(
        name = "uk_admin_coworking_access_admin_coworking", columnNames = {"admin_user_id", "coworking_id"}
)
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCoworkingAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private AdminUser adminUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coworking_id", nullable = false)
    private Coworking coworking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminCoworkingAssignmentType assignmentType;

    @Enumerated(EnumType.STRING)
    @Column
    private AdminCoworkingRole role;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
