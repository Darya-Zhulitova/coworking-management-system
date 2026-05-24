package com.hse.userservice.feature.servicerequest.repository;

import com.hse.userservice.feature.servicerequest.domain.ServiceRequestAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ServiceRequestAttachmentRepository extends JpaRepository<ServiceRequestAttachment, Long> {
    List<ServiceRequestAttachment> findAllByMessageIdIn(Collection<Long> messageIds);
}
