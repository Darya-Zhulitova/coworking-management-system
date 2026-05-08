package com.hse.adminservice.servicecatalog.mapper;

import com.hse.adminservice.servicecatalog.domain.ServiceRequestType;
import com.hse.adminservice.servicecatalog.dto.ServiceRequestTypeResponse;
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
