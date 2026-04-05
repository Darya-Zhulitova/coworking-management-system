package com.hse.adminservice.repository;

import com.hse.adminservice.entity.AdminCoworkingAccess;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminCoworkingAccessRepository extends JpaRepository<AdminCoworkingAccess, Long> {

    @EntityGraph(attributePaths = "coworking")
    List<AdminCoworkingAccess> findAllByAdminUserIdAndActiveTrueAndCoworkingArchivedFalse(Long adminUserId);

    boolean existsByAdminUserIdAndCoworkingIdAndActiveTrueAndCoworkingArchivedFalse(Long adminUserId, Long coworkingId);

    @EntityGraph(attributePaths = {"coworking", "adminUser"})
    Optional<AdminCoworkingAccess> findByAdminUserIdAndCoworkingId(Long adminUserId, Long coworkingId);

    @EntityGraph(attributePaths = {"coworking", "adminUser"})
    List<AdminCoworkingAccess> findAllByCoworkingIdAndCoworkingArchivedFalse(Long coworkingId);

    @EntityGraph(attributePaths = {"coworking", "adminUser"})
    Optional<AdminCoworkingAccess> findByIdAndCoworkingIdAndCoworkingArchivedFalse(Long id, Long coworkingId);
}
