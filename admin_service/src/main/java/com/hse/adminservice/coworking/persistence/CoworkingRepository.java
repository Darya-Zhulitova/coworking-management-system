package com.hse.adminservice.coworking.persistence;

import com.hse.adminservice.coworking.domain.Coworking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoworkingRepository extends JpaRepository<Coworking, Long> {
    List<Coworking> findAllByArchivedFalse();

    List<Coworking> findAllByOwnerIdAndArchivedFalse(Long ownerId);

    List<Coworking> findAllByArchivedTrue();

    Optional<Coworking> findByIdAndArchivedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coworking c where c.id = :id and c.archived = false")
    Optional<Coworking> findByIdAndArchivedFalseForUpdate(@Param("id") Long id);

    Optional<Coworking> findByJoinTokenAndArchivedFalse(String joinToken);

    boolean existsByJoinToken(String joinToken);

    boolean existsByIdAndArchivedFalse(Long id);
}
