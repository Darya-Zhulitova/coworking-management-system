package com.hse.userservice.service;

import com.hse.userservice.client.AdminServiceClient;
import com.hse.userservice.client.dto.CoworkingConfigSnapshot;
import com.hse.userservice.context.service.CurrentUserService;
import com.hse.userservice.domain.membership.Membership;
import com.hse.userservice.domain.membership.MembershipStatus;
import com.hse.userservice.domain.message.Message;
import com.hse.userservice.domain.message.MessageAuthorType;
import com.hse.userservice.domain.request.ServiceRequest;
import com.hse.userservice.domain.request.ServiceRequestStatus;
import com.hse.userservice.dto.request.CreateServiceRequestDto;
import com.hse.userservice.dto.request.CreateServiceRequestMessageDto;
import com.hse.userservice.dto.response.ServiceRequestDto;
import com.hse.userservice.dto.response.ServiceRequestMessageDto;
import com.hse.userservice.dto.response.ServiceRequestTypeOptionDto;
import com.hse.userservice.exception.ResourceNotFoundException;
import com.hse.userservice.repository.MessageRepository;
import com.hse.userservice.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {
    private final ServiceRequestRepository serviceRequestRepository;
    private final MembershipService membershipService;
    private final AdminServiceClient adminServiceClient;
    private final MessageRepository messageRepository;
    private final CurrentUserService currentUserService;

    public ServiceRequestDto create(CreateServiceRequestDto dto) {
        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(dto.coworkingId());
        validateMembershipCanCreate(membership);

        CoworkingConfigSnapshot.ServiceRequestType requestType = getRequestType(
                membership.getCoworkingId(),
                dto.typeId()
        );

        ServiceRequest request = new ServiceRequest();
        request.setMembershipId(membership.getId());
        request.setTypeId(requestType.id());
        request.setName(dto.name().trim());
        request.setCost(requestType.cost());

        ServiceRequest saved = serviceRequestRepository.save(request);
        seedInitialUserMessage(saved, currentUserService.getCurrentUser().getName());
        return toDto(saved, requestType.name(), membership.getCoworkingId());
    }

    public List<ServiceRequestDto> getByCoworkingId(Long coworkingId) {
        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(coworkingId);
        CoworkingConfigSnapshot snapshot = adminServiceClient.getCoworkingConfigSnapshot(coworkingId);
        return serviceRequestRepository.findByMembershipIdOrderByCreatedAtDesc(membership.getId())
                .stream()
                .map(request -> toDto(request, resolveTypeName(snapshot, request.getTypeId()), coworkingId))
                .toList();
    }

    public ServiceRequestDto getDetails(Long coworkingId, Long requestId) {
        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(coworkingId);
        ServiceRequest request = serviceRequestRepository.findById(requestId).filter(item -> item.getMembershipId()
                .equals(membership.getId())).orElseThrow(() -> new ResourceNotFoundException(
                "Service request not found: " + requestId));
        CoworkingConfigSnapshot snapshot = adminServiceClient.getCoworkingConfigSnapshot(coworkingId);
        return toDto(request, resolveTypeName(snapshot, request.getTypeId()), coworkingId);
    }

    public List<ServiceRequestTypeOptionDto> getTypes(Long coworkingId) {
        membershipService.requireOwnedMembershipByCoworkingId(coworkingId);
        CoworkingConfigSnapshot snapshot = adminServiceClient.getCoworkingConfigSnapshot(coworkingId);
        return snapshot.serviceRequestTypes()
                .stream()
                .filter(type -> Boolean.TRUE.equals(type.active()))
                .map(type -> new ServiceRequestTypeOptionDto(type.id(), coworkingId, type.name(), type.cost()))
                .toList();
    }

    public List<ServiceRequestMessageDto> getMessages(Long coworkingId, Long requestId) {
        ServiceRequest request = requireOwnedRequest(coworkingId, requestId);
        return messageRepository.findAllByServiceRequestIdOrderByTimestampAsc(request.getId())
                .stream()
                .map(this::toMessageDto)
                .toList();
    }

    public ServiceRequestMessageDto addMessage(Long requestId, CreateServiceRequestMessageDto dto) {
        ServiceRequest request = requireOwnedRequest(dto.coworkingId(), requestId);
        if (request.getStatus() == ServiceRequestStatus.RESOLVED || request.getStatus() == ServiceRequestStatus.REJECTED) {
            throw new IllegalArgumentException("Cannot add message to final service request.");
        }

        Message message = new Message();
        message.setServiceRequestId(request.getId());
        message.setAuthorType(MessageAuthorType.USER);
        message.setAuthorId(currentUserService.getCurrentUserId());
        message.setText(dto.text().trim());
        return toMessageDto(messageRepository.save(message));
    }

    private void validateMembershipCanCreate(Membership membership) {
        if (membership.getStatus() == MembershipStatus.PENDING) {
            throw new IllegalArgumentException("Service request can be created only for active or blocked membership.");
        }
    }

    private ServiceRequest requireOwnedRequest(Long coworkingId, Long requestId) {
        Membership membership = membershipService.requireOwnedMembershipByCoworkingId(coworkingId);
        return serviceRequestRepository.findById(requestId).filter(item -> item.getMembershipId()
                .equals(membership.getId())).orElseThrow(() -> new ResourceNotFoundException(
                "Service request not found: " + requestId));
    }

    private CoworkingConfigSnapshot.ServiceRequestType getRequestType(Long coworkingId, Long typeId) {
        CoworkingConfigSnapshot snapshot = adminServiceClient.getCoworkingConfigSnapshot(coworkingId);
        return snapshot.serviceRequestTypes().stream().filter(item -> item.id().equals(typeId) && Boolean.TRUE.equals(
                item.active())).findFirst().orElseThrow(() -> new ResourceNotFoundException(
                "Service request type not found: " + typeId));
    }

    private String resolveTypeName(CoworkingConfigSnapshot snapshot, Long typeId) {
        return snapshot.serviceRequestTypes().stream().filter(item -> item.id().equals(typeId)).map(
                CoworkingConfigSnapshot.ServiceRequestType::name).findFirst().orElse("Тип #" + typeId);
    }

    private void seedInitialUserMessage(ServiceRequest request, String authorName) {
        Message message = new Message();
        message.setServiceRequestId(request.getId());
        message.setAuthorType(MessageAuthorType.SYSTEM);
        message.setAuthorId(null);
        message.setText("Заявка создана пользователем " + authorName + ".");
        messageRepository.save(message);
    }

    private ServiceRequestDto toDto(ServiceRequest request, String typeName, Long coworkingId) {
        return new ServiceRequestDto(
                request.getId(),
                request.getMembershipId(),
                request.getTypeId(),
                coworkingId,
                request.getName(),
                typeName,
                request.getCost(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getResolvedAt() == null ? request.getCreatedAt() : request.getResolvedAt(),
                request.getResolvedAt()
        );
    }

    private ServiceRequestMessageDto toMessageDto(Message message) {
        return new ServiceRequestMessageDto(
                message.getId(),
                message.getServiceRequestId(),
                message.getAuthorType(),
                resolveAuthorName(message),
                message.getText(),
                message.getTimestamp(),
                message.getReadAt()
        );
    }

    private String resolveAuthorName(Message message) {
        return switch (message.getAuthorType()) {
            case USER -> "Вы";
            case ADMIN -> "Администратор";
            case SYSTEM -> "Система";
        };
    }
}
