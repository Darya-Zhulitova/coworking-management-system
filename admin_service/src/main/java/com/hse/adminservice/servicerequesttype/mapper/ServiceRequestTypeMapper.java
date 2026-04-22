package com.hse.adminservice.servicerequesttype.mapper;

import com.hse.adminservice.servicerequesttype.dto.ServiceRequestTypeResponse;
import com.hse.adminservice.servicerequesttype.entity.ServiceRequestType;
import org.springframework.stereotype.Component;

@Component
public class ServiceRequestTypeMapper {
    public ServiceRequestTypeResponse toResponse(ServiceRequestType entity) {
        return ServiceRequestTypeResponse.builder()
                .id(entity.getId())
                .coworkingId(entity.getCoworking().getId())
                .name(entity.getName())
                .cost(entity.getCost())
                .version(entity.getVersion())
                .active(entity.getActive())
                .archived(entity.getArchived())
                .archivedAt(entity.getArchivedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
