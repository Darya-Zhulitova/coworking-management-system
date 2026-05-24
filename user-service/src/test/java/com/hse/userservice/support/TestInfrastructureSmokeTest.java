package com.hse.userservice.support;

import com.hse.userservice.feature.balance.domain.LedgerEntry;
import com.hse.userservice.feature.balance.domain.PayRequest;
import com.hse.userservice.feature.balance.repository.LedgerEntryRepository;
import com.hse.userservice.feature.balance.repository.PayRequestRepository;
import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.repository.BookingRepository;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.repository.MembershipRepository;
import com.hse.userservice.feature.servicerequest.domain.ServiceRequest;
import com.hse.userservice.feature.servicerequest.repository.ServiceRequestRepository;
import com.hse.userservice.feature.user.domain.User;
import com.hse.userservice.feature.user.repository.UserRepository;
import com.hse.userservice.integration.AdminServiceClient;
import com.hse.userservice.support.config.TestAdminServiceClientConfig.FakeAdminServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static com.hse.userservice.support.builder.BookingTestBuilder.booking;
import static com.hse.userservice.support.builder.LedgerEntryTestBuilder.ledgerEntry;
import static com.hse.userservice.support.builder.MembershipTestBuilder.membership;
import static com.hse.userservice.support.builder.PayRequestTestBuilder.payRequest;
import static com.hse.userservice.support.builder.ServiceRequestTestBuilder.serviceRequest;
import static com.hse.userservice.support.builder.UserTestBuilder.user;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class TestInfrastructureSmokeTest extends IntegrationTestSupport {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private PayRequestRepository payRequestRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private AdminServiceClient adminServiceClient;

    @Test
    void buildersPersistCoreUserDomainEntities() {
        User savedUser = userRepository.save(user().email("resident-smoke@example.test").build());
        Membership savedMembership = membershipRepository.save(membership().userId(savedUser.getId()).build());
        Booking savedBooking = bookingRepository.save(booking().membershipId(savedMembership.getId()).build());
        LedgerEntry savedLedgerEntry = ledgerEntryRepository.save(ledgerEntry()
                .membershipId(savedMembership.getId())
                .referenceId(savedBooking.getId())
                .build());
        PayRequest savedPayRequest = payRequestRepository.save(payRequest().membershipId(savedMembership.getId()).build());
        ServiceRequest savedServiceRequest = serviceRequestRepository.save(serviceRequest()
                .membershipId(savedMembership.getId())
                .build());

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedMembership.getId()).isNotNull();
        assertThat(savedBooking.getId()).isNotNull();
        assertThat(savedLedgerEntry.getId()).isNotNull();
        assertThat(savedPayRequest.getId()).isNotNull();
        assertThat(savedServiceRequest.getId()).isNotNull();
    }

    @Test
    void fakeAdminServiceClientIsPrimaryTestClient() {
        assertThat(adminServiceClient).isInstanceOf(FakeAdminServiceClient.class);
        assertThat(adminServiceClient.getCoworkingInfo(1L).name()).isEqualTo("Test Coworking");
        assertThat(adminServiceClient.getBookingContext(1L).places()).hasSize(1);
        assertThat(adminServiceClient.getServiceRequestType(1L, 1L).name()).isEqualTo("Water");
        assertThat(adminServiceClient.getPlaceSummaries(1L, java.util.List.of(1L))).hasSize(1);
    }
}
