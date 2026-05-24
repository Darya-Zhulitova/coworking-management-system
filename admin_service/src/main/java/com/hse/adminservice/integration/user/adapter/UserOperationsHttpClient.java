package com.hse.adminservice.integration.user.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.integration.user.port.UserOperationsClient;
import com.hse.adminservice.operations.booking.dto.PlaceBookingListResponse;
import com.hse.adminservice.operations.bookingimpact.dto.ImpactCommitRequest;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.operations.common.DecisionRequest;
import com.hse.adminservice.operations.dashboard.OperationsDashboardResponse;
import com.hse.adminservice.operations.membership.dto.ManualBalanceAdjustmentRequest;
import com.hse.adminservice.operations.membership.dto.MembershipListItemResponse;
import com.hse.adminservice.operations.membership.dto.MembershipProfileResponse;
import com.hse.adminservice.operations.queue.dto.MembershipQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.PayRequestQueueItemResponse;
import com.hse.adminservice.operations.queue.dto.ServiceRequestQueueItemResponse;
import com.hse.adminservice.operations.servicedesk.dto.CreateServiceRequestMessageRequest;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestMessageResponse;
import com.hse.adminservice.operations.servicedesk.dto.ServiceRequestWorkspaceResponse;
import com.hse.adminservice.operations.users.dto.CoworkingUserReadModelResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class UserOperationsHttpClient implements UserOperationsClient {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public UserOperationsHttpClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${integration.user-service.base-url:http://localhost:8080/api}") String baseUrl,
            @Value("${integration.internal-api.key}") String internalApiKey
    ) {
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl(baseUrl).defaultHeader(INTERNAL_API_KEY_HEADER, internalApiKey).build();
    }

    @Override
    public PlaceBookingListResponse getPlaceBookings(Long coworkingId, Long placeId) {
        return get(
                "/internal/coworkings/{coworkingId}/places/{placeId}/bookings",
                PlaceBookingListResponse.class,
                coworkingId,
                placeId
        );
    }

    @Override
    public List<CoworkingUserReadModelResponse> getUsers(Long coworkingId) {
        return getList(
                "/internal/coworkings/{coworkingId}/users", new ParameterizedTypeReference<>() {
                }, coworkingId
        );
    }


    @Override
    public List<MembershipListItemResponse> getMembershipList(Long coworkingId, String search) {
        if (StringUtils.hasText(search)) {
            return getList(
                    "/internal/coworkings/{coworkingId}/memberships?search={search}",
                    new ParameterizedTypeReference<>() {
                    },
                    coworkingId,
                    search.trim()
            );
        }
        return getList(
                "/internal/coworkings/{coworkingId}/memberships", new ParameterizedTypeReference<>() {
                }, coworkingId
        );
    }

    @Override
    public MembershipProfileResponse getMembershipProfile(Long coworkingId, Long membershipId) {
        return get(
                "/internal/coworkings/{coworkingId}/memberships/{membershipId}",
                MembershipProfileResponse.class,
                coworkingId,
                membershipId
        );
    }

    @Override
    public MembershipProfileResponse adjustMembershipBalance(
            Long coworkingId,
            Long membershipId,
            ManualBalanceAdjustmentRequest request
    ) {
        return post(
                "/internal/coworkings/{coworkingId}/memberships/{membershipId}/balance-adjustments",
                request,
                MembershipProfileResponse.class,
                coworkingId,
                membershipId
        );
    }


    @Override
    public OperationalImpactResponse previewMembershipBlock(Long coworkingId, Long membershipId) {
        return post(
                "/internal/coworkings/{coworkingId}/booking-impact/membership-block/{membershipId}/preview",
                Map.of(),
                OperationalImpactResponse.class,
                coworkingId,
                membershipId
        );
    }

    @Override
    public OperationalImpactResponse blockMembership(Long coworkingId, Long membershipId, ImpactCommitRequest request) {
        return post(
                "/internal/coworkings/{coworkingId}/booking-impact/membership-block/{membershipId}/commit",
                request,
                OperationalImpactResponse.class,
                coworkingId,
                membershipId
        );
    }

    @Override
    public OperationsDashboardResponse getOperationsDashboard(Long coworkingId) {
        return get(
                "/internal/coworkings/{coworkingId}/operations-dashboard",
                OperationsDashboardResponse.class,
                coworkingId
        );
    }

    @Override
    public List<MembershipQueueItemResponse> getMemberships(Long coworkingId) {
        return getList(
                "/internal/coworkings/{coworkingId}/membership-requests", new ParameterizedTypeReference<>() {
                }, coworkingId
        );
    }

    @Override
    public void decideMembership(Long coworkingId, Long membershipId, DecisionRequest request) {
        post(
                "/internal/coworkings/{coworkingId}/membership-requests/{membershipId}/decision",
                request,
                Void.class,
                coworkingId,
                membershipId
        );
    }

    @Override
    public List<PayRequestQueueItemResponse> getPayRequests(Long coworkingId) {
        return getList(
                "/internal/coworkings/{coworkingId}/pay-requests", new ParameterizedTypeReference<>() {
                }, coworkingId
        );
    }

    @Override
    public void decidePayRequest(Long coworkingId, Long payRequestId, DecisionRequest request) {
        post(
                "/internal/coworkings/{coworkingId}/pay-requests/{payRequestId}/decision",
                request,
                Void.class,
                coworkingId,
                payRequestId
        );
    }

    @Override
    public List<ServiceRequestQueueItemResponse> getServiceRequests(Long coworkingId) {
        return getList(
                "/internal/coworkings/{coworkingId}/service-requests", new ParameterizedTypeReference<>() {
                }, coworkingId
        );
    }

    @Override
    public ServiceRequestWorkspaceResponse getServiceRequestWorkspace(Long coworkingId, Long serviceRequestId) {
        return get(
                "/internal/coworkings/{coworkingId}/service-requests/{serviceRequestId}/workspace",
                ServiceRequestWorkspaceResponse.class,
                coworkingId,
                serviceRequestId
        );
    }

    @Override
    public ServiceRequestMessageResponse addServiceRequestMessage(
            Long coworkingId,
            Long serviceRequestId,
            String text,
            MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            CreateServiceRequestMessageRequest request = new CreateServiceRequestMessageRequest();
            request.setText(text);
            return post(
                    "/internal/coworkings/{coworkingId}/service-requests/{serviceRequestId}/messages",
                    request,
                    ServiceRequestMessageResponse.class,
                    coworkingId,
                    serviceRequestId
            );
        }
        return postMultipart(
                "/internal/coworkings/{coworkingId}/service-requests/{serviceRequestId}/messages",
                text,
                file,
                ServiceRequestMessageResponse.class,
                coworkingId,
                serviceRequestId
        );
    }

    @Override
    public void decideServiceRequest(Long coworkingId, Long serviceRequestId, DecisionRequest request) {
        post(
                "/internal/coworkings/{coworkingId}/service-requests/{serviceRequestId}/decision",
                request,
                Void.class,
                coworkingId,
                serviceRequestId
        );
    }

    private <T> T get(String uriTemplate, Class<T> responseType, Object... uriVariables) {
        try {
            return restClient.get().uri(uriTemplate, uriVariables).retrieve().onStatus(
                    HttpStatusCode::isError,
                    (request, response) -> handleErrorResponse(response)
            ).body(responseType);
        } catch (RestClientResponseException exception) {
            log.error("User service request failed. uriTemplate={}", uriTemplate, exception);
            throw new IllegalStateException("Не удалось получить данные пользователей.", exception);
        } catch (ResourceAccessException exception) {
            log.error("Unable to reach user service. uriTemplate={}", uriTemplate, exception);
            throw new IllegalStateException("Невозможно получить доступ к сервису пользователей.", exception);
        }
    }

    private <T> List<T> getList(
            String uriTemplate,
            ParameterizedTypeReference<List<T>> typeReference,
            Object... uriVariables
    ) {
        try {
            return restClient.get().uri(uriTemplate, uriVariables).retrieve().onStatus(
                    HttpStatusCode::isError,
                    (request, response) -> handleErrorResponse(response)
            ).body(typeReference);
        } catch (RestClientResponseException exception) {
            log.error("User service request failed. uriTemplate={}", uriTemplate, exception);
            throw new IllegalStateException("Не удалось получить данные пользователей.", exception);
        } catch (ResourceAccessException exception) {
            log.error("Unable to reach user service. uriTemplate={}", uriTemplate, exception);
            throw new IllegalStateException("Невозможно получить доступ к сервису пользователей.", exception);
        }
    }

    private <T> T post(String uriTemplate, Object body, Class<T> responseType, Object... uriVariables) {
        try {
            return restClient.post()
                    .uri(uriTemplate, uriVariables)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleErrorResponse(response))
                    .body(responseType);
        } catch (RestClientResponseException exception) {
            log.error("User service request failed. uriTemplate={}", uriTemplate, exception);
            throw new IllegalStateException("Не удалось получить данные пользователей.", exception);
        } catch (ResourceAccessException exception) {
            log.error("Unable to reach user service. uriTemplate={}", uriTemplate, exception);
            throw new IllegalStateException("Невозможно получить доступ к сервису пользователей.", exception);
        }
    }


    private <T> T postMultipart(
            String uriTemplate,
            String text,
            MultipartFile file,
            Class<T> responseType,
            Object... uriVariables
    ) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            if (StringUtils.hasText(text)) {
                body.add("text", text.trim());
            }
            body.add("file", filePart(file));
            return restClient.post()
                    .uri(uriTemplate, uriVariables)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleErrorResponse(response))
                    .body(responseType);
        } catch (RestClientResponseException exception) {
            log.error("User service request failed. uriTemplate={}", uriTemplate, exception);
            throw new IllegalStateException("Не удалось получить данные пользователей.", exception);
        } catch (Exception exception) {
            log.error("Unable to send file to user service. uriTemplate={}", uriTemplate, exception);
            throw new IllegalStateException("Не удалось отправить файл пользователю.", exception);
        }
    }

    private HttpEntity<InputStreamResource> filePart(MultipartFile file) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.getContentType() == null ? "application/octet-stream" : file.getContentType()));
        headers.setContentDispositionFormData(
                "file",
                file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename()
        );
        InputStreamResource resource = new InputStreamResource(file.getInputStream()) {
            @Override
            public long contentLength() {
                return file.getSize();
            }

            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
        return new HttpEntity<>(resource, headers);
    }

    private void postNoContent(String uriTemplate, Object... uriVariables) {
        try {
            restClient.post().uri(uriTemplate, uriVariables).retrieve().onStatus(
                    HttpStatusCode::isError,
                    (request, response) -> handleErrorResponse(response)
            ).toBodilessEntity();
        } catch (RestClientResponseException exception) {
            log.error("User service request failed. uriTemplate={}", uriTemplate, exception);
            throw new IllegalStateException("Не удалось получить данные пользователей.", exception);
        } catch (ResourceAccessException exception) {
            log.error("Unable to reach user service. uriTemplate={}", uriTemplate, exception);
            throw new IllegalStateException("Невозможно получить доступ к сервису пользователей.", exception);
        }
    }

    private void handleErrorResponse(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String message = extractErrorMessage(response);
        if (status == 404) {
            throw new ResourceNotFoundException(message);
        }
        if (status == 409) {
            throw new ConflictException(message);
        }
        if (status >= 500) {
            throw new IllegalStateException(message.isBlank() ? "Не удалось получить данные пользователей." : message);
        }
        throw new IllegalStateException(message.isBlank() ? "Не удалось получить данные пользователей. Статус ответа: " + status : message);
    }

    private String extractErrorMessage(ClientHttpResponse response) {
        try {
            String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
            if (body == null || body.isBlank()) {
                return "";
            }
            JsonNode root = objectMapper.readTree(body);
            if (root.hasNonNull("message")) {
                return root.get("message").asText();
            }
            if (root.has("details") && root.get("details").isArray() && root.get("details").size() > 0) {
                return root.get("details").get(0).asText();
            }
            return body;
        } catch (Exception exception) {
            log.error("Failed to parse error response from user service", exception);
            return "";
        }
    }
}
