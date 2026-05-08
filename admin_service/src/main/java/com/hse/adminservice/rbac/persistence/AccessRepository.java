package com.hse.adminservice.rbac.persistence;

import com.hse.adminservice.rbac.domain.Access;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccessRepository extends JpaRepository<Access, Long> {

    @EntityGraph(attributePaths = {"coworking", "role"})
    List<Access> findAllByAdminIdAndActiveTrueAndCoworkingArchivedFalse(Long adminId);

    boolean existsByAdminIdAndCoworkingIdAndActiveTrueAndCoworkingArchivedFalse(Long adminId, Long coworkingId);

    @EntityGraph(attributePaths = {"coworking", "admin", "role"})
    Optional<Access> findByAdminIdAndCoworkingId(Long adminId, Long coworkingId);

    @EntityGraph(attributePaths = {"coworking", "admin", "role"})
    List<Access> findAllByCoworkingIdAndCoworkingArchivedFalse(Long coworkingId);

    @EntityGraph(attributePaths = {"coworking", "admin", "role"})
    Optional<Access> findByIdAndCoworkingIdAndCoworkingArchivedFalse(Long id, Long coworkingId);
}
