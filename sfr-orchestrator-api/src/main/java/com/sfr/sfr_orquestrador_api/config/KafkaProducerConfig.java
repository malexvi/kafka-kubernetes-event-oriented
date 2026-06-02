package com.sfr.sfr_orquestrador_api.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.sfr.sfr_orquestrador_api.avro.RequestStartedEvent;

import io.confluent.kafka.serializers.KafkaAvroSerializer;


@Configuration
public class KafkaProducerConfig {
    
    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${schema-registry.url}")
    private String schemaRegistryUrl;

    @Bean
    public ProducerFactory<String, RequestStartedEvent> producerFactory() {

        Map<String, Object> config = new HashMap<>();

        config.put(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapServers
        );

        config.put(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class
        );

        config.put(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            KafkaAvroSerializer.class
        );

        config.put(
            "schema.registry.url",
            schemaRegistryUrl
        );

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, RequestStartedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

}
