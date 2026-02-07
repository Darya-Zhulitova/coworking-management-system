package com.hse.adminservice.entity;

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