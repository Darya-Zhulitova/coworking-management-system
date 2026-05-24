package com.hse.userservice.support.builder;

import com.hse.userservice.feature.servicerequest.domain.ServiceRequest;
import com.hse.userservice.feature.servicerequest.domain.ServiceRequestStatus;

import java.time.LocalDateTime;

public class ServiceRequestTestBuilder {
    private Long membershipId = 1L;
    private Long typeId = 1L;
    private String typeName = "Water delivery";
    private String name = "Water";
    private Long cost = 0L;
    private ServiceRequestStatus status = ServiceRequestStatus.NEW;
    private LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 12, 0);
    private LocalDateTime resolvedAt;

    public static ServiceRequestTestBuilder serviceRequest() { return new ServiceRequestTestBuilder(); }

    public ServiceRequestTestBuilder membershipId(Long membershipId) { this.membershipId = membershipId; return this; }
    public ServiceRequestTestBuilder typeId(Long typeId) { this.typeId = typeId; return this; }
    public ServiceRequestTestBuilder typeName(String typeName) { this.typeName = typeName; return this; }
    public ServiceRequestTestBuilder name(String name) { this.name = name; return this; }
    public ServiceRequestTestBuilder cost(Long cost) { this.cost = cost; return this; }
    public ServiceRequestTestBuilder status(ServiceRequestStatus status) { this.status = status; return this; }
    public ServiceRequestTestBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
    public ServiceRequestTestBuilder resolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; return this; }

    public ServiceRequest build() {
        ServiceRequest request = new ServiceRequest();
        request.setMembershipId(membershipId);
        request.setTypeId(typeId);
        request.setTypeName(typeName);
        request.setName(name);
        request.setCost(cost);
        request.setStatus(status);
        request.setCreatedAt(createdAt);
        request.setResolvedAt(resolvedAt);
        return request;
    }
}
