package com.hse.userservice.support.builder;

import com.hse.userservice.feature.balance.domain.PayRequest;
import com.hse.userservice.feature.balance.domain.PayRequestStatus;

import java.time.LocalDateTime;

public class PayRequestTestBuilder {
    private Long membershipId = 1L;
    private Long amount = 5000L;
    private PayRequestStatus status = PayRequestStatus.PENDING;
    private String userComment = "Top up balance";
    private String adminComment;
    private LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 30);

    public static PayRequestTestBuilder payRequest() { return new PayRequestTestBuilder(); }

    public PayRequestTestBuilder membershipId(Long membershipId) { this.membershipId = membershipId; return this; }
    public PayRequestTestBuilder amount(Long amount) { this.amount = amount; return this; }
    public PayRequestTestBuilder status(PayRequestStatus status) { this.status = status; return this; }
    public PayRequestTestBuilder userComment(String userComment) { this.userComment = userComment; return this; }
    public PayRequestTestBuilder adminComment(String adminComment) { this.adminComment = adminComment; return this; }
    public PayRequestTestBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

    public PayRequest build() {
        PayRequest request = new PayRequest();
        request.setMembershipId(membershipId);
        request.setAmount(amount);
        request.setStatus(status);
        request.setUserComment(userComment);
        request.setAdminComment(adminComment);
        request.setCreatedAt(createdAt);
        return request;
    }
}
