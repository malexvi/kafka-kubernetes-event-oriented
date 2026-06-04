package com.sfr.sfr_orchestrator_api.application.event;

import java.time.Instant;
import java.util.UUID;

public sealed interface Event
        permits DeliveryRequestedEvent {

    UUID correlationId();

    Instant timestamp();
}
