package com.hse.adminservice.integration.user.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hse.adminservice.common.exception.ConflictException;
import com.hse.adminservice.common.exception.ResourceNotFoundException;
import com.hse.adminservice.coworking.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class UserOperationsHttpClient implements UserOperationsClient {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public UserOperationsHttpClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${integration.user-service.base-url:http://localhost:8080/api}") String baseUrl,
            @Value("${integration.internal-api.key:demo-internal-key}") String internalApiKey
    ) {
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl(baseUrl).defaultHeader(INTERNAL_API_KEY_HEADER, internalApiKey).build();
    }

    @Override
    public List<CoworkingUserReadModelResponse> getUsers(Long coworkingId) {
        return getList(
                "/internal/coworkings/{coworkingId}/users", new ParameterizedTypeReference<>() {
                }, coworkingId
        );
    }

    @Override
    public UserQueueSummaryResponse getSummary(Long coworkingId) {
        return get("/internal/coworkings/{coworkingId}/users/summary", UserQueueSummaryResponse.class, coworkingId);
    }

    @Override
    public UserAnalyticsResponse getAnalytics(Long coworkingId) {
        return get("/internal/coworkings/{coworkingId}/users/analytics", UserAnalyticsResponse.class, coworkingId);
    }

    @Override
    public List<MembershipQueueItemResponse> getMemberships(Long coworkingId) {
        return getList(
                "/internal/coworkings/{coworkingId}/users/memberships", new ParameterizedTypeReference<>() {
                }, coworkingId
        );
    }

    @Override
    public List<PayRequestQueueItemResponse> getPayRequests(Long coworkingId) {
        return getList(
                "/internal/coworkings/{coworkingId}/users/pay-requests", new ParameterizedTypeReference<>() {
                }, coworkingId
        );
    }

    @Override
    public List<ServiceRequestQueueItemResponse> getServiceRequests(Long coworkingId) {
        return getList(
                "/internal/coworkings/{coworkingId}/users/service-requests", new ParameterizedTypeReference<>() {
                }, coworkingId
        );
    }

    @Override
    public ServiceRequestDetailResponse getServiceRequestDetails(Long coworkingId, Long serviceRequestId) {
        return get(
                "/internal/coworkings/{coworkingId}/users/service-requests/{serviceRequestId}",
                ServiceRequestDetailResponse.class,
                coworkingId,
                serviceRequestId
        );
    }

    @Override
    public List<ServiceRequestMessageResponse> getServiceRequestMessages(Long coworkingId, Long serviceRequestId) {
        return getList(
                "/internal/coworkings/{coworkingId}/users/service-requests/{serviceRequestId}/messages",
                new ParameterizedTypeReference<>() {
                },
                coworkingId,
                serviceRequestId
        );
    }

    @Override
    public ServiceRequestMessageResponse addServiceRequestMessage(
            Long coworkingId,
            Long serviceRequestId,
            String text
    ) {
        CreateServiceRequestMessageRequest request = new CreateServiceRequestMessageRequest();
        request.setText(text);
        return post(
                "/internal/coworkings/{coworkingId}/users/service-requests/{serviceRequestId}/messages",
                request,
                ServiceRequestMessageResponse.class,
                coworkingId,
                serviceRequestId
        );
    }

    @Override
    public void approveMembership(Long coworkingId, Long membershipId) {
        postNoContent(
                "/internal/coworkings/{coworkingId}/users/memberships/{membershipId}/approve",
                coworkingId,
                membershipId
        );
    }

    @Override
    public void rejectMembership(Long coworkingId, Long membershipId) {
        postNoContent(
                "/internal/coworkings/{coworkingId}/users/memberships/{membershipId}/reject",
                coworkingId,
                membershipId
        );
    }

    @Override
    public void approvePayRequest(Long coworkingId, Long payRequestId) {
        postNoContent(
                "/internal/coworkings/{coworkingId}/users/pay-requests/{payRequestId}/approve",
                coworkingId,
                payRequestId
        );
    }

    @Override
    public void rejectPayRequest(Long coworkingId, Long payRequestId) {
        postNoContent(
                "/internal/coworkings/{coworkingId}/users/pay-requests/{payRequestId}/reject",
                coworkingId,
                payRequestId
        );
    }

    @Override
    public void advanceServiceRequest(Long coworkingId, Long serviceRequestId, String status) {
        postNoContent(
                "/internal/coworkings/{coworkingId}/users/service-requests/{serviceRequestId}/status/{status}",
                coworkingId,
                serviceRequestId,
                status
        );
    }

    private <T> T get(String uriTemplate, Class<T> responseType, Object... uriVariables) {
        try {
            return restClient.get().uri(uriTemplate, uriVariables).retrieve().onStatus(
                    HttpStatusCode::isError,
                    (request, response) -> handleErrorResponse(response)
            ).body(responseType);
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("User service request failed.", exception);
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException("Unable to reach user service.", exception);
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
            throw new IllegalStateException("User service request failed.", exception);
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException("Unable to reach user service.", exception);
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
            throw new IllegalStateException("User service request failed.", exception);
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException("Unable to reach user service.", exception);
        }
    }

    private void postNoContent(String uriTemplate, Object... uriVariables) {
        try {
            restClient.post().uri(uriTemplate, uriVariables).retrieve().onStatus(
                    HttpStatusCode::isError,
                    (request, response) -> handleErrorResponse(response)
            ).toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("User service request failed.", exception);
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException("Unable to reach user service.", exception);
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
            throw new IllegalStateException(message.isBlank() ? "User service is unavailable." : message);
        }
        throw new IllegalStateException(message.isBlank() ? "User service rejected request with status " + status : message);
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
            return "";
        }
    }
}
