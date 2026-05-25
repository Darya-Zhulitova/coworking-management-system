package com.hse.adminservice.space.place.domain;

import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.space.floor.domain.Floor;
import com.hse.adminservice.space.placetype.domain.PlaceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "places", uniqueConstraints = {@UniqueConstraint(
        name = "uk_place_floor_name_archived", columnNames = {"floor_id", "name", "archived"}
)}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coworking_id", nullable = false, updatable = false)
    private Coworking coworking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false, updatable = false)
    private Floor floor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_type_id", nullable = false, updatable = false)
    private PlaceType placeType;

    @Column(nullable = false)
    private String name;

    @Column(name = "loc_x", precision = 10, scale = 4)
    private BigDecimal locX;

    @Column(name = "loc_y", precision = 10, scale = 4)
    private BigDecimal locY;

    @Column(name = "image_file_id", length = 500)
    private String imageFileId;

    @Column(name = "preview_image_file_id", length = 500)
    private String previewImageFileId;

    @Column(name = "full_image_file_id", length = 500)
    private String fullImageFileId;

    @Column(name = "amenities_raw", length = 1000)
    private String amenitiesRaw;

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
