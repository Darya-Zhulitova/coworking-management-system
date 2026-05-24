package com.hse.userservice.feature.servicerequest.repository;

import com.hse.userservice.feature.servicerequest.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findAllByServiceRequestIdOrderByTimestampAsc(Long serviceRequestId);
}
