package com.hse.userservice.integration;

import com.hse.userservice.common.exception.ExternalServiceException;
import com.hse.userservice.common.exception.ResourceNotFoundException;
import com.hse.userservice.integration.dto.BookingContext;
import com.hse.userservice.integration.dto.CoworkingInfo;
import com.hse.userservice.integration.dto.PlaceSummary;
import com.hse.userservice.integration.dto.ServiceRequestTypeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AdminServiceHttpClient implements AdminServiceClient {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final String internalApiKey;

    public AdminServiceHttpClient(
            RestClient.Builder restClientBuilder,
            @Value("${integration.admin-service.base-url:http://localhost:8081/api}") String baseUrl,
            @Value("${integration.internal-api.key}") String internalApiKey
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    @Override
    public CoworkingInfo getCoworkingInfo(Long coworkingId) {
        return get("/internal/coworkings/{coworkingId}/public-profile", coworkingId, CoworkingInfo.class);
    }

    @Override
    public CoworkingInfo getCoworkingInfoByJoinToken(String joinToken) {
        return getByToken("/internal/coworkings/join/{joinToken}/public-profile", joinToken, CoworkingInfo.class);
    }

    @Override
    public BookingContext getBookingContext(Long coworkingId) {
        return get("/internal/coworkings/{coworkingId}/booking-context", coworkingId, BookingContext.class);
    }

    @Override
    public List<ServiceRequestTypeInfo> getServiceRequestTypes(Long coworkingId) {
        return getArray(
                "/internal/coworkings/{coworkingId}/service-request-types",
                coworkingId,
                ServiceRequestTypeInfo[].class
        );
    }

    @Override
    public ServiceRequestTypeInfo getServiceRequestType(Long coworkingId, Long typeId) {
        try {
            return restClient.get().uri(
                    "/internal/coworkings/{coworkingId}/service-request-types/{typeId}",
                    coworkingId,
                    typeId
            ).header(
                    INTERNAL_API_KEY_HEADER,
                    internalApiKey
            ).retrieve().onStatus(
                    HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new ResourceNotFoundException("Тип сервисной заявки не найден: " + typeId);
                        }
                        throw new ExternalServiceException("Не удалось получить данные коворкингов. Статус ответа: " + response.getStatusCode()
                                .value());
                    }
            ).onStatus(
                    HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new ExternalServiceException("Не удалось получить данные коворкингов.");
                    }
            ).body(ServiceRequestTypeInfo.class);
        } catch (RestClientResponseException exception) {
            log.error("Admin service request failed. serviceRequestTypeId={}", typeId, exception);
            throw new ExternalServiceException("Не удалось получить данные коворкингов.", exception);
        } catch (ResourceAccessException exception) {
            log.error("Admin service request failed. serviceRequestTypeId={}", typeId, exception);
            throw new ExternalServiceException("Не удалось получить данные коворкингов.", exception);
        }
    }

    @Override
    public List<PlaceSummary> getPlaceSummaries(Long coworkingId, Collection<Long> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return List.of();
        }
        String ids = placeIds.stream().distinct().map(String::valueOf).collect(Collectors.joining(","));
        try {
            PlaceSummary[] summaries = restClient.get()
                    .uri(builder -> builder.path("/internal/coworkings/{coworkingId}/places/summary")
                            .queryParam("ids", ids)
                            .build(coworkingId))
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::is4xxClientError, (request, response) -> {
                                if (response.getStatusCode().value() == 404) {
                                    throw new ResourceNotFoundException("Коворкинг не найден: " + coworkingId);
                                }
                                throw new ExternalServiceException(
                                        "Не удалось получить данные коворкингов. Статус ответа: " + response.getStatusCode()
                                                .value());
                            }
                    )
                    .onStatus(
                            HttpStatusCode::is5xxServerError, (request, response) -> {
                                throw new ExternalServiceException("Не удалось получить данные коворкингов.");
                            }
                    )
                    .body(PlaceSummary[].class);
            return summaries == null ? List.of() : Arrays.asList(summaries);
        } catch (RestClientResponseException exception) {
            log.error("Admin service request failed. coworkingId={}", coworkingId, exception);
            throw new ExternalServiceException("Не удалось получить данные коворкингов.", exception);
        } catch (ResourceAccessException exception) {
            log.error("Admin service request failed. coworkingId={}", coworkingId, exception);
            throw new ExternalServiceException("Не удалось получить данные коворкингов.", exception);
        }
    }

    private <T> T getByToken(String uriTemplate, String joinToken, Class<T> responseType) {
        try {
            return restClient.get()
                    .uri(uriTemplate, joinToken)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::is4xxClientError, (request, response) -> {
                                if (response.getStatusCode().value() == 404) {
                                    throw new ResourceNotFoundException("Ссылка приглашения в коворкинг не найдена: " + joinToken);
                                }
                                throw new ExternalServiceException(
                                        "Не удалось получить данные коворкингов. Статус ответа: " + response.getStatusCode()
                                                .value());
                            }
                    )
                    .onStatus(
                            HttpStatusCode::is5xxServerError, (request, response) -> {
                                throw new ExternalServiceException("Не удалось получить данные коворкингов.");
                            }
                    )
                    .body(responseType);
        } catch (RestClientResponseException exception) {
            log.error("Admin service request failed. uriTemplate={}", uriTemplate, exception);
            throw new ExternalServiceException("Не удалось получить данные коворкингов.", exception);
        } catch (ResourceAccessException exception) {
            log.error("Admin service request failed. uriTemplate={}", uriTemplate, exception);
            throw new ExternalServiceException("Не удалось получить данные коворкингов.", exception);
        }
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
                                    throw new ResourceNotFoundException("Коворкинг не найден: " + coworkingId);
                                }
                                throw new ExternalServiceException(
                                        "Не удалось получить данные коворкингов. Статус ответа: " + response.getStatusCode()
                                                .value());
                            }
                    )
                    .onStatus(
                            HttpStatusCode::is5xxServerError, (request, response) -> {
                                throw new ExternalServiceException("Не удалось получить данные коворкингов.");
                            }
                    )
                    .body(responseType);
        } catch (RestClientResponseException exception) {
            log.error("Admin service request failed. uriTemplate={}", uriTemplate, exception);
            throw new ExternalServiceException("Не удалось получить данные коворкингов.", exception);
        } catch (ResourceAccessException exception) {
            log.error("Admin service request failed. uriTemplate={}", uriTemplate, exception);
            throw new ExternalServiceException("Не удалось получить данные коворкингов.", exception);
        }
    }

    private <T> List<T> getArray(String uriTemplate, Long coworkingId, Class<T[]> responseType) {
        T[] response = get(uriTemplate, coworkingId, responseType);
        return response == null ? List.of() : Arrays.asList(response);
    }
}
