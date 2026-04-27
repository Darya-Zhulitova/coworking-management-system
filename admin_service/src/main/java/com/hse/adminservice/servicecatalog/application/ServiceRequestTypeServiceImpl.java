package com.hse.adminservice.servicecatalog.application;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.coworking.application.CoworkingConfigurationVersionService;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.servicecatalog.domain.ServiceRequestType;
import com.hse.adminservice.servicecatalog.dto.ServiceRequestTypeCreateRequest;
import com.hse.adminservice.servicecatalog.dto.ServiceRequestTypeResponse;
import com.hse.adminservice.servicecatalog.dto.ServiceRequestTypeUpdateRequest;
import com.hse.adminservice.servicecatalog.mapper.ServiceRequestTypeMapper;
import com.hse.adminservice.servicecatalog.persistence.ServiceRequestTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceRequestTypeServiceImpl implements ServiceRequestTypeService {

    private final ServiceRequestTypeRepository serviceRequestTypeRepository;
    private final CoworkingRepository coworkingRepository;
    private final AdminAuthorizationService authorizationService;
    private final ServiceRequestTypeMapper serviceRequestTypeMapper;
    private final CoworkingConfigurationVersionService configurationVersionService;

    @Override
    @Transactional
    public ServiceRequestTypeResponse create(Long coworkingId, ServiceRequestTypeCreateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SERVICE_REQUEST_TYPE_EDIT);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));
        String normalizedName = request.getName().trim();
        if (serviceRequestTypeRepository.existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(
                coworkingId,
                normalizedName
        )) {
            throw new ConflictException("Service request type name must be unique within coworking");
        }
        LocalDateTime now = LocalDateTime.now();
        ServiceRequestType entity = serviceRequestTypeRepository.save(ServiceRequestType.builder()
                .coworking(coworking)
                .name(normalizedName)
                .cost(request.getCost())
                .version(1)
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build());
        configurationVersionService.bumpVersion(coworkingId);
        return serviceRequestTypeMapper.toResponse(entity);
    }

    @Override
    public List<ServiceRequestTypeResponse> getAll(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SERVICE_REQUEST_TYPE_READ);
        return serviceRequestTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId)
                .stream()
                .map(serviceRequestTypeMapper::toResponse)
                .toList();
    }

    @Override
    public ServiceRequestTypeResponse getById(Long coworkingId, Long serviceRequestTypeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SERVICE_REQUEST_TYPE_READ);
        return serviceRequestTypeMapper.toResponse(getExistingType(coworkingId, serviceRequestTypeId));
    }

    @Override
    @Transactional
    public ServiceRequestTypeResponse update(
            Long coworkingId,
            Long serviceRequestTypeId,
            ServiceRequestTypeUpdateRequest request
    ) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SERVICE_REQUEST_TYPE_EDIT);
        ServiceRequestType entity = getExistingType(coworkingId, serviceRequestTypeId);
        String normalizedName = request.getName().trim();
        if (!entity.getName()
                .equalsIgnoreCase(normalizedName) && serviceRequestTypeRepository.existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(coworkingId,
                normalizedName
        )) {
            throw new ConflictException("Service request type name must be unique within coworking");
        }
        entity.setName(normalizedName);
        entity.setCost(request.getCost());
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        entity.setVersion(entity.getVersion() + 1);
        entity.setUpdatedAt(LocalDateTime.now());
        ServiceRequestType saved = serviceRequestTypeRepository.save(entity);
        configurationVersionService.bumpVersion(coworkingId);
        return serviceRequestTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void archive(Long coworkingId, Long serviceRequestTypeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SERVICE_REQUEST_TYPE_EDIT);
        ServiceRequestType entity = getExistingType(coworkingId, serviceRequestTypeId);
        LocalDateTime now = LocalDateTime.now();
        entity.setArchived(true);
        entity.setActive(false);
        entity.setArchivedAt(now);
        entity.setUpdatedAt(now);
        serviceRequestTypeRepository.save(entity);
        configurationVersionService.bumpVersion(coworkingId);
    }

    private ServiceRequestType getExistingType(Long coworkingId, Long serviceRequestTypeId) {
        return serviceRequestTypeRepository.findByIdAndCoworkingIdAndArchivedFalse(serviceRequestTypeId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request type not found"));
    }
}
