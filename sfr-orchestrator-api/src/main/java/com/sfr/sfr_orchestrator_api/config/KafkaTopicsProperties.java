package com.sfr.sfr_orchestrator_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "kafka.topic")
@Getter
@Setter
public class KafkaTopicsProperties {

    private String packageDeliveryTopic;

}
