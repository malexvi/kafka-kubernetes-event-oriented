package com.sfr.sfr_orchestrator_api.application.port;

import com.sfr.sfr_orchestrator_api.application.event.Event;

public interface EventPublisher {

    <T extends Event> void publish(T event);

}
