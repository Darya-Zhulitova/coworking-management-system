package com.hse.adminservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coworking_place_types", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"coworking_id", "code", "archived"}),
        @UniqueConstraint(columnNames = {"coworking_id", "name", "archived"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoworkingPlaceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coworking_id", nullable = false)
    private Coworking coworking;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Boolean archived;

    private LocalDateTime archivedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
