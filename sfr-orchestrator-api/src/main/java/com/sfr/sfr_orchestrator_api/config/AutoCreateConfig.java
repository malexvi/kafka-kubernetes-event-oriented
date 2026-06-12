package com.sfr.sfr_orchestrator_api.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class AutoCreateConfig {

    private final KafkaTopicsProperties topicsProperties;

    @Bean
    public NewTopic libraryEvents() {
        return TopicBuilder
                .name(topicsProperties.getPackageDeliveryTopic())
                .partitions(3)
                .replicas(3)
                .build();
    }
}