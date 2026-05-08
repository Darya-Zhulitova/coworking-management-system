package com.hse.adminservice.integration.user.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.integration.user.dto.UserDeactivateOperationRequest;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class UserOperationDeactivationClient {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public UserOperationDeactivationClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${integration.user-service.base-url:http://localhost:8080/api}") String baseUrl,
            @Value("${integration.internal-api.key:demo-internal-key}") String internalApiKey
    ) {
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl(baseUrl).defaultHeader(INTERNAL_API_KEY_HEADER, internalApiKey).build();
    }

    public OperationalImpactResponse preview(UserDeactivateOperationRequest request) {
        return post("/internal/deactivate/preview", request);
    }

    public OperationalImpactResponse deactivate(UserDeactivateOperationRequest request) {
        return post("/internal/deactivate/commit", request);
    }

    private OperationalImpactResponse post(String uri, UserDeactivateOperationRequest request) {
        try {
            return restClient.post().uri(uri).body(request).retrieve().onStatus(
                    HttpStatusCode::isError,
                    (rq, rs) -> handleErrorResponse(rs)
            ).body(OperationalImpactResponse.class);
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("User service deactivation request failed.", exception);
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException("Unable to reach user service.", exception);
        }
    }

    private void handleErrorResponse(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String message = extractErrorMessage(response);
        if (status == 404)
            throw new ResourceNotFoundException(message);
        if (status == 409)
            throw new ConflictException(message);
        throw new IllegalStateException(message.isBlank() ? "User service request failed." : message);
    }

    private String extractErrorMessage(ClientHttpResponse response) throws IOException {
        String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        if (body == null || body.isBlank())
            return "";
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull("message"))
                return node.get("message").asText();
            if (node.has("details") && node.get("details").isArray() && node.get("details").size() > 0)
                return node.get("details").get(0).asText();
        } catch (Exception ignored) {
        }
        return body;
    }
}
