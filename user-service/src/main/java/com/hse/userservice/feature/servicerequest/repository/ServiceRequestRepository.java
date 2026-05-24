package com.hse.userservice.feature.servicerequest.repository;

import com.hse.userservice.feature.servicerequest.domain.ServiceRequest;
import com.hse.userservice.feature.servicerequest.domain.ServiceRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByMembershipIdOrderByCreatedAtDesc(Long membershipId);

    Optional<ServiceRequest> findByIdAndMembershipIdIn(Long id, Collection<Long> membershipIds);

    @Query(
            """
                    select r from ServiceRequest r
                    where exists (
                        select m.id from Membership m
                        where m.id = r.membershipId and m.coworkingId = :coworkingId
                    )
                    order by r.createdAt desc
                    """
    )
    List<ServiceRequest> findAllByCoworkingIdOrderByCreatedAtDesc(@Param("coworkingId") Long coworkingId);

    @Query(
            """
                    select count(r) from ServiceRequest r
                    where r.status not in :statuses
                      and exists (
                        select m.id from Membership m
                        where m.id = r.membershipId and m.coworkingId = :coworkingId
                      )
                    """
    )
    long countByCoworkingIdAndStatusNotIn(
            @Param("coworkingId") Long coworkingId,
            @Param("statuses") Collection<ServiceRequestStatus> statuses
    );


    @Query(
            """
                    select r from ServiceRequest r
                    where r.id = :id
                      and exists (
                        select m.id from Membership m
                        where m.id = r.membershipId and m.coworkingId = :coworkingId
                      )
                    """
    )
    Optional<ServiceRequest> findByIdAndCoworkingId(@Param("id") Long id, @Param("coworkingId") Long coworkingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ServiceRequest r where r.id = :id and r.membershipId in :membershipIds")
    Optional<ServiceRequest> findByIdAndMembershipIdInForUpdate(
            @Param("id") Long id,
            @Param("membershipIds") Collection<Long> membershipIds
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
                    select r from ServiceRequest r
                    where r.id = :id
                      and exists (
                        select m.id from Membership m
                        where m.id = r.membershipId and m.coworkingId = :coworkingId
                      )
                    """
    )
    Optional<ServiceRequest> findByIdAndCoworkingIdForUpdate(
            @Param("id") Long id,
            @Param("coworkingId") Long coworkingId
    );
}
