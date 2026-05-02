package com.hse.userservice.config;

import com.hse.userservice.domain.booking.Booking;
import com.hse.userservice.domain.booking.BookingStatus;
import com.hse.userservice.domain.ledger.LedgerEntry;
import com.hse.userservice.domain.ledger.LedgerEntryType;
import com.hse.userservice.domain.membership.Membership;
import com.hse.userservice.domain.membership.MembershipStatus;
import com.hse.userservice.domain.message.Message;
import com.hse.userservice.domain.message.MessageAuthorType;
import com.hse.userservice.domain.payrequest.PayRequest;
import com.hse.userservice.domain.payrequest.PayRequestStatus;
import com.hse.userservice.domain.request.ServiceRequest;
import com.hse.userservice.domain.request.ServiceRequestStatus;
import com.hse.userservice.domain.user.User;
import com.hse.userservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class MockUserDataRunner implements CommandLineRunner {
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PayRequestRepository payRequestRepository;
    private final BookingRepository bookingRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (membershipRepository.count() > 0) {
            return;
        }

        User primary = seedUser("user@test.test", "Дарья", "Разработчик");
        User second = seedUser("user2@test.test", "Иван", "Дизайнер");
        User third = seedUser("user3@test.test", "Дмитрий", "Менеджер");

        Membership active = seedMembership(
                primary.getId(),
                1L,
                MembershipStatus.ACTIVE,
                LocalDateTime.now().minusDays(40)
        );
        Membership secondActive = seedMembership(
                second.getId(),
                1L,
                MembershipStatus.PENDING,
                LocalDateTime.now().minusDays(25)
        );
        Membership thirdActive = seedMembership(
                third.getId(),
                1L,
                MembershipStatus.BLOCKED,
                LocalDateTime.now().minusDays(18)
        );
        Membership pending = seedMembership(
                primary.getId(),
                2L,
                MembershipStatus.PENDING,
                LocalDateTime.now().minusDays(12)
        );
        Membership blocked = seedMembership(
                primary.getId(),
                3L,
                MembershipStatus.BLOCKED,
                LocalDateTime.now().minusDays(65)
        );

        seedLedger(
                active.getId(),
                1L,
                LedgerEntryType.DEPOSIT,
                1000000L,
                "Пополнение счёта",
                "Оплата по СБП",
                LocalDateTime.now().minusDays(10)
        );
        seedLedger(
                active.getId(),
                1L,
                LedgerEntryType.BOOKING_CHARGE,
                -300000L,
                "Оплата бронирования",
                "Переговорная Ока - 2026-05-14",
                LocalDateTime.now().minusDays(9)
        );
        seedLedger(
                active.getId(),
                1L,
                LedgerEntryType.BOOKING_CHARGE,
                -300000L,
                "Оплата бронирования",
                "Стол A-1 - 2026-04-25",
                LocalDateTime.now().minusDays(9)
        );
        seedLedger(
                active.getId(),
                1L,
                LedgerEntryType.BOOKING_CHARGE,
                -650000L,
                "Оплата бронирования",
                "Кабинет Витязь - 2026-04-22",
                LocalDateTime.now().minusDays(9)
        );
        seedLedger(
                active.getId(),
                1L,
                LedgerEntryType.CANCELLATION_REFUND,
                700000L,
                "Возврат за отмену",
                "Кабинет Витязь - 2026-04-22",
                LocalDateTime.now().minusDays(8)
        );
        //        seedLedger(active.getId(), 1L, LedgerEntryType.DAY_CLOSURE_COMPENSATION, 50000L, "Компенсация за закрытие дня", "Переговорная была закрыта администратором", LocalDateTime.now().minusDays(20));
        seedLedger(
                blocked.getId(),
                3L,
                LedgerEntryType.DEPOSIT,
                2500000L,
                "Пополнение счёта",
                "Оплата по СБП",
                LocalDateTime.now().minusDays(10)
        );
        seedLedger(
                blocked.getId(),
                1L,
                LedgerEntryType.MEMBERSHIP_BLOCK_COMPENSATION,
                260000L,
                "Компенсация при блокировке membership",
                "Возврат за затронутые будущие бронирования",
                LocalDateTime.now().minusDays(21)
        );
        //        seedLedger(secondActive.getId(), 1L, LedgerEntryType.DEPOSIT, 190000L, "Пополнение баланса", "User 2 top-up", LocalDateTime.now().minusDays(7));
        //        seedLedger(secondActive.getId(), 1L, LedgerEntryType.BOOKING_CHARGE, -36000L, "Оплата бронирования", "Desk D-01", LocalDateTime.now().minusDays(5));
        //        seedLedger(thirdActive.getId(), 1L, LedgerEntryType.DEPOSIT, 110000L, "Пополнение баланса", "User 3 top-up", LocalDateTime.now().minusDays(14));

        seedPayRequest(
                active.getId(),
                250000L,
                PayRequestStatus.APPROVED,
                "Пополнение баланса",
                "Проверено, платёж пришел",
                LocalDateTime.now().minusDays(10)
        );
        //        seedPayRequest(active.getId(), -70000L, PayRequestStatus.Pending, "Вывод остатка", null, LocalDateTime.now().minusHours(10));
        seedPayRequest(
                blocked.getId(),
                250000L,
                PayRequestStatus.REJECTED,
                "Пополнение для будущих визитов",
                "Rejected in seed data",
                LocalDateTime.now().minusDays(30)
        );
        //        seedPayRequest(secondActive.getId(), 500000L, PayRequestStatus.Pending, "Need more balance", null, LocalDateTime.now().minusDays(1));
        //        seedPayRequest(pending.getId(), 120000L, PayRequestStatus.Pending, "Пополнение перед активацией", null, LocalDateTime.now().minusDays(2));

        seedBooking(
                active.getId(),
                1L,
                1L,
                LocalDate.now().plusDays(1),
                100000L,
                true,
                "REQ-23D742A3F9CA",
                1L,
                100000,
                0
        );
        seedBooking(
                active.getId(),
                1L,
                2L,
                LocalDate.now().plusDays(20),
                300000L,
                true,
                "REQ-23D742A3F9CA",
                2L,
                300000,
                0
        );
        seedBooking(
                active.getId(),
                1L,
                3L,
                LocalDate.now().minusDays(20),
                650000L,
                false,
                "REQ-23D742A3F9CA",
                3L,
                650000,
                0
        );
        seedBooking(
                secondActive.getId(),
                1L,
                2L,
                LocalDate.now().plusDays(25),
                18000L,
                true,
                "REQ-APR-08",
                1L,
                180000,
                10
        );
        seedBooking(
                thirdActive.getId(),
                1L,
                3L,
                LocalDate.now().plusDays(5),
                300000L,
                true,
                "REQ-APR-09",
                2L,
                300000,
                0
        );
        seedBooking(
                blocked.getId(),
                3L,
                6L,
                LocalDate.now().minusDays(60),
                260000L,
                false,
                "REQ-FEB-09",
                5L,
                260000,
                0
        );
        //
        //        ServiceRequest sr1 = seedServiceRequest(active.getId(), 1L, "Заявка 1", 15000, ServiceRequestStatus.IN_PROGRESS, LocalDateTime.now().minusDays(2), null);
        //        ServiceRequest sr2 = seedServiceRequest(active.getId(), 2L, "Заявка 2", 6000, ServiceRequestStatus.RESOLVED, LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(10).plusHours(5));
        //        ServiceRequest sr3 = seedServiceRequest(blocked.getId(), 4L, "Заявка 3", 0, ServiceRequestStatus.NEW, LocalDateTime.now().minusDays(4), null);
        //        ServiceRequest sr4 = seedServiceRequest(secondActive.getId(), 3L, "Аренда повербанка на день", 9000, ServiceRequestStatus.NEW, LocalDateTime.now().minusHours(8), null);
        //
        //        seedMessage(sr1.getId(), MessageAuthorType.USER, primary.getId(), "Нужна настройка места к 15:00.", LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2).plusMinutes(10));
        //        seedMessage(sr1.getId(), MessageAuthorType.ADMIN, 1L, "Место подготовлено, проверьте доступ за 10 минут до встречи.", LocalDateTime.now().minusDays(1), null);
        //        seedMessage(sr3.getId(), MessageAuthorType.USER, primary.getId(), "Прошу пересмотреть статус блокировки.", LocalDateTime.now().minusDays(4), null);
        //        seedMessage(sr4.getId(), MessageAuthorType.USER, second.getId(), "Please bring HDMI adapter.", LocalDateTime.now().minusHours(8), null);
    }

    private User seedUser(String email, String name, String description) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setDescription(description);
            user.setPasswordHash(passwordEncoder.encode("123456"));
            return userRepository.save(user);
        });
    }

    private Membership seedMembership(Long userId, Long coworkingId, MembershipStatus status, LocalDateTime createdAt) {
        Membership membership = membershipRepository.findByUserIdAndCoworkingId(userId, coworkingId).orElseGet(
                Membership::new);
        membership.setUserId(userId);
        membership.setCoworkingId(coworkingId);
        membership.setStatus(status);
        membership.setCreatedAt(createdAt);
        membership.setApprovedAt(status == MembershipStatus.ACTIVE ? createdAt.plusHours(1) : null);
        membership.setBlockedAt(status == MembershipStatus.BLOCKED ? createdAt.plusDays(1) : null);
        return membershipRepository.save(membership);
    }

    private void seedLedger(
            Long membershipId,
            Long coworkingId,
            LedgerEntryType type,
            Long amount,
            String name,
            String comment,
            LocalDateTime timestamp
    ) {
        LedgerEntry entry = new LedgerEntry();
        entry.setMembershipId(membershipId);
        entry.setCoworkingId(coworkingId);
        entry.setType(type);
        entry.setAmount(amount);
        entry.setName(name);
        entry.setComment(comment);
        entry.setTimestamp(timestamp);
        ledgerEntryRepository.save(entry);
    }

    private void seedPayRequest(
            Long membershipId,
            Long amount,
            PayRequestStatus status,
            String userComment,
            String adminComment,
            LocalDateTime createdAt
    ) {
        PayRequest request = new PayRequest();
        request.setMembershipId(membershipId);
        request.setAmount(amount);
        request.setStatus(status);
        request.setUserComment(userComment);
        request.setAdminComment(adminComment);
        request.setCreatedAt(createdAt);
        payRequestRepository.save(request);
    }

    private void seedBooking(
            Long membershipId,
            Long coworkingId,
            Long placeId,
            LocalDate date,
            Long cost,
            boolean active,
            String requestId,
            Long tariffId,
            Integer pricePerDay,
            Integer discountPercent
    ) {
        Booking booking = new Booking();
        booking.setMembershipId(membershipId);
        booking.setCoworkingId(coworkingId);
        booking.setPlaceId(placeId);
        booking.setDate(date);
        booking.setCost(cost);
        booking.setActive(active);
        booking.setStatus(active ? BookingStatus.ACTUAL : BookingStatus.CANCELED_ADMIN);
        booking.setRequestId(requestId);
        booking.setTariffId(tariffId);
        booking.setPricePerDay(pricePerDay);
        booking.setAppliedDiscountPercent(discountPercent);
        booking.setFullRefundHoursBefore(48);
        booking.setLateCancellationRefundPercent(50);
        booking.setCancellationCompensationCoefficient(new BigDecimal("1.0000"));
        booking.setDayClosureCompensationCoefficient(new BigDecimal("1.0000"));
        booking.setMembershipBlockCompensationCoefficient(new BigDecimal("0.5000"));
        bookingRepository.save(booking);
    }

    private ServiceRequest seedServiceRequest(
            Long membershipId,
            Long typeId,
            String name,
            Integer cost,
            ServiceRequestStatus status,
            LocalDateTime createdAt,
            LocalDateTime resolvedAt
    ) {
        ServiceRequest request = new ServiceRequest();
        request.setMembershipId(membershipId);
        request.setTypeId(typeId);
        request.setName(name);
        request.setCost(cost);
        request.setStatus(status);
        request.setCreatedAt(createdAt);
        request.setResolvedAt(resolvedAt);
        return serviceRequestRepository.save(request);
    }

    private void seedMessage(
            Long serviceRequestId,
            MessageAuthorType authorType,
            Long authorId,
            String text,
            LocalDateTime timestamp,
            LocalDateTime readAt
    ) {
        Message message = new Message();
        message.setServiceRequestId(serviceRequestId);
        message.setAuthorType(authorType);
        message.setAuthorId(authorId);
        message.setText(text);
        message.setTimestamp(timestamp);
        message.setReadAt(readAt);
        messageRepository.save(message);
    }
}
