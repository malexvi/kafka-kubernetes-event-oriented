package com.sfr.sfr_orchestrator_api.intg.controller;

import static com.sfr.sfr_orchestrator_api.config.constants.PathConstants.DELIVERY_API_PATH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import org.springframework.http.*;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sfr.sfr_orchestrator_api.application.port.OutboxRepositoryPort;
import com.sfr.sfr_orchestrator_api.config.KafkaTopicsProperties;
import com.sfr.sfr_orchestrator_api.domain.entity.OutboxEvent;
import com.sfr.sfr_orchestrator_api.infrastructure.kafka.avro.RequestStartedEvent;
import com.sfr.sfr_orchestrator_api.utils.PackageDeliveryTestUtils;

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
        private TestRestTemplate template;

        @Autowired
        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
        private EmbeddedKafkaBroker kafkaBroker;

        @MockitoBean
        private OutboxRepositoryPort outboxRepositoryPort;

        @Autowired
        private KafkaTopicsProperties topicsProperties;

        private Consumer<String, RequestStartedEvent> consumer;

        @BeforeEach
        void setUp() {
                var configs = new HashMap<>(KafkaTestUtils.consumerProps("group1", "true", kafkaBroker));
                configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
                configs.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");
                configs.put("schema.registry.url", "mock://schema-registry");

                Deserializer<?> avroDeserializer = new KafkaAvroDeserializer();
                avroDeserializer.configure(configs, false);

                @SuppressWarnings("unchecked")
                var consumerFactory = new DefaultKafkaConsumerFactory<>(
                        configs,
                        new StringDeserializer(),
                        (Deserializer<RequestStartedEvent>) avroDeserializer
                );

                consumer = consumerFactory.createConsumer();

                kafkaBroker.consumeFromEmbeddedTopics(consumer, "package-delivery-topic");
        }

        @AfterEach
        void tearDown() {
                consumer.close();
        }

        @Test
        void shouldPostEventSuccessfully() {
                // GIVEN
                var httpHeaders = new HttpHeaders();
                httpHeaders.set("content-type", MediaType.APPLICATION_JSON_VALUE);

                var requestBody = PackageDeliveryTestUtils.getValidRequestBody();
                var httpRequest = new HttpEntity<>(requestBody, httpHeaders);

                OutboxEvent outboxEvent = PackageDeliveryTestUtils.getOutboxEvent(topicsProperties.getPackageDeliveryTopic());

                when(outboxRepositoryPort.findUnprocessedEvents())
                        .thenReturn(List.of(outboxEvent))
                        .thenReturn(Collections.emptyList());

                ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);

                // WHEN
                ResponseEntity<Void> response = template.exchange(DELIVERY_API_PATH, HttpMethod.POST, httpRequest, Void.class);

                // THEN
                assertEquals(HttpStatus.CREATED, response.getStatusCode());
                verify(outboxRepositoryPort, times(1)).save(outboxCaptor.capture());

                OutboxEvent eventoCriadoPeloController = outboxCaptor.getValue();

                String payloadSalvo = eventoCriadoPeloController.getPayload();
                assertNotNull(payloadSalvo, "O payload JSON não deveria ser nulo");

                // Valida se as propriedades reais da requisição estão escritas dentro da String JSON
                assertTrue(payloadSalvo.contains("\"height\":15.5"), "O JSON deveria conter a altura informada");
                assertTrue(payloadSalvo.contains("\"originZipCode\":\"01001000\""), "O JSON deveria conter o CEP de origem");

                assertNotNull(eventoCriadoPeloController);
                assertNotNull(eventoCriadoPeloController.getAggregateId());
                assertFalse(eventoCriadoPeloController.isProcessed());
                assertEquals(topicsProperties.getPackageDeliveryTopic(), eventoCriadoPeloController.getTopic());
        }

        @Test
        void shouldGenerateCorrelationIdWhenHeaderIsMissing() {
                // GIVEN - Requisição sem passar o header X-Correlation-ID
                var httpHeaders = new HttpHeaders();
                httpHeaders.set("content-type", MediaType.APPLICATION_JSON_VALUE);
                var httpRequest = new HttpEntity<>(PackageDeliveryTestUtils.getValidRequestBody(), httpHeaders);

                // WHEN
                ResponseEntity<Void> response = template.exchange(DELIVERY_API_PATH, HttpMethod.POST, httpRequest, Void.class);

                // THEN - O Interceptor deve gerar um UUID e nos devolver no header da resposta
                assertEquals(HttpStatus.CREATED, response.getStatusCode());
                String returnedCorrelationId = response.getHeaders().getFirst("X-Correlation-ID");

                assertNotNull(returnedCorrelationId, "A API deveria ter gerado e retornado um Correlation ID");
                assertDoesNotThrow(() -> UUID.fromString(returnedCorrelationId), "O Correlation ID retornado deve ser um UUID válido");
        }

        @Test
        void shouldPropagateCorrelationIdWhenHeaderIsPresent() {
                // GIVEN - Enviando um Correlation ID explícito gerado pelo cliente/gateway
                String originalCorrelationId = UUID.randomUUID().toString();

                var httpHeaders = new HttpHeaders();
                httpHeaders.set("content-type", MediaType.APPLICATION_JSON_VALUE);
                httpHeaders.set("X-Correlation-ID", originalCorrelationId);

                var httpRequest = new HttpEntity<>(PackageDeliveryTestUtils.getValidRequestBody(), httpHeaders);

                // WHEN
                ResponseEntity<Void> response = template.exchange(DELIVERY_API_PATH, HttpMethod.POST, httpRequest, Void.class);

                // THEN - A API tem a obrigação de manter o ID original intacto na resposta
                assertEquals(HttpStatus.CREATED, response.getStatusCode());
                String returnedCorrelationId = response.getHeaders().getFirst("X-Correlation-ID");

                assertEquals(originalCorrelationId, returnedCorrelationId, "O Correlation ID foi alterado ou perdido pelo interceptor");
        }

        @Test
        void shouldKeepEventAsUnprocessedWhenKafkaIsDown() {
                // GIVEN
                var httpHeaders = new HttpHeaders();
                httpHeaders.set("content-type", MediaType.APPLICATION_JSON_VALUE);
                var httpRequest = new HttpEntity<>(PackageDeliveryTestUtils.getValidRequestBody(), httpHeaders);

                doThrow(new DataAccessResourceFailureException("Banco fora do ar de proposito"))
                        .when(outboxRepositoryPort).save(any());

                // WHEN
                ResponseEntity<Void> response = template.exchange(DELIVERY_API_PATH, HttpMethod.POST, httpRequest, Void.class);

                // THEN
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        @Test
        void shouldReturnBadRequestAndNotPersistAnythingWhenCepIsInvalid() {
                // GIVEN
                var httpHeaders = new HttpHeaders();
                httpHeaders.set("content-type", MediaType.APPLICATION_JSON_VALUE);
                var httpRequest = new HttpEntity<>(PackageDeliveryTestUtils.getRequestBodyWithCepInvalid(), httpHeaders);

                // WHEN
                ResponseEntity<Void> response = template.exchange(DELIVERY_API_PATH, HttpMethod.POST, httpRequest, Void.class);

                // THEN
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

                assertThrows(IllegalArgumentException.class, ()
                        -> KafkaTestUtils
                        .getSingleRecord(
                                consumer,
                                topicsProperties.getPackageDeliveryTopic(),
                                Duration.ofMillis(1000)),
                        "Nenhuma mensagem deveria ter sido enviada ao Kafka para um CEP inválido");
        }
}