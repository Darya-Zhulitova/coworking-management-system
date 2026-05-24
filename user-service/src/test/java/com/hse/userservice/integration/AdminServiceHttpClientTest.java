package com.hse.userservice.integration;

import com.hse.userservice.common.exception.ExternalServiceException;
import com.hse.userservice.common.exception.ResourceNotFoundException;
import com.hse.userservice.integration.dto.BookingContext;
import com.hse.userservice.integration.dto.CoworkingInfo;
import com.hse.userservice.integration.dto.PlaceSummary;
import com.hse.userservice.integration.dto.ServiceRequestTypeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AdminServiceHttpClientTest {
    private MockRestServiceServer server;
    private AdminServiceHttpClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AdminServiceHttpClient(builder, "https://admin.example.test/api", "internal-secret");
    }

    @Test
    void getCoworkingInfoSendsInternalApiKeyAndMapsResponse() {
        server.expect(requestTo("https://admin.example.test/api/internal/coworkings/7/public-profile"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Api-Key", "internal-secret"))
                .andRespond(withSuccess("""
                        {
                          "id": 7,
                          "name": "Volga Hub",
                          "description": "desc",
                          "address": "Nizhny Novgorod",
                          "workingHoursLabel": "09:00-21:00",
                          "heroTitle": "Hero",
                          "heroText": "Text",
                          "imageUrls": ["https://example.test/image.png"],
                          "autoApproveMembership": true,
                          "floorMapEnabled": true,
                          "active": true
                        }
                        """, MediaType.APPLICATION_JSON));

        CoworkingInfo result = client.getCoworkingInfo(7L);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.name()).isEqualTo("Volga Hub");
        assertThat(result.imageUrls()).containsExactly("https://example.test/image.png");
        assertThat(result.autoApproveMembership()).isTrue();
        server.verify();
    }

    @Test
    void getCoworkingInfoByJoinTokenMapsInviteNotFoundToResourceNotFound() {
        server.expect(requestTo("https://admin.example.test/api/internal/coworkings/join/VOLGA/public-profile"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.getCoworkingInfoByJoinToken("VOLGA"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ссылка приглашения");
    }

    @Test
    void getBookingContextMapsNestedConfiguration() {
        server.expect(requestTo("https://admin.example.test/api/internal/coworkings/7/booking-context"))
                .andExpect(header("X-Internal-Api-Key", "internal-secret"))
                .andRespond(withSuccess("""
                        {
                          "coworkingId": 7,
                          "configVersion": 3,
                          "generatedAt": "2026-05-22T10:15:30",
                          "schedule": 62,
                          "name": "Volga Hub",
                          "floorMapEnabled": true,
                          "floors": [{"id": 1, "name": "Floor 1", "index": 1, "imageFileId": "floor", "imageUrl": "https://example.test/floor.png", "active": true}],
                          "tariffs": [],
                          "placeTypes": [],
                          "places": [],
                          "scheduleExceptions": [],
                          "placeClosings": []
                        }
                        """, MediaType.APPLICATION_JSON));

        BookingContext context = client.getBookingContext(7L);

        assertThat(context.coworkingId()).isEqualTo(7L);
        assertThat(context.configVersion()).isEqualTo(3L);
        assertThat(context.floors()).hasSize(1);
        assertThat(context.floors().getFirst().name()).isEqualTo("Floor 1");
    }

    @Test
    void getServiceRequestTypesReturnsEmptyListWhenBodyIsNull() {
        server.expect(requestTo("https://admin.example.test/api/internal/coworkings/7/service-request-types"))
                .andRespond(withSuccess());

        assertThat(client.getServiceRequestTypes(7L)).isEmpty();
    }

    @Test
    void getServiceRequestTypeMapsSuccessAndNotFound() {
        server.expect(requestTo("https://admin.example.test/api/internal/coworkings/7/service-request-types/5"))
                .andExpect(header("X-Internal-Api-Key", "internal-secret"))
                .andRespond(withSuccess("""
                        {"id": 5, "name": "Кофемашина", "cost": 15000, "version": 2, "active": true}
                        """, MediaType.APPLICATION_JSON));

        ServiceRequestTypeInfo type = client.getServiceRequestType(7L, 5L);

        assertThat(type.id()).isEqualTo(5L);
        assertThat(type.name()).isEqualTo("Кофемашина");
        assertThat(type.cost()).isEqualTo(15_000L);

        server.reset();
        server.expect(requestTo("https://admin.example.test/api/internal/coworkings/7/service-request-types/404"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.getServiceRequestType(7L, 404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Тип сервисной заявки");
    }

    @Test
    void getPlaceSummariesDoesNotCallAdminServiceWhenIdsAreEmpty() {
        server.expect(never(), requestTo("https://admin.example.test/api/internal/coworkings/7/places/summary?ids="));

        assertThat(client.getPlaceSummaries(7L, List.of())).isEmpty();
        assertThat(client.getPlaceSummaries(7L, null)).isEmpty();
        server.verify();
    }

    @Test
    void getPlaceSummariesDeduplicatesIdsAndMapsResponse() {
        server.expect(requestTo("https://admin.example.test/api/internal/coworkings/7/places/summary?ids=10,11"))
                .andExpect(header("X-Internal-Api-Key", "internal-secret"))
                .andRespond(withSuccess("""
                        [
                          {"id": 10, "name": "Desk 10", "floorName": "Floor 1", "placeTypeName": "Desk", "previewImageUrl": "preview", "fullImageUrl": "full"},
                          {"id": 11, "name": "Room 11", "floorName": "Floor 2", "placeTypeName": "Room", "previewImageUrl": null, "fullImageUrl": null}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<PlaceSummary> result = client.getPlaceSummaries(7L, List.of(10L, 11L, 10L));

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().id()).isEqualTo(10L);
        assertThat(result.getFirst().previewImageUrl()).isEqualTo("preview");
    }

    @Test
    void serverErrorIsWrappedAsExternalServiceException() {
        server.expect(requestTo("https://admin.example.test/api/internal/coworkings/7/public-profile"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getCoworkingInfo(7L))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Не удалось получить данные коворкингов");
    }
}
