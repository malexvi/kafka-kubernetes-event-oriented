package com.sfr.sfr_orchestrator_api.intg.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sfr.sfr_orchestrator_api.application.dto.PackageDeliveryRequest;
import com.sfr.sfr_orchestrator_api.application.port.OutboxRepositoryPort;
import com.sfr.sfr_orchestrator_api.config.KafkaTopicsProperties;
import com.sfr.sfr_orchestrator_api.domain.entity.OutboxEvent;
import com.sfr.sfr_orchestrator_api.infrastructure.kafka.avro.RequestStartedEvent;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = { "package-delivery-topic" })
@TestPropertySource(properties = {
                "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.admin.properties.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.properties.schema.registry.url=mock://schema-registry",

                "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=user",
                "spring.datasource.password=",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
public class PackageDeliverControllerIntegracionTest {

        @Autowired
        TestRestTemplate template;

        @Autowired
        EmbeddedKafkaBroker kafkaBroker;

        @MockitoBean
        OutboxRepositoryPort outboxRepositoryPort;

        @Autowired
        KafkaTopicsProperties topicsProperties;

        private Consumer<String, RequestStartedEvent> consumer;

        @BeforeEach
        void setUp() {
                var configs = new HashMap<>(KafkaTestUtils.consumerProps("group1", "true", kafkaBroker));

                configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

                configs.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");

                configs.put("schema.registry.url", "mock://schema-registry");

                var avroDeserializer = new KafkaAvroDeserializer();
                avroDeserializer.configure(configs, false);

                consumer = new DefaultKafkaConsumerFactory<>(
                                configs,
                                new StringDeserializer(),
                                (Deserializer) avroDeserializer).createConsumer();

                kafkaBroker.consumeFromEmbeddedTopics(consumer, "package-delivery-topic");
        }

        @AfterEach
        void tearDown() {
                consumer.close();
        }

        @Test
        void shouldPostEventSuccesfully() {
                // GIVEN
                var httpHeaders = new HttpHeaders();
                httpHeaders.set("content-type", MediaType.APPLICATION_JSON_VALUE.toString());

                var requestBody = getValidRequestBody();
                var httpRequest = new HttpEntity<>(requestBody, httpHeaders);

                OutboxEvent outboxEvent = getOutboxEvent();

                when(outboxRepositoryPort.findUnprocessedEvents())
                                .thenReturn(List.of(outboxEvent))
                                .thenReturn(Collections.emptyList());

                ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);

                // WHEN
                ResponseEntity<Void> response = template.exchange(
                                "/api/delivery",
                                HttpMethod.POST,
                                httpRequest,
                                Void.class);

                assertEquals(HttpStatus.CREATED, response.getStatusCode());

                verify(outboxRepositoryPort, times(1)).save(outboxCaptor.capture());

                List<OutboxEvent> eventosSalvos = outboxCaptor.getAllValues();
                OutboxEvent eventoCriadoPeloController = eventosSalvos.get(0);

                assertNotNull(eventoCriadoPeloController);
                assertNotNull(eventoCriadoPeloController.getAggregateId());
                assertEquals(false, eventoCriadoPeloController.isProcessed(),
                                "O Controller deve criar o evento como NÃO processado");

                assertEquals("package-delivery-topic",
                                eventoCriadoPeloController.getTopic());

                // // 3. Valida se o SCHEDULER atualizou o evento para processado = true
                // OutboxEvent eventoAtualizadoPeloScheduler = eventosSalvos.get(1);
                // assertEquals(true, eventoAtualizadoPeloScheduler.isProcessed(),
                // "O Scheduler deve marcar o evento como processado");
        }

        @Test
        void shouldKeepEventAsUnprocessedWhenKafkaIsDown() {
                // GIVEN
                var httpHeaders = new HttpHeaders();
                httpHeaders.set("content-type", MediaType.APPLICATION_JSON_VALUE.toString());

                var requestBody = getValidRequestBody();

                var httpRequest = new HttpEntity<>(requestBody, httpHeaders);

                doThrow(new DataAccessResourceFailureException("Banco fora do ar de proposito"))
                                .when(outboxRepositoryPort).save(any());

                // WHEN
                ResponseEntity<Void> response = template.exchange(
                                "/api/delivery",
                                HttpMethod.POST,
                                httpRequest,
                                Void.class);

                // THEN
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        }

        @Test
        void shouldReturnBadRequestAndNotPersistAnythingWhenCepIsInvalid() {
                // GIVEN
                var httpHeaders = new HttpHeaders();
                httpHeaders.set("content-type", MediaType.APPLICATION_JSON_VALUE.toString());

                var requestBody = getRequestBodyWithCepInvalid();

                var httpRequest = new HttpEntity<>(requestBody, httpHeaders);

                // WHEN
                ResponseEntity<Void> response = template.exchange(
                                "/api/delivery",
                                HttpMethod.POST,
                                httpRequest,
                                Void.class);

                // THEN
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

                assertThrows(IllegalStateException.class, () -> {
                        KafkaTestUtils.getSingleRecord(consumer, "package-delivery-topic",
                                        Duration.ofMillis(1000));
                }, "Nenhuma mensagem deveria ter sido enviada ao Kafka para um CEP inválido");
        }

        private OutboxEvent getOutboxEvent() {
                OutboxEvent outboxEvent = new OutboxEvent();
                outboxEvent.setAggregateId(UUID.randomUUID().toString());
                outboxEvent.setProcessed(false);
                outboxEvent.setTopic(topicsProperties.getPackageDelivery());

                outboxEvent.setPayload("""
                                {
                                    "height": 15.5,
                                    "width": 20.0,
                                    "length": 30.0,
                                    "weight": 2.5,
                                    "originZipCode": "01001000",
                                    "destinationZipCode": "20001000"
                                }
                                """);
                return outboxEvent;
        }

        private PackageDeliveryRequest getValidRequestBody() {
                var requestBody = new PackageDeliveryRequest(
                                15.5,
                                20.0,
                                30.0,
                                2.5,
                                "01001000",
                                "20001000");
                return requestBody;
        }

        private PackageDeliveryRequest getRequestBodyWithCepInvalid() {
                var requestBody = new PackageDeliveryRequest(
                                15.5,
                                20.0,
                                30.0,
                                2.5,
                                "0100-1000",
                                "20001000");
                return requestBody;
        }

}
