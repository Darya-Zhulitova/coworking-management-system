package com.hse.userservice.feature.balance.repository;

import com.hse.userservice.feature.balance.domain.PayRequest;
import com.hse.userservice.feature.balance.domain.PayRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayRequestRepository extends JpaRepository<PayRequest, Long> {
    List<PayRequest> findAllByMembershipIdOrderByCreatedAtDesc(Long membershipId);

    @Query(
            """
                    select p from PayRequest p
                    where exists (
                        select m.id from Membership m
                        where m.id = p.membershipId and m.coworkingId = :coworkingId
                    )
                    order by p.createdAt desc
                    """
    )
    List<PayRequest> findAllByCoworkingIdOrderByCreatedAtDesc(@Param("coworkingId") Long coworkingId);

    @Query(
            """
                    select count(p) from PayRequest p
                    where p.status = :status
                      and exists (
                        select m.id from Membership m
                        where m.id = p.membershipId and m.coworkingId = :coworkingId
                      )
                    """
    )
    long countByCoworkingIdAndStatus(@Param("coworkingId") Long coworkingId, @Param("status") PayRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
                    select p from PayRequest p
                    where p.id = :id
                      and exists (
                        select m.id from Membership m
                        where m.id = p.membershipId and m.coworkingId = :coworkingId
                      )
                    """
    )
    Optional<PayRequest> findByIdAndCoworkingIdForUpdate(@Param("id") Long id, @Param("coworkingId") Long coworkingId);
}
