package com.sfr.sfr_orchestrator_api.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sfr.sfr_orchestrator_api.application.port.OutboxRepositoryPort;
import com.sfr.sfr_orchestrator_api.domain.entity.OutboxEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxEventPersistenceAdapter implements OutboxRepositoryPort {

    private final JpaOutboxEventRepository jpaOutboxPackageRepository;

    @Override
    public void save(OutboxEvent outboxEvents) {
        jpaOutboxPackageRepository.save(outboxEvents);
    }

    @Override
    public List<OutboxEvent> findUnprocessedEvents() {
        return jpaOutboxPackageRepository.findByProcessedFalseOrderByCreatedAtAsc();
    }

}
