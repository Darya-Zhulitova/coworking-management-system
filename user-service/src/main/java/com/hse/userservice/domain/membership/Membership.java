package com.hse.userservice.domain.membership;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_memberships",
        uniqueConstraints = {@UniqueConstraint(
                name = "uk_membership_user_coworking",
                columnNames = {"user_id", "coworking_id"}
        )}
)
@Getter
@Setter
@NoArgsConstructor
public class Membership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coworking_id", nullable = false)
    private Long coworkingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MembershipStatus status = MembershipStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime approvedAt;

    private LocalDateTime blockedAt;
}
