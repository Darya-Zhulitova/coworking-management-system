package com.hse.adminservice.rbac.domain;

import com.hse.adminservice.coworking.domain.Coworking;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_coworking_name", columnNames = {"coworking_id", "name"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coworking_id", nullable = false)
    private Coworking coworking;

    @Column(nullable = false)
    private String name;

    @Column(name = "grants_raw", nullable = false, length = 2000)
    private String grantsRaw;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
