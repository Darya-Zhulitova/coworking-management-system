package com.hse.adminservice.repository;

import com.hse.adminservice.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    @EntityGraph(attributePaths = "coworking")
    List<Role> findAllByCoworkingIdAndActiveTrueOrderByNameAsc(Long coworkingId);

    @EntityGraph(attributePaths = "coworking")
    Optional<Role> findByIdAndCoworkingIdAndActiveTrue(Long id, Long coworkingId);

    boolean existsByCoworkingIdAndNameIgnoreCase(Long coworkingId, String name);
}
