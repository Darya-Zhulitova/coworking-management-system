package com.hse.userservice.repository;

import com.hse.userservice.domain.message.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findAllByServiceRequestIdOrderByTimestampAsc(Long serviceRequestId);
}
