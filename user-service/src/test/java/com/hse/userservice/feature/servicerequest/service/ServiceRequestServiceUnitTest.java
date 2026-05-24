package com.hse.userservice.feature.servicerequest.service;

import com.hse.userservice.common.context.CurrentUserService;
import com.hse.userservice.common.exception.ResourceConflictException;
import com.hse.userservice.common.exception.ResourceNotFoundException;
import com.hse.userservice.common.files.FileStorageService;
import com.hse.userservice.common.files.StoredFileResponse;
import com.hse.userservice.feature.balance.service.LedgerService;
import com.hse.userservice.feature.balance.service.UnitsService;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.domain.MembershipStatus;
import com.hse.userservice.feature.membership.repository.MembershipRepository;
import com.hse.userservice.feature.membership.service.MembershipService;
import com.hse.userservice.feature.servicerequest.domain.Message;
import com.hse.userservice.feature.servicerequest.domain.MessageAuthorType;
import com.hse.userservice.feature.servicerequest.domain.ServiceRequest;
import com.hse.userservice.feature.servicerequest.domain.ServiceRequestAttachment;
import com.hse.userservice.feature.servicerequest.domain.ServiceRequestStatus;
import com.hse.userservice.feature.servicerequest.dto.CreateServiceRequestDto;
import com.hse.userservice.feature.servicerequest.dto.ServiceRequestMessageDto;
import com.hse.userservice.feature.servicerequest.dto.ServiceRequestTypeOptionDto;
import com.hse.userservice.feature.servicerequest.repository.MessageRepository;
import com.hse.userservice.feature.servicerequest.repository.ServiceRequestAttachmentRepository;
import com.hse.userservice.feature.servicerequest.repository.ServiceRequestRepository;
import com.hse.userservice.feature.user.domain.User;
import com.hse.userservice.feature.user.repository.UserRepository;
import com.hse.userservice.integration.AdminServiceClient;
import com.hse.userservice.integration.dto.ServiceRequestTypeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServiceRequestServiceUnitTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-22T10:15:30Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(FIXED_CLOCK.instant(), FIXED_CLOCK.getZone());

    @Mock ServiceRequestRepository serviceRequestRepository;
    @Mock MembershipService membershipService;
    @Mock AdminServiceClient adminServiceClient;
    @Mock MessageRepository messageRepository;
    @Mock CurrentUserService currentUserService;
    @Mock ServiceRequestAttachmentRepository attachmentRepository;
    @Mock FileStorageService fileStorageService;
    @Mock MembershipRepository membershipRepository;
    @Mock UserRepository userRepository;
    @Mock UnitsService unitsService;
    @Mock LedgerService ledgerService;

    private ServiceRequestService service;
    private final AtomicLong requestIds = new AtomicLong(100L);
    private final AtomicLong messageIds = new AtomicLong(200L);
    private final AtomicLong attachmentIds = new AtomicLong(300L);

    @BeforeEach
    void setUp() {
        service = new ServiceRequestService(
                serviceRequestRepository,
                membershipService,
                adminServiceClient,
                messageRepository,
                currentUserService,
                attachmentRepository,
                fileStorageService,
                membershipRepository,
                userRepository,
                unitsService,
                ledgerService,
                FIXED_CLOCK
        );
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(invocation -> {
            ServiceRequest request = invocation.getArgument(0);
            if (request.getId() == null) {
                request.setId(requestIds.incrementAndGet());
            }
            return request;
        });
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            if (message.getId() == null) {
                message.setId(messageIds.incrementAndGet());
            }
            return message;
        });
    }

    @Test
    void createPersistsRequestWithTrimmedNameAndSeedsSystemMessage() {
        Membership membership = membership(7L, 11L, MembershipStatus.ACTIVE);
        User user = user(41L, "Resident Name");
        when(membershipService.requireOwnedMembershipById(7L)).thenReturn(membership);
        when(adminServiceClient.getServiceRequestType(11L, 5L)).thenReturn(type(5L, "Cleaning", 350L, true));
        when(currentUserService.getCurrentUser()).thenReturn(user);

        var result = service.create(7L, new CreateServiceRequestDto(5L, "  Need cleaning  "));

        assertThat(result.id()).isEqualTo(101L);
        assertThat(result.membershipId()).isEqualTo(7L);
        assertThat(result.typeName()).isEqualTo("Cleaning");
        assertThat(result.name()).isEqualTo("Need cleaning");
        assertThat(result.cost()).isEqualTo(350L);
        assertThat(result.status()).isEqualTo(ServiceRequestStatus.NEW);
        assertThat(result.createdAt()).isEqualTo(NOW);

        ArgumentCaptor<ServiceRequest> requestCaptor = ArgumentCaptor.forClass(ServiceRequest.class);
        verify(serviceRequestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getName()).isEqualTo("Need cleaning");
        assertThat(requestCaptor.getValue().getTypeId()).isEqualTo(5L);
        assertThat(requestCaptor.getValue().getTypeName()).isEqualTo("Cleaning");
        assertThat(requestCaptor.getValue().getCreatedAt()).isEqualTo(NOW);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getAuthorType()).isEqualTo(MessageAuthorType.SYSTEM);
        assertThat(messageCaptor.getValue().getText()).isEqualTo("Сервисная заявка создана пользователем Resident Name.");
        assertThat(messageCaptor.getValue().getTimestamp()).isEqualTo(NOW);
    }

    @Test
    void createRejectsPendingMembershipBeforeCallingAdminCatalog() {
        when(membershipService.requireOwnedMembershipById(7L)).thenReturn(membership(7L, 11L, MembershipStatus.PENDING));

        assertThatThrownBy(() -> service.create(7L, new CreateServiceRequestDto(5L, "Need help")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Сервисные заявки доступны только для активных и заблокированных пользователей");

        verifyNoInteractions(adminServiceClient, currentUserService);
        verify(serviceRequestRepository, never()).save(any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void createRejectsInactiveServiceRequestType() {
        when(membershipService.requireOwnedMembershipById(7L)).thenReturn(membership(7L, 11L, MembershipStatus.ACTIVE));
        when(adminServiceClient.getServiceRequestType(11L, 5L)).thenReturn(type(5L, "Old type", 0L, false));

        assertThatThrownBy(() -> service.create(7L, new CreateServiceRequestDto(5L, "Need help")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Тип сервисной заявки не найден");

        verify(serviceRequestRepository, never()).save(any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void getTypesReturnsOnlyActiveTypes() {
        when(membershipService.requireOwnedMembershipById(7L)).thenReturn(membership(7L, 11L, MembershipStatus.ACTIVE));
        when(adminServiceClient.getServiceRequestTypes(11L)).thenReturn(List.of(
                type(1L, "Coffee machine", 0L, true),
                type(2L, "Archived", 100L, false),
                type(3L, "Paid cleaning", 500L, true)
        ));

        List<ServiceRequestTypeOptionDto> result = service.getTypes(7L);

        assertThat(result).extracting(ServiceRequestTypeOptionDto::id).containsExactly(1L, 3L);
        assertThat(result).extracting(ServiceRequestTypeOptionDto::name).containsExactly("Coffee machine", "Paid cleaning");
    }

    @Test
    void addMessageTrimsTextAndStoresAttachmentMetadata() {
        ServiceRequest request = request(15L, 7L, 5L, "Printer", 0L, ServiceRequestStatus.NEW);
        when(membershipService.requireOwnedMembershipById(7L)).thenReturn(membership(7L, 11L, MembershipStatus.ACTIVE));
        when(serviceRequestRepository.findByIdAndMembershipIdInForUpdate(15L, List.of(7L))).thenReturn(Optional.of(request));
        when(currentUserService.getCurrentUserId()).thenReturn(41L);
        when(fileStorageService.uploadServiceRequestAttachment(eq(15L), eq(201L), any())).thenReturn(new StoredFileResponse(
                "file-1",
                "https://files/uploaded"
        ));
        when(fileStorageService.presignedUrl("file-1")).thenReturn("https://files/signed");
        when(attachmentRepository.save(any(ServiceRequestAttachment.class))).thenAnswer(invocation -> {
            ServiceRequestAttachment attachment = invocation.getArgument(0);
            attachment.setId(attachmentIds.incrementAndGet());
            return attachment;
        });
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.png",
                "image/png",
                "image".getBytes(StandardCharsets.UTF_8)
        );

        ServiceRequestMessageDto result = service.addMessage(7L, 15L, "  Here is a photo  ", file);

        assertThat(result.id()).isEqualTo(201L);
        assertThat(result.authorType()).isEqualTo(MessageAuthorType.USER);
        assertThat(result.authorName()).isEqualTo("Вы");
        assertThat(result.text()).isEqualTo("Here is a photo");
        assertThat(result.attachments()).hasSize(1);
        assertThat(result.attachments().getFirst().fileName()).isEqualTo("photo.png");
        assertThat(result.attachments().getFirst().url()).isEqualTo("https://files/signed");

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getAuthorId()).isEqualTo(41L);
        assertThat(messageCaptor.getValue().getTimestamp()).isEqualTo(NOW);

        ArgumentCaptor<ServiceRequestAttachment> attachmentCaptor = ArgumentCaptor.forClass(ServiceRequestAttachment.class);
        verify(attachmentRepository).save(attachmentCaptor.capture());
        assertThat(attachmentCaptor.getValue().getMessageId()).isEqualTo(201L);
        assertThat(attachmentCaptor.getValue().getFileId()).isEqualTo("file-1");
        assertThat(attachmentCaptor.getValue().getContentType()).isEqualTo("image/png");
        assertThat(attachmentCaptor.getValue().getSizeBytes()).isEqualTo(file.getSize());
    }

    @Test
    void addMessageRejectsEmptyContentAndFinalRequest() {
        ServiceRequest newRequest = request(15L, 7L, 5L, "Printer", 0L, ServiceRequestStatus.NEW);
        when(membershipService.requireOwnedMembershipById(7L)).thenReturn(membership(7L, 11L, MembershipStatus.ACTIVE));
        when(serviceRequestRepository.findByIdAndMembershipIdInForUpdate(15L, List.of(7L))).thenReturn(Optional.of(newRequest));

        assertThatThrownBy(() -> service.addMessage(7L, 15L, "   ", null))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Добавьте текст сообщения или файл");

        ServiceRequest resolvedRequest = request(16L, 7L, 5L, "Printer", 0L, ServiceRequestStatus.RESOLVED);
        when(serviceRequestRepository.findByIdAndMembershipIdInForUpdate(16L, List.of(7L))).thenReturn(Optional.of(
                resolvedRequest));

        assertThatThrownBy(() -> service.addMessage(7L, 16L, "new text", null))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("закрытую сервисную заявку");

        verify(messageRepository, never()).save(any());
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void changeStatusByAdminAddsCommentAndStatusMessageWhenMovingToInProgress() {
        ServiceRequest request = request(15L, 7L, 5L, "Printer", 0L, ServiceRequestStatus.NEW);
        when(serviceRequestRepository.findByIdAndCoworkingIdForUpdate(15L, 11L)).thenReturn(Optional.of(request));

        ServiceRequest result = service.changeStatusByAdmin(11L, 15L, ServiceRequestStatus.IN_PROGRESS, "  We started  ");

        assertThat(result.getStatus()).isEqualTo(ServiceRequestStatus.IN_PROGRESS);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, org.mockito.Mockito.times(2)).save(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues()).extracting(Message::getAuthorType)
                .containsExactly(MessageAuthorType.ADMIN, MessageAuthorType.SYSTEM);
        assertThat(messageCaptor.getAllValues().get(0).getText()).isEqualTo("We started");
        assertThat(messageCaptor.getAllValues().get(1).getText()).contains("новая → в работе");
        verify(ledgerService, never()).createServiceRequestCharge(any(), anyLong(), any(), any());
    }

    @Test
    void changeStatusByAdminChargesPaidRequestWhenResolvingWithEnoughBalance() {
        ServiceRequest request = request(15L, 7L, 5L, "Paid cleaning", 400L, ServiceRequestStatus.IN_PROGRESS);
        Membership membership = membership(7L, 11L, MembershipStatus.ACTIVE);
        when(serviceRequestRepository.findByIdAndCoworkingIdForUpdate(15L, 11L)).thenReturn(Optional.of(request));
        when(membershipService.requireMembershipInCoworkingForUpdate(11L, 7L)).thenReturn(membership);
        when(unitsService.getBalanceMinorUnits(7L)).thenReturn(1_000L);

        ServiceRequest result = service.changeStatusByAdmin(11L, 15L, ServiceRequestStatus.RESOLVED, null);

        assertThat(result.getStatus()).isEqualTo(ServiceRequestStatus.RESOLVED);
        assertThat(result.getResolvedAt()).isEqualTo(NOW);
        verify(ledgerService).createServiceRequestCharge(7L, 400L, 15L, "Paid cleaning");
        verify(serviceRequestRepository).save(request);
    }

    @Test
    void changeStatusByAdminRejectsPaidResolveWhenBalanceIsInsufficient() {
        ServiceRequest request = request(15L, 7L, 5L, "Paid cleaning", 400L, ServiceRequestStatus.IN_PROGRESS);
        when(serviceRequestRepository.findByIdAndCoworkingIdForUpdate(15L, 11L)).thenReturn(Optional.of(request));
        when(membershipService.requireMembershipInCoworkingForUpdate(11L, 7L)).thenReturn(membership(
                7L,
                11L,
                MembershipStatus.ACTIVE
        ));
        when(unitsService.getBalanceMinorUnits(7L)).thenReturn(399L);

        assertThatThrownBy(() -> service.changeStatusByAdmin(11L, 15L, ServiceRequestStatus.RESOLVED, null))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("баланс");

        assertThat(request.getStatus()).isEqualTo(ServiceRequestStatus.IN_PROGRESS);
        assertThat(request.getResolvedAt()).isNull();
        verify(ledgerService, never()).createServiceRequestCharge(any(), anyLong(), any(), any());
        verify(serviceRequestRepository, never()).save(any());
    }



    @Test
    void getByMembershipIdUsesPersistedTypeNameWithoutAdminCatalogLookup() {
        Membership membership = membership(7L, 11L, MembershipStatus.ACTIVE);
        ServiceRequest request = request(15L, 7L, 999L, "Unknown type", 0L, ServiceRequestStatus.NEW);
        request.setTypeName("Snapshot type");
        when(membershipService.requireOwnedMembershipById(7L)).thenReturn(membership);
        when(serviceRequestRepository.findByMembershipIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(request));

        var result = service.getByMembershipId(7L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().typeName()).isEqualTo("Snapshot type");
        verify(adminServiceClient, never()).getServiceRequestTypes(anyLong());
    }

    @Test
    void getDetailsUsesPersistedTypeNameWithoutAdminCatalogLookup() {
        Membership membership = membership(7L, 11L, MembershipStatus.ACTIVE);
        ServiceRequest request = request(15L, 7L, 5L, "Printer", 0L, ServiceRequestStatus.NEW);
        request.setTypeName("Saved type name");
        when(membershipService.requireOwnedMembershipById(7L)).thenReturn(membership);
        when(serviceRequestRepository.findById(15L)).thenReturn(Optional.of(request));

        var result = service.getDetails(7L, 15L);

        assertThat(result.typeId()).isEqualTo(5L);
        assertThat(result.typeName()).isEqualTo("Saved type name");
        verify(adminServiceClient, never()).getServiceRequestTypes(anyLong());
    }

    @Test
    void getDetailsRejectsRequestFromAnotherMembership() {
        Membership membership = membership(7L, 11L, MembershipStatus.ACTIVE);
        ServiceRequest foreignRequest = request(15L, 8L, 5L, "Foreign", 0L, ServiceRequestStatus.NEW);
        when(membershipService.requireOwnedMembershipById(7L)).thenReturn(membership);
        when(serviceRequestRepository.findById(15L)).thenReturn(Optional.of(foreignRequest));

        assertThatThrownBy(() -> service.getDetails(7L, 15L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Сервисная заявка не найдена");
    }

    @Test
    void getMessagesReturnsEmptyListWithoutAttachmentLookupWhenRequestHasNoMessages() {
        Membership membership = membership(7L, 11L, MembershipStatus.ACTIVE);
        ServiceRequest request = request(15L, 7L, 5L, "Printer", 0L, ServiceRequestStatus.NEW);
        when(membershipService.requireOwnedMembershipById(7L)).thenReturn(membership);
        when(serviceRequestRepository.findById(15L)).thenReturn(Optional.of(request));
        when(messageRepository.findAllByServiceRequestIdOrderByTimestampAsc(15L)).thenReturn(List.of());

        List<ServiceRequestMessageDto> result = service.getMessages(7L, 15L);

        assertThat(result).isEmpty();
        verify(attachmentRepository, never()).findAllByMessageIdIn(any());
    }

    @Test
    void addAdminMessageRejectsClosedRequestBeforeSavingMessage() {
        ServiceRequest request = request(15L, 7L, 5L, "Printer", 0L, ServiceRequestStatus.RESOLVED);
        when(serviceRequestRepository.findByIdAndCoworkingIdForUpdate(15L, 11L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.addAdminMessage(11L, 15L, "closed", null))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("закрытую сервисную заявку");

        verify(messageRepository, never()).save(any());
    }

    @Test
    void addAdminMessageTrimsTextAndStoresAttachmentMetadata() {
        ServiceRequest request = request(15L, 7L, 5L, "Printer", 0L, ServiceRequestStatus.IN_PROGRESS);
        when(serviceRequestRepository.findByIdAndCoworkingIdForUpdate(15L, 11L)).thenReturn(Optional.of(request));
        when(fileStorageService.uploadServiceRequestAttachment(eq(15L), eq(201L), any())).thenReturn(new StoredFileResponse(
                "file-1",
                "https://files/uploaded"
        ));
        when(fileStorageService.presignedUrl("file-1")).thenReturn("https://files/signed");
        when(attachmentRepository.save(any(ServiceRequestAttachment.class))).thenAnswer(invocation -> {
            ServiceRequestAttachment attachment = invocation.getArgument(0);
            attachment.setId(attachmentIds.incrementAndGet());
            return attachment;
        });
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "admin-note.txt",
                "text/plain",
                "note".getBytes(StandardCharsets.UTF_8)
        );

        ServiceRequestMessageDto result = service.addAdminMessage(11L, 15L, "  Admin reply  ", file);

        assertThat(result.authorType()).isEqualTo(MessageAuthorType.ADMIN);
        assertThat(result.authorName()).isEqualTo("Администратор");
        assertThat(result.text()).isEqualTo("Admin reply");
        assertThat(result.attachments()).hasSize(1);
        assertThat(result.attachments().getFirst().fileName()).isEqualTo("admin-note.txt");
    }

    @Test
    void addMessageSanitizesAndTruncatesAttachmentDisplayName() {
        ServiceRequest request = request(15L, 7L, 5L, "Printer", 0L, ServiceRequestStatus.NEW);
        String unsafeLongName = "../" + "a".repeat(270) + "\n.png";
        when(membershipService.requireOwnedMembershipById(7L)).thenReturn(membership(7L, 11L, MembershipStatus.ACTIVE));
        when(serviceRequestRepository.findByIdAndMembershipIdInForUpdate(15L, List.of(7L))).thenReturn(Optional.of(request));
        when(currentUserService.getCurrentUserId()).thenReturn(41L);
        when(fileStorageService.uploadServiceRequestAttachment(eq(15L), eq(201L), any())).thenReturn(new StoredFileResponse(
                "file-1",
                "https://files/uploaded"
        ));
        when(fileStorageService.presignedUrl("file-1")).thenReturn("https://files/signed");
        when(attachmentRepository.save(any(ServiceRequestAttachment.class))).thenAnswer(invocation -> {
            ServiceRequestAttachment attachment = invocation.getArgument(0);
            attachment.setId(attachmentIds.incrementAndGet());
            return attachment;
        });
        MockMultipartFile file = new MockMultipartFile("file", unsafeLongName, "image/png", "image".getBytes(StandardCharsets.UTF_8));

        service.addMessage(7L, 15L, "photo", file);

        ArgumentCaptor<ServiceRequestAttachment> captor = ArgumentCaptor.forClass(ServiceRequestAttachment.class);
        verify(attachmentRepository).save(captor.capture());
        assertThat(captor.getValue().getFileName()).hasSize(255);
        assertThat(captor.getValue().getFileName()).doesNotContain("/", "\\", "\n", "\r", "\t");
    }


    @Test
    void getInternalQueueMapsUserAndPersistedTypeNames() {
        Membership knownMembership = membership(7L, 11L, MembershipStatus.ACTIVE);
        knownMembership.setUserId(101L);
        ServiceRequest knownRequest = request(10L, 7L, 77L, "Fix printer", 0L, ServiceRequestStatus.NEW);
        ServiceRequest unknownMembershipRequest = request(11L, 99L, 88L, "Unknown owner", 500L, ServiceRequestStatus.IN_PROGRESS);
        when(membershipService.getMembershipsForCoworking(11L)).thenReturn(List.of(knownMembership));
        knownRequest.setTypeName("Equipment");
        unknownMembershipRequest.setTypeName("Paid service");
        when(membershipService.getUsersByMemberships(List.of(knownMembership))).thenReturn(java.util.Map.of(101L, user(101L, "Daria")));
        when(serviceRequestRepository.findAllByCoworkingIdOrderByCreatedAtDesc(11L)).thenReturn(List.of(
                knownRequest,
                unknownMembershipRequest
        ));

        var result = service.getInternalQueue(11L);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().userId()).isEqualTo(101L);
        assertThat(result.getFirst().userName()).isEqualTo("Daria");
        assertThat(result.getFirst().typeName()).isEqualTo("Equipment");
        assertThat(result.getFirst().status()).isEqualTo("new");
        assertThat(result.get(1).userId()).isNull();
        assertThat(result.get(1).userName()).isEqualTo("Пользователь #99");
        assertThat(result.get(1).typeName()).isEqualTo("Paid service");
        verify(adminServiceClient, never()).getServiceRequestTypes(anyLong());
    }

    @Test
    void getInternalMessagesResolveAuthorsAndAttachmentUrls() {
        Membership membership = membership(7L, 11L, MembershipStatus.ACTIVE);
        ServiceRequest request = request(10L, 7L, 77L, "Fix printer", 0L, ServiceRequestStatus.NEW);
        Message userMessage = message(100L, 10L, MessageAuthorType.USER, "User text");
        Message adminMessage = message(101L, 10L, MessageAuthorType.ADMIN, "Admin text");
        ServiceRequestAttachment attachment = attachment(500L, 100L, "file-1", "photo.png");
        when(serviceRequestRepository.findByIdAndCoworkingId(10L, 11L)).thenReturn(Optional.of(request));
        when(membershipService.requireMembershipInCoworking(11L, 7L)).thenReturn(membership);
        when(userRepository.findById(41L)).thenReturn(Optional.of(user(41L, "Daria")));
        when(messageRepository.findAllByServiceRequestIdOrderByTimestampAsc(10L)).thenReturn(List.of(userMessage, adminMessage));
        when(attachmentRepository.findAllByMessageIdIn(List.of(100L, 101L))).thenReturn(List.of(attachment));
        when(fileStorageService.presignedUrl("file-1")).thenReturn("https://files/photo");

        var result = service.getInternalMessages(11L, 10L);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().authorType()).isEqualTo("USER");
        assertThat(result.getFirst().authorName()).isEqualTo("Daria");
        assertThat(result.getFirst().attachments()).hasSize(1);
        assertThat(result.getFirst().attachments().getFirst().url()).isEqualTo("https://files/photo");
        assertThat(result.get(1).authorType()).isEqualTo("ADMIN");
        assertThat(result.get(1).authorName()).isEqualTo("Администратор");
    }

    @Test
    void getInternalWorkspaceHasActionsOnlyForNonFinalStatuses() {
        Membership membership = membership(7L, 11L, MembershipStatus.ACTIVE);
        ServiceRequest openRequest = request(10L, 7L, 77L, "Fix printer", 0L, ServiceRequestStatus.IN_PROGRESS);
        when(serviceRequestRepository.findByIdAndCoworkingId(10L, 11L)).thenReturn(Optional.of(openRequest));
        when(membershipService.requireMembershipInCoworking(11L, 7L)).thenReturn(membership);
        when(userRepository.findById(41L)).thenReturn(Optional.of(user(41L, "Daria")));
        openRequest.setTypeName("Equipment");
        when(unitsService.getBalanceMinorUnits(7L)).thenReturn(1_000L);
        when(messageRepository.findAllByServiceRequestIdOrderByTimestampAsc(10L)).thenReturn(List.of());

        var openWorkspace = service.getInternalWorkspace(11L, 10L);

        assertThat(openWorkspace.request().userEmail()).isEqualTo("resident@example.test");
        assertThat(openWorkspace.request().balanceMinorUnits()).isEqualTo(1_000L);
        assertThat(openWorkspace.request().typeName()).isEqualTo("Equipment");
        assertThat(openWorkspace.availableActions()).containsExactly("REPLY", "IN_PROGRESS", "RESOLVE", "REJECT");

        openRequest.setStatus(ServiceRequestStatus.RESOLVED);
        var finalWorkspace = service.getInternalWorkspace(11L, 10L);

        assertThat(finalWorkspace.availableActions()).isEmpty();
    }

    @Test
    void changeStatusByAdminReturnsRequestWithoutSideEffectsWhenTargetStatusEqualsCurrentStatus() {
        ServiceRequest request = request(15L, 7L, 5L, "Printer", 0L, ServiceRequestStatus.IN_PROGRESS);
        when(serviceRequestRepository.findByIdAndCoworkingIdForUpdate(15L, 11L)).thenReturn(Optional.of(request));

        ServiceRequest result = service.changeStatusByAdmin(11L, 15L, ServiceRequestStatus.IN_PROGRESS, "  duplicate  ");

        assertThat(result).isSameAs(request);
        verify(messageRepository, never()).save(any());
        verify(serviceRequestRepository, never()).save(any());
    }

    @Test
    void changeStatusByAdminRejectsClosedRequestBeforeSavingAnything() {
        ServiceRequest request = request(15L, 7L, 5L, "Printer", 0L, ServiceRequestStatus.REJECTED);
        when(serviceRequestRepository.findByIdAndCoworkingIdForUpdate(15L, 11L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.changeStatusByAdmin(11L, 15L, ServiceRequestStatus.IN_PROGRESS, "reopen"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("закрытой сервисной заявки");

        verify(messageRepository, never()).save(any());
        verify(serviceRequestRepository, never()).save(any());
    }



    private static Message message(Long id, Long requestId, MessageAuthorType authorType, String text) {
        Message message = new Message();
        message.setId(id);
        message.setServiceRequestId(requestId);
        message.setAuthorType(authorType);
        message.setText(text);
        message.setTimestamp(NOW.plusMinutes(id));
        return message;
    }

    private static ServiceRequestAttachment attachment(Long id, Long messageId, String fileId, String fileName) {
        ServiceRequestAttachment attachment = new ServiceRequestAttachment();
        attachment.setId(id);
        attachment.setMessageId(messageId);
        attachment.setFileId(fileId);
        attachment.setFileName(fileName);
        attachment.setContentType("image/png");
        attachment.setSizeBytes(42L);
        attachment.setCreatedAt(NOW);
        return attachment;
    }
    private static Membership membership(Long id, Long coworkingId, MembershipStatus status) {
        Membership membership = new Membership();
        membership.setId(id);
        membership.setUserId(41L);
        membership.setCoworkingId(coworkingId);
        membership.setStatus(status);
        membership.setCreatedAt(NOW.minusDays(3));
        return membership;
    }

    private static ServiceRequest request(
            Long id,
            Long membershipId,
            Long typeId,
            String name,
            Long cost,
            ServiceRequestStatus status
    ) {
        ServiceRequest request = new ServiceRequest();
        request.setId(id);
        request.setMembershipId(membershipId);
        request.setTypeId(typeId);
        request.setTypeName("Type " + typeId);
        request.setName(name);
        request.setCost(cost);
        request.setStatus(status);
        request.setCreatedAt(NOW.minusDays(1));
        return request;
    }

    private static ServiceRequestTypeInfo type(Long id, String name, Long cost, Boolean active) {
        return new ServiceRequestTypeInfo(id, name, cost, 1, active);
    }

    private static User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail("resident@example.test");
        user.setPasswordHash("hash");
        return user;
    }
}
