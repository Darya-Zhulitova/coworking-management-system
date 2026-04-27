package com.hse.adminservice.servicecatalog.persistence;

import com.hse.adminservice.servicecatalog.domain.ServiceRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRequestTypeRepository extends JpaRepository<ServiceRequestType, Long> {
    List<ServiceRequestType> findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(Long coworkingId);

    Optional<ServiceRequestType> findByIdAndCoworkingIdAndArchivedFalse(Long id, Long coworkingId);

    boolean existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(Long coworkingId, String name);
}
