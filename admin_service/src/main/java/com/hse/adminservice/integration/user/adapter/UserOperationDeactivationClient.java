package com.hse.adminservice.integration.user.adapter;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;

@Slf4j
@Component
public class UserOperationDeactivationClient {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private final RestClient restClient;

    public UserOperationDeactivationClient(
            RestClient.Builder builder,
            @Value("${integration.user-service.base-url:http://localhost:8080/api}") String baseUrl,
            @Value("${integration.internal-api.key}") String internalApiKey
    ) {
        this.restClient = builder.baseUrl(baseUrl).defaultHeader(INTERNAL_API_KEY_HEADER, internalApiKey).build();
    }

    public OperationalImpactResponse preview(Long coworkingId, String useCase, Object request) {
        return post("/internal/coworkings/{coworkingId}/booking-impact/" + useCase + "/preview", request, coworkingId);
    }

    public OperationalImpactResponse commit(Long coworkingId, String useCase, Object request) {
        return post("/internal/coworkings/{coworkingId}/booking-impact/" + useCase + "/commit", request, coworkingId);
    }

    private OperationalImpactResponse post(String uriTemplate, Object body, Object... uriVariables) {
        try {
            return restClient.post()
                    .uri(uriTemplate, uriVariables)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleErrorResponse(response))
                    .body(OperationalImpactResponse.class);
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
        if (status == 404) {
            throw new ResourceNotFoundException("Не удалось получить данные пользователей.");
        }
        if (status == 409) {
            throw new ConflictException("Не удалось выполнить действие с бронированиями пользователя.");
        }
        throw new IllegalStateException("Не удалось получить данные пользователей. Статус ответа: " + status);
    }
}
