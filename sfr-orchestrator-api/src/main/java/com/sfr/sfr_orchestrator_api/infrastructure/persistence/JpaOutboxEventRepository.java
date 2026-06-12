package com.sfr.sfr_orchestrator_api.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sfr.sfr_orchestrator_api.domain.entity.OutboxEvent;

public interface JpaOutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();

}