package com.hse.adminservice.space.floor.domain;

import com.hse.adminservice.coworking.domain.Coworking;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "floors", uniqueConstraints = {@UniqueConstraint(
        name = "uk_floor_coworking_index_archived", columnNames = {"coworking_id", "floor_index", "archived"}
)}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Floor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coworking_id", nullable = false)
    private Coworking coworking;

    @Column(nullable = false)
    private String name;

    @Column(name = "floor_index", nullable = false)
    private Integer index;

    @Column(name = "image_file_id")
    private String imageFileId;

    @Column(name = "full_image_file_id")
    private String fullImageFileId;

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
