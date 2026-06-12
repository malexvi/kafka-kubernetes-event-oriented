package com.sfr.sfr_orchestrator_api.application.port;

import java.util.List;

import com.sfr.sfr_orchestrator_api.domain.entity.OutboxEvent;

public interface OutboxRepositoryPort {

    void save(OutboxEvent outboxEvents);

    List<OutboxEvent> findUnprocessedEvents();
}
