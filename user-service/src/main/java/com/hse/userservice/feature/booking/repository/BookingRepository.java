package com.hse.userservice.feature.booking.repository;

import com.hse.userservice.feature.booking.domain.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByMembershipIdOrderByDateDesc(Long membershipId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id and b.membershipId = :membershipId")
    Optional<Booking> findByIdAndMembershipIdForUpdate(@Param("id") Long id, @Param("membershipId") Long membershipId);

    List<Booking> findAllByPlaceIdInAndDateInAndActiveTrue(Collection<Long> placeIds, Collection<LocalDate> dates);

    List<Booking> findAllByMembershipIdInAndPlaceIdAndActiveTrueAndDateGreaterThanEqualOrderByDateAscIdAsc(
            Collection<Long> membershipIds,
            Long placeId,
            LocalDate from
    );

    @Query(
            """
                    select b from Booking b
                    where b.placeId = :placeId
                      and b.active = true
                      and b.date >= :from
                      and exists (
                        select m.id from Membership m
                        where m.id = b.membershipId and m.coworkingId = :coworkingId
                      )
                    order by b.date asc, b.id asc
                    """
    )
    List<Booking> findAllActiveByCoworkingIdAndPlaceIdAndDateGreaterThanEqual(
            @Param("coworkingId") Long coworkingId,
            @Param("placeId") Long placeId,
            @Param("from") LocalDate from
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
                    select b from Booking b
                    where b.placeId = :placeId
                      and b.active = true
                      and b.date >= :from
                      and exists (
                        select m.id from Membership m
                        where m.id = b.membershipId and m.coworkingId = :coworkingId
                      )
                    order by b.id asc
                    """
    )
    List<Booking> findActiveByCoworkingIdAndPlaceIdAndDateGreaterThanEqualForUpdate(
            @Param("coworkingId") Long coworkingId,
            @Param("placeId") Long placeId,
            @Param("from") LocalDate from
    );

    @Query(
            """
                    select b from Booking b
                    where b.placeId = :placeId
                      and b.active = true
                      and b.date = :date
                      and exists (
                        select m.id from Membership m
                        where m.id = b.membershipId and m.coworkingId = :coworkingId
                      )
                    order by b.date asc, b.id asc
                    """
    )
    List<Booking> findAllActiveByCoworkingIdAndPlaceIdAndDate(
            @Param("coworkingId") Long coworkingId,
            @Param("placeId") Long placeId,
            @Param("date") LocalDate date
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
                    select b from Booking b
                    where b.placeId = :placeId
                      and b.active = true
                      and b.date = :date
                      and exists (
                        select m.id from Membership m
                        where m.id = b.membershipId and m.coworkingId = :coworkingId
                      )
                    order by b.id asc
                    """
    )
    List<Booking> findActiveByCoworkingIdAndPlaceIdAndDateForUpdate(
            @Param("coworkingId") Long coworkingId,
            @Param("placeId") Long placeId,
            @Param("date") LocalDate date
    );

    @Query(
            """
                    select b from Booking b
                    where b.active = true
                      and b.date in :dates
                      and exists (
                        select m.id from Membership m
                        where m.id = b.membershipId and m.coworkingId = :coworkingId
                      )
                    order by b.date asc, b.id asc
                    """
    )
    List<Booking> findAllActiveByCoworkingIdAndDateIn(
            @Param("coworkingId") Long coworkingId,
            @Param("dates") Collection<LocalDate> dates
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
                    select b from Booking b
                    where b.active = true
                      and b.date in :dates
                      and exists (
                        select m.id from Membership m
                        where m.id = b.membershipId and m.coworkingId = :coworkingId
                      )
                    order by b.id asc
                    """
    )
    List<Booking> findActiveByCoworkingIdAndDateInForUpdate(
            @Param("coworkingId") Long coworkingId,
            @Param("dates") Collection<LocalDate> dates
    );


    @Query(
            """
                    select b from Booking b
                    where b.membershipId = :membershipId
                      and b.active = true
                      and b.date >= :from
                      and exists (
                        select m.id from Membership m
                        where m.id = b.membershipId and m.coworkingId = :coworkingId
                      )
                    order by b.date asc, b.id asc
                    """
    )
    List<Booking> findAllActiveByCoworkingIdAndMembershipIdAndDateGreaterThanEqual(
            @Param("coworkingId") Long coworkingId,
            @Param("membershipId") Long membershipId,
            @Param("from") LocalDate from
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
                    select b from Booking b
                    where b.membershipId = :membershipId
                      and b.active = true
                      and b.date >= :from
                      and exists (
                        select m.id from Membership m
                        where m.id = b.membershipId and m.coworkingId = :coworkingId
                      )
                    order by b.id asc
                    """
    )
    List<Booking> findActiveByCoworkingIdAndMembershipIdAndDateGreaterThanEqualForUpdate(
            @Param("coworkingId") Long coworkingId,
            @Param("membershipId") Long membershipId,
            @Param("from") LocalDate from
    );

}
