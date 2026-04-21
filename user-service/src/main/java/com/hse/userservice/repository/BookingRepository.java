package com.hse.userservice.repository;

import com.hse.userservice.domain.booking.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByMembershipIdInOrderByDateDesc(Collection<Long> membershipIds);

    List<Booking> findAllByMembershipIdOrderByDateDesc(Long membershipId);

    Optional<Booking> findByIdAndMembershipId(Long id, Long membershipId);

    List<Booking> findAllByPlaceIdInAndDateInAndActiveTrue(Collection<Long> placeIds, Collection<LocalDate> dates);

    boolean existsByPlaceIdAndDateAndActiveTrue(Long placeId, LocalDate date);

    long countByMembershipIdIn(Collection<Long> membershipIds);

    long countByMembershipIdInAndActiveTrue(Collection<Long> membershipIds);

    long countByCoworkingIdAndActiveTrueAndDateBetween(Long coworkingId, LocalDate from, LocalDate to);

    List<Booking> findAllByCoworkingIdAndActiveTrueAndDateIn(Long coworkingId, Collection<LocalDate> dates);

    List<Booking> findAllByCoworkingIdAndPlaceIdAndActiveTrueAndDateGreaterThanEqual(
            Long coworkingId,
            Long placeId,
            LocalDate from
    );

    List<Booking> findAllByCoworkingIdAndPlaceIdAndActiveTrueAndDate(Long coworkingId, Long placeId, LocalDate date);
}
