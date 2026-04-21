package com.hse.userservice.domain.payrequest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pay_requests")
@Getter
@Setter
@NoArgsConstructor
public class PayRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "membership_id", nullable = false)
    private Long membershipId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PayRequestStatus status = PayRequestStatus.PENDING;

    @Column(length = 1000)
    private String userComment;

    @Column(length = 1000)
    private String adminComment;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
