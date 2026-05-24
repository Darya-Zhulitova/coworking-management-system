package com.hse.userservice.integration;

import com.hse.userservice.integration.dto.BookingContext;
import com.hse.userservice.integration.dto.CoworkingInfo;
import com.hse.userservice.integration.dto.PlaceSummary;
import com.hse.userservice.integration.dto.ServiceRequestTypeInfo;

import java.util.Collection;
import java.util.List;

public interface AdminServiceClient {
    CoworkingInfo getCoworkingInfo(Long coworkingId);

    CoworkingInfo getCoworkingInfoByJoinToken(String joinToken);

    BookingContext getBookingContext(Long coworkingId);

    List<ServiceRequestTypeInfo> getServiceRequestTypes(Long coworkingId);

    ServiceRequestTypeInfo getServiceRequestType(Long coworkingId, Long typeId);

    List<PlaceSummary> getPlaceSummaries(Long coworkingId, Collection<Long> placeIds);
}
