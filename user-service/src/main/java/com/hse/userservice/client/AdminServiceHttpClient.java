package com.hse.userservice.client;

import com.hse.userservice.client.dto.CoworkingConfigSnapshot;
import com.hse.userservice.client.dto.CoworkingInfo;
import com.hse.userservice.exception.ExternalServiceException;
import com.hse.userservice.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AdminServiceHttpClient implements AdminServiceClient {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final String internalApiKey;

    public AdminServiceHttpClient(
            RestClient.Builder restClientBuilder,
            @Value("${integration.admin-service.base-url:http://localhost:8081/api}") String baseUrl,
            @Value("${integration.internal-api.key:demo-internal-key}") String internalApiKey
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    @Override
    public CoworkingInfo getCoworkingInfo(Long coworkingId) {
        return get("/internal/coworkings/{coworkingId}/info", coworkingId, CoworkingInfo.class);
    }

    @Override
    public CoworkingConfigSnapshot getCoworkingConfigSnapshot(Long coworkingId) {
        return get("/internal/config/coworkings/{coworkingId}/snapshot", coworkingId, CoworkingConfigSnapshot.class);
    }

    private <T> T get(String uriTemplate, Long coworkingId, Class<T> responseType) {
        try {
            return restClient.get()
                    .uri(uriTemplate, coworkingId)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::is4xxClientError, (request, response) -> {
                                if (response.getStatusCode().value() == 404) {
                                    throw new ResourceNotFoundException("Coworking not found: " + coworkingId);
                                }
                                throw new ExternalServiceException("Admin service rejected request with status " + response.getStatusCode()
                                        .value());
                            }
                    )
                    .onStatus(
                            HttpStatusCode::is5xxServerError, (request, response) -> {
                                throw new ExternalServiceException("Admin service is unavailable.");
                            }
                    )
                    .body(responseType);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new ExternalServiceException("Admin service request failed.", exception);
        } catch (ResourceAccessException exception) {
            throw new ExternalServiceException("Unable to reach admin service.", exception);
        }
    }
}
