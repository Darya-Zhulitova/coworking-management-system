package com.hse.adminservice.rbac.repository;

import com.hse.adminservice.rbac.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    @EntityGraph(attributePaths = "coworking")
    List<Role> findAllByCoworkingIdOrderByNameAsc(Long coworkingId);

    @EntityGraph(attributePaths = "coworking")
    List<Role> findAllByCoworkingIdAndActiveTrueOrderByNameAsc(Long coworkingId);

    @EntityGraph(attributePaths = "coworking")
    Optional<Role> findByIdAndCoworkingId(Long id, Long coworkingId);

    @EntityGraph(attributePaths = "coworking")
    Optional<Role> findByIdAndCoworkingIdAndActiveTrue(Long id, Long coworkingId);

    boolean existsByCoworkingIdAndNameIgnoreCase(Long coworkingId, String name);

    boolean existsByCoworkingIdAndNameIgnoreCaseAndIdNot(Long coworkingId, String name, Long id);
}
