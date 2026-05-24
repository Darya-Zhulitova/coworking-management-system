package com.hse.userservice.feature.servicerequest.service;

import com.hse.userservice.common.context.CurrentUserService;
import com.hse.userservice.common.exception.ResourceConflictException;
import com.hse.userservice.common.exception.ResourceNotFoundException;
import com.hse.userservice.common.files.FileStorageService;
import com.hse.userservice.feature.balance.service.LedgerService;
import com.hse.userservice.feature.balance.service.UnitsService;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.domain.MembershipStatus;
import com.hse.userservice.feature.membership.repository.MembershipRepository;
import com.hse.userservice.feature.membership.service.MembershipService;
import com.hse.userservice.feature.servicerequest.domain.*;
import com.hse.userservice.feature.servicerequest.dto.*;
import com.hse.userservice.feature.servicerequest.repository.MessageRepository;
import com.hse.userservice.feature.servicerequest.repository.ServiceRequestAttachmentRepository;
import com.hse.userservice.feature.servicerequest.repository.ServiceRequestRepository;
import com.hse.userservice.feature.user.domain.User;
import com.hse.userservice.feature.user.repository.UserRepository;
import com.hse.userservice.integration.AdminServiceClient;
import com.hse.userservice.integration.dto.ServiceRequestTypeInfo;
import com.hse.userservice.internal.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {
    private final ServiceRequestRepository serviceRequestRepository;
    private final MembershipService membershipService;
    private final AdminServiceClient adminServiceClient;
    private final MessageRepository messageRepository;
    private final CurrentUserService currentUserService;
    private final ServiceRequestAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final UnitsService unitsService;
    private final LedgerService ledgerService;
    private final Clock clock;

    @Transactional
    public ServiceRequestDto create(Long membershipId, CreateServiceRequestDto dto) {
        Membership membership = membershipService.requireOwnedMembershipById(membershipId);
        validateMembershipCanCreate(membership);

        ServiceRequestTypeInfo requestType = getRequestType(membership.getCoworkingId(), dto.typeId());

        ServiceRequest request = new ServiceRequest();
        request.setMembershipId(membership.getId());
        request.setTypeId(requestType.id());
        request.setTypeName(requestType.name());
        request.setName(dto.name().trim());
        request.setCost(requestType.cost());
        request.setCreatedAt(LocalDateTime.now(clock));

        ServiceRequest saved = serviceRequestRepository.save(request);
        seedInitialUserMessage(saved, currentUserService.getCurrentUser().getName());
        return toDto(saved);
    }

    public List<ServiceRequestDto> getByMembershipId(Long membershipId) {
        Membership membership = membershipService.requireOwnedMembershipById(membershipId);
        return serviceRequestRepository.findByMembershipIdOrderByCreatedAtDesc(membership.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ServiceRequestDto getDetails(Long membershipId, Long requestId) {
        Membership membership = membershipService.requireOwnedMembershipById(membershipId);
        ServiceRequest request = serviceRequestRepository.findById(requestId).filter(item -> item.getMembershipId()
                .equals(membership.getId())).orElseThrow(() -> new ResourceNotFoundException(
                "Сервисная заявка не найдена: " + requestId));
        return toDto(request);
    }

    public List<ServiceRequestTypeOptionDto> getTypes(Long membershipId) {
        Membership membership = membershipService.requireOwnedMembershipById(membershipId);
        return adminServiceClient.getServiceRequestTypes(membership.getCoworkingId())
                .stream()
                .filter(type -> Boolean.TRUE.equals(type.active()))
                .map(type -> new ServiceRequestTypeOptionDto(type.id(), type.name(), type.cost()))
                .toList();
    }


    @Transactional(readOnly = true)
    public List<ServiceRequestQueueItemDto> getInternalQueue(Long coworkingId) {
        List<Membership> memberships = membershipService.getMembershipsForCoworking(coworkingId);
        Map<Long, Membership> membershipById = memberships.stream().collect(Collectors.toMap(
                Membership::getId,
                Function.identity()
        ));
        Map<Long, User> users = membershipService.getUsersByMemberships(memberships);
        return serviceRequestRepository.findAllByCoworkingIdOrderByCreatedAtDesc(coworkingId).stream().map(item -> {
            Membership membership = membershipById.get(item.getMembershipId());
            User user = membership == null ? null : users.get(membership.getUserId());
            return new ServiceRequestQueueItemDto(
                    item.getId(),
                    item.getMembershipId(),
                    membership == null ? null : membership.getUserId(),
                    userName(user, item.getMembershipId()),
                    item.getTypeName(),
                    item.getName(),
                    item.getCost(),
                    item.getStatus().name().toLowerCase(),
                    item.getCreatedAt().toLocalDate()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public InternalServiceRequestWorkspaceDto getInternalWorkspace(Long coworkingId, Long serviceRequestId) {
        InternalServiceRequestDetailDto detail = getInternalDetails(coworkingId, serviceRequestId);
        List<InternalServiceRequestMessageDto> messages = getInternalMessages(coworkingId, serviceRequestId);
        ServiceRequest request = requireRequestForCoworking(coworkingId, serviceRequestId);
        List<String> actions = request.getStatus() == ServiceRequestStatus.RESOLVED || request.getStatus() == ServiceRequestStatus.REJECTED ? List.of() : List.of(
                "REPLY",
                "IN_PROGRESS",
                "RESOLVE",
                "REJECT"
        );
        return new InternalServiceRequestWorkspaceDto(detail, messages, actions);
    }

    @Transactional(readOnly = true)
    public InternalServiceRequestDetailDto getInternalDetails(Long coworkingId, Long serviceRequestId) {
        ServiceRequest request = requireRequestForCoworking(coworkingId, serviceRequestId);
        Membership membership = membershipService.requireMembershipInCoworking(coworkingId, request.getMembershipId());
        User user = userRepository.findById(membership.getUserId()).orElseThrow(() -> new ResourceNotFoundException(
                "Пользователь по сервисной заявке не найден."));
        return new InternalServiceRequestDetailDto(
                request.getId(),
                request.getMembershipId(),
                membership.getUserId(),
                user.getName(),
                user.getEmail(),
                request.getTypeName(),
                request.getName(),
                request.getCost(),
                unitsService.getBalanceMinorUnits(membership.getId()),
                request.getStatus().name().toLowerCase(),
                request.getCreatedAt(),
                request.getResolvedAt() == null ? request.getCreatedAt() : request.getResolvedAt(),
                request.getResolvedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<InternalServiceRequestMessageDto> getInternalMessages(Long coworkingId, Long serviceRequestId) {
        ServiceRequest request = requireRequestForCoworking(coworkingId, serviceRequestId);
        Membership membership = membershipService.requireMembershipInCoworking(coworkingId, request.getMembershipId());
        User user = userRepository.findById(membership.getUserId()).orElseThrow(() -> new ResourceNotFoundException(
                "Пользователь по сервисной заявке не найден."));
        List<Message> messages = messageRepository.findAllByServiceRequestIdOrderByTimestampAsc(request.getId());
        Map<Long, List<ServiceRequestAttachment>> attachmentsByMessageId = loadAttachments(messages);
        return messages.stream().map(message -> toInternalMessageDto(
                message,
                user,
                attachmentsByMessageId.getOrDefault(message.getId(), List.of())
        )).toList();
    }

    @Transactional
    public InternalServiceRequestMessageDto addAdminMessageInternal(
            Long coworkingId,
            Long serviceRequestId,
            String text,
            MultipartFile file
    ) {
        return toInternalMessageDto(addAdminMessage(coworkingId, serviceRequestId, text, file));
    }

    @Transactional(readOnly = true)
    public int countOpenServiceRequestsByCoworking(Long coworkingId) {
        return toInt(serviceRequestRepository.countByCoworkingIdAndStatusNotIn(
                coworkingId,
                List.of(ServiceRequestStatus.RESOLVED, ServiceRequestStatus.REJECTED)
        ));
    }

    public List<ServiceRequestMessageDto> getMessages(Long membershipId, Long requestId) {
        ServiceRequest request = requireOwnedRequest(membershipId, requestId);
        List<Message> messages = messageRepository.findAllByServiceRequestIdOrderByTimestampAsc(request.getId());
        Map<Long, List<ServiceRequestAttachment>> attachmentsByMessageId = loadAttachments(messages);
        return messages.stream().map(message -> toMessageDto(
                message,
                attachmentsByMessageId.getOrDefault(message.getId(), List.of())
        )).toList();
    }

    @Transactional
    public ServiceRequestMessageDto addMessage(Long membershipId, Long requestId, String text, MultipartFile file) {
        ServiceRequest request = requireOwnedRequestForUpdate(membershipId, requestId);
        if (request.getStatus() == ServiceRequestStatus.RESOLVED || request.getStatus() == ServiceRequestStatus.REJECTED) {
            throw new ResourceConflictException("Нельзя добавить сообщение в закрытую сервисную заявку.");
        }
        validateMessageContent(text, file);

        Message message = new Message();
        message.setServiceRequestId(request.getId());
        message.setAuthorType(MessageAuthorType.USER);
        message.setAuthorId(currentUserService.getCurrentUserId());
        message.setText(normalizeText(text));
        message.setTimestamp(LocalDateTime.now(clock));
        Message saved = messageRepository.save(message);
        List<ServiceRequestAttachment> attachments = saveAttachmentIfPresent(request, saved, file);
        return toMessageDto(saved, attachments);
    }

    @Transactional
    public ServiceRequestMessageDto addAdminMessage(
            Long coworkingId,
            Long serviceRequestId,
            String text,
            MultipartFile file
    ) {
        ServiceRequest request = requireRequestForCoworkingForUpdate(coworkingId, serviceRequestId);
        if (request.getStatus() == ServiceRequestStatus.RESOLVED || request.getStatus() == ServiceRequestStatus.REJECTED) {
            throw new ResourceConflictException("Нельзя добавить сообщение в закрытую сервисную заявку.");
        }
        validateMessageContent(text, file);
        return createAdminMessage(request, text, file);
    }

    @Transactional
    public ServiceRequest changeStatusByAdmin(
            Long coworkingId,
            Long serviceRequestId,
            ServiceRequestStatus target,
            String comment
    ) {
        ServiceRequest request = requireRequestForCoworkingForUpdate(coworkingId, serviceRequestId);
        ServiceRequestStatus previousStatus = request.getStatus();
        if (previousStatus == ServiceRequestStatus.RESOLVED || previousStatus == ServiceRequestStatus.REJECTED) {
            throw new ResourceConflictException("Статус закрытой сервисной заявки нельзя изменить.");
        }
        if (previousStatus == target) {
            return request;
        }
        if (StringUtils.hasText(comment)) {
            createAdminMessage(request, comment, null);
        }
        if (target == ServiceRequestStatus.RESOLVED) {
            chargePaidServiceRequest(coworkingId, request);
            request.setResolvedAt(LocalDateTime.now(clock));
        }
        request.setStatus(target);
        ServiceRequest saved = serviceRequestRepository.save(request);
        addStatusChangedMessage(request.getId(), previousStatus, target);
        return saved;
    }

    private ServiceRequestMessageDto createAdminMessage(ServiceRequest request, String text, MultipartFile file) {
        Message message = new Message();
        message.setServiceRequestId(request.getId());
        message.setAuthorType(MessageAuthorType.ADMIN);
        message.setAuthorId(null);
        message.setText(normalizeText(text));
        message.setTimestamp(LocalDateTime.now(clock));
        Message saved = messageRepository.save(message);
        List<ServiceRequestAttachment> attachments = saveAttachmentIfPresent(request, saved, file);
        return toMessageDto(saved, attachments);
    }

    private void chargePaidServiceRequest(Long coworkingId, ServiceRequest request) {
        if (request.getCost() <= 0) {
            return;
        }
        Membership membership = membershipService.requireMembershipInCoworkingForUpdate(
                coworkingId,
                request.getMembershipId()
        );
        long resultingBalance = unitsService.getBalanceMinorUnits(membership.getId()) - request.getCost();
        if (resultingBalance < 0) {
            throw new ResourceConflictException(
                    "Сервисную заявку нельзя закрыть: на балансе пользователя недостаточно средств.");
        }
        ledgerService.createServiceRequestCharge(
                membership.getId(),
                request.getCost(),
                request.getId(),
                request.getName()
        );
    }

    private ServiceRequest requireRequestForCoworking(Long coworkingId, Long requestId) {
        return serviceRequestRepository.findByIdAndCoworkingId(requestId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Сервисная заявка не найдена: " + requestId));
    }

    private ServiceRequest requireRequestForCoworkingForUpdate(Long coworkingId, Long requestId) {
        return serviceRequestRepository.findByIdAndCoworkingIdForUpdate(requestId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Сервисная заявка не найдена: " + requestId));
    }

    private void addStatusChangedMessage(
            Long serviceRequestId,
            ServiceRequestStatus previousStatus,
            ServiceRequestStatus targetStatus
    ) {
        Message message = new Message();
        message.setServiceRequestId(serviceRequestId);
        message.setAuthorType(MessageAuthorType.SYSTEM);
        message.setAuthorId(null);
        message.setText("Статус сервисной заявки изменен: " + serviceRequestStatusDisplayName(previousStatus) + " → " + serviceRequestStatusDisplayName(
                targetStatus) + ".");
        message.setTimestamp(LocalDateTime.now(clock));
        messageRepository.save(message);
    }

    private String serviceRequestStatusDisplayName(ServiceRequestStatus status) {
        return switch (status) {
            case NEW -> "новая";
            case IN_PROGRESS -> "в работе";
            case RESOLVED -> "выполнена";
            case REJECTED -> "отклонена";
        };
    }

    private void validateMembershipCanCreate(Membership membership) {
        if (membership.getStatus() == MembershipStatus.PENDING) {
            throw new ResourceConflictException(
                    "Сервисные заявки доступны только для активных и заблокированных пользователей.");
        }
    }

    private ServiceRequest requireOwnedRequest(Long membershipId, Long requestId) {
        Membership membership = membershipService.requireOwnedMembershipById(membershipId);
        return serviceRequestRepository.findById(requestId).filter(item -> item.getMembershipId()
                .equals(membership.getId())).orElseThrow(() -> new ResourceNotFoundException(
                "Сервисная заявка не найдена: " + requestId));
    }

    private ServiceRequest requireOwnedRequestForUpdate(Long membershipId, Long requestId) {
        Membership membership = membershipService.requireOwnedMembershipById(membershipId);
        return serviceRequestRepository.findByIdAndMembershipIdInForUpdate(requestId, List.of(membership.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Сервисная заявка не найдена: " + requestId));
    }

    private ServiceRequestTypeInfo getRequestType(Long coworkingId, Long typeId) {
        ServiceRequestTypeInfo type = adminServiceClient.getServiceRequestType(coworkingId, typeId);
        if (!Boolean.TRUE.equals(type.active())) {
            throw new ResourceNotFoundException("Тип сервисной заявки не найден: " + typeId);
        }
        return type;
    }


    private void seedInitialUserMessage(ServiceRequest request, String authorName) {
        Message message = new Message();
        message.setServiceRequestId(request.getId());
        message.setAuthorType(MessageAuthorType.SYSTEM);
        message.setAuthorId(null);
        message.setText("Сервисная заявка создана пользователем " + authorName + ".");
        message.setTimestamp(LocalDateTime.now(clock));
        messageRepository.save(message);
    }

    private ServiceRequestDto toDto(ServiceRequest request) {
        return new ServiceRequestDto(
                request.getId(),
                request.getMembershipId(),
                request.getTypeId(),
                request.getName(),
                request.getTypeName(),
                request.getCost(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getResolvedAt() == null ? request.getCreatedAt() : request.getResolvedAt(),
                request.getResolvedAt()
        );
    }

    private ServiceRequestMessageDto toMessageDto(Message message, List<ServiceRequestAttachment> attachments) {
        return new ServiceRequestMessageDto(
                message.getId(),
                message.getServiceRequestId(),
                message.getAuthorType(),
                resolveAuthorName(message),
                message.getText(),
                message.getTimestamp(),
                message.getReadAt(),
                attachments.stream().map(this::toAttachmentDto).toList()
        );
    }

    private Map<Long, List<ServiceRequestAttachment>> loadAttachments(List<Message> messages) {
        List<Long> messageIds = messages.stream().map(Message::getId).toList();
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        return attachmentRepository.findAllByMessageIdIn(messageIds).stream().collect(Collectors.groupingBy(
                ServiceRequestAttachment::getMessageId));
    }

    private List<ServiceRequestAttachment> saveAttachmentIfPresent(
            ServiceRequest request,
            Message message,
            MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            return List.of();
        }
        var stored = fileStorageService.uploadServiceRequestAttachment(request.getId(), message.getId(), file);
        ServiceRequestAttachment attachment = new ServiceRequestAttachment();
        attachment.setMessageId(message.getId());
        attachment.setFileId(stored.fileId());
        attachment.setFileName(displayFileName(file.getOriginalFilename()));
        attachment.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        attachment.setSizeBytes(file.getSize());
        attachment.setCreatedAt(LocalDateTime.now(clock));
        return List.of(attachmentRepository.save(attachment));
    }


    private String displayFileName(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            return "Файл";
        }
        String normalized = originalFileName.replace('\\', ' ')
                .replace('/', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim();
        if (!StringUtils.hasText(normalized)) {
            return "Файл";
        }
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }

    private void validateMessageContent(String text, MultipartFile file) {
        if (!StringUtils.hasText(text) && (file == null || file.isEmpty())) {
            throw new ResourceConflictException("Добавьте текст сообщения или файл.");
        }
        if (StringUtils.hasText(text) && text.trim().length() > 1000) {
            throw new ResourceConflictException("Текст должен быть не длиннее 1000 символов");
        }
    }

    private String normalizeText(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private ServiceRequestAttachmentDto toAttachmentDto(ServiceRequestAttachment attachment) {
        return new ServiceRequestAttachmentDto(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                fileStorageService.presignedUrl(attachment.getFileId())
        );
    }

    private String resolveAuthorName(Message message) {
        return switch (message.getAuthorType()) {
            case USER -> "Вы";
            case ADMIN -> "Администратор";
            case SYSTEM -> "Система";
        };
    }

    private InternalServiceRequestMessageDto toInternalMessageDto(ServiceRequestMessageDto message) {
        return new InternalServiceRequestMessageDto(
                message.id(),
                message.authorType().name(),
                message.authorName(),
                message.text(),
                message.timestamp(),
                message.attachments().stream().map(attachment -> new InternalServiceRequestAttachmentDto(
                        attachment.id(),
                        attachment.fileName(),
                        attachment.contentType(),
                        attachment.sizeBytes(),
                        attachment.url()
                )).toList()
        );
    }

    private InternalServiceRequestMessageDto toInternalMessageDto(
            Message message,
            User user,
            List<ServiceRequestAttachment> attachments
    ) {
        return new InternalServiceRequestMessageDto(
                message.getId(), message.getAuthorType().name(), switch (message.getAuthorType()) {
            case USER -> user.getName();
            case ADMIN -> "Администратор";
            case SYSTEM -> "Система";
        }, message.getText(), message.getTimestamp(), attachments.stream().map(this::toInternalAttachmentDto).toList()
        );
    }

    private InternalServiceRequestAttachmentDto toInternalAttachmentDto(ServiceRequestAttachment attachment) {
        return new InternalServiceRequestAttachmentDto(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                fileStorageService.presignedUrl(attachment.getFileId())
        );
    }

    private String userName(User user, Long fallbackId) {
        return user == null ? "Пользователь #" + fallbackId : user.getName();
    }

    private int toInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }
}
