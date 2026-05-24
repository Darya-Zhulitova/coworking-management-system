package com.hse.userservice.support.builder;

import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.domain.MembershipStatus;

import java.time.LocalDateTime;

public class MembershipTestBuilder {
    private Long userId = 1L;
    private Long coworkingId = 1L;
    private MembershipStatus status = MembershipStatus.ACTIVE;
    private LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
    private LocalDateTime approvedAt = LocalDateTime.of(2026, 1, 1, 10, 5);
    private LocalDateTime blockedAt;

    public static MembershipTestBuilder membership() {
        return new MembershipTestBuilder();
    }

    public MembershipTestBuilder userId(Long userId) {
        this.userId = userId;
        return this;
    }

    public MembershipTestBuilder coworkingId(Long coworkingId) {
        this.coworkingId = coworkingId;
        return this;
    }

    public MembershipTestBuilder status(MembershipStatus status) {
        this.status = status;
        return this;
    }

    public MembershipTestBuilder createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public MembershipTestBuilder approvedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
        return this;
    }

    public MembershipTestBuilder blockedAt(LocalDateTime blockedAt) {
        this.blockedAt = blockedAt;
        return this;
    }

    public Membership build() {
        Membership membership = new Membership();
        membership.setUserId(userId);
        membership.setCoworkingId(coworkingId);
        membership.setStatus(status);
        membership.setCreatedAt(createdAt);
        membership.setApprovedAt(approvedAt);
        membership.setBlockedAt(blockedAt);
        return membership;
    }
}
