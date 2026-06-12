package com.sfr.sfr_orchestrator_api.unit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfr.sfr_orchestrator_api.application.dto.PackageDeliveryRequest;
import com.sfr.sfr_orchestrator_api.application.event.DeliveryRequestedEvent;
import com.sfr.sfr_orchestrator_api.application.mapper.OutboxEventMapper;
import com.sfr.sfr_orchestrator_api.application.mapper.PackageDeliveryMapper;
import com.sfr.sfr_orchestrator_api.application.port.JpaRepositoryPort;
import com.sfr.sfr_orchestrator_api.application.port.OutboxRepositoryPort;
import com.sfr.sfr_orchestrator_api.application.service.PackageDeliveryService;
import com.sfr.sfr_orchestrator_api.config.KafkaTopicsProperties;
import com.sfr.sfr_orchestrator_api.config.exception.exceptions.SerializerException;
import com.sfr.sfr_orchestrator_api.domain.entity.OutboxEvent;
import com.sfr.sfr_orchestrator_api.domain.entity.PackageDelivery;
import com.sfr.sfr_orchestrator_api.utils.PackageDeliveryTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PackageDeliveryUnitTest {

    @Mock
    private KafkaTopicsProperties topicProperties;

    @Mock
    private JpaRepositoryPort jpaRepositoryPort;

    @Mock
    private OutboxRepositoryPort outboxRepositoryPort;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private OutboxEventMapper outboxEventMapper;

    private PackageDeliveryService packageDeliveryService;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxCaptor;

    @BeforeEach
    void setUp() {
        // Neste ponto (dentro do BeforeEach), o Mockito JÁ interceptou o objectMapper e ele é um Spy legítimo.
        // Agora instanciamos o OutboxEventMapper envelopando ele também com o Mockito.spy()
        outboxEventMapper = Mockito.spy(new OutboxEventMapper(objectMapper));

        // Instanciamos a Service passando toda a cadeia perfeitamente mockada e alinhada
        packageDeliveryService = new PackageDeliveryService(
                topicProperties,
                jpaRepositoryPort,
                outboxRepositoryPort,
                outboxEventMapper
        );
    }

    //@Test
    void shouldCreateNewPackageDelivery() throws JsonProcessingException {

        // GIVE
        UUID correlationId = UUID.fromString("e4536e98-3280-4711-93b0-e00a25b569f8");

        var delivery = new PackageDeliveryRequest(
                correlationId,
                15.5,
                20.0,
                30.0,
                2.5,
                "01001000",
                "20001000");

        UUID orderId = UUID.fromString("4405857f-af87-470c-88f9-18056b3fd745");

        var savedPackageDelivery = PackageDeliveryMapper.toDelivery(delivery);
        savedPackageDelivery.setOrderId(orderId);

        var topicExpect = "package-delivery-topic";

        String jsonPayload = """
                {
                    "e4536e98-3280-4711-93b0-e00a25b569f8",
                    "height": 15.5,
                    "width": 20.0,
                    "length": 30.0,
                    "weight": 2.5,
                    "originZipCode": "01001000",
                    "destinationZipCode": "20001000"
                }
                """;

        // WHEN
        when(jpaRepositoryPort.save(any(PackageDelivery.class))).thenReturn(savedPackageDelivery);
        when(topicProperties.getPackageDeliveryTopic()).thenReturn(topicExpect);
        when(objectMapper.writeValueAsString(any(DeliveryRequestedEvent.class))).thenReturn(jsonPayload);

        // THEN
        packageDeliveryService.create(delivery);

        verify(jpaRepositoryPort, times(1)).save(any(PackageDelivery.class));
        verify(topicProperties, times(1)).getPackageDeliveryTopic();
        verify(objectMapper, times(1)).writeValueAsString(any(DeliveryRequestedEvent.class));
        verify(outboxRepositoryPort, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void shouldCreateNewPackageDeliveryAndSaveToOutboxCorrectly() throws JsonProcessingException {
        // GIVEN - Preparação estrita de dados
        UUID expectedCorrelationId = UUID.fromString("e4536e98-3280-4711-93b0-e00a25b569f8");
        UUID expectedOrderId = UUID.fromString("4405857f-af87-470c-88f9-18056b3fd745");

        PackageDeliveryRequest request = PackageDeliveryTestUtils.createValidRequest(expectedCorrelationId);
        PackageDelivery inputDelivery = PackageDeliveryMapper.toDelivery(request);
        PackageDelivery persistedDelivery = PackageDeliveryTestUtils.createSavedEntity(request, expectedOrderId, expectedCorrelationId);

        String expectedTopic = "package-delivery-topic";
        DeliveryRequestedEvent expectedEvent = DeliveryRequestedEvent.from(persistedDelivery);
        String expectedJsonPayload = objectMapper.writeValueAsString(expectedEvent);

        // Configurando stubs de comportamento
        when(jpaRepositoryPort.save(inputDelivery)).thenReturn(persistedDelivery);
        when(topicProperties.getPackageDeliveryTopic()).thenReturn(expectedTopic);

        // WHEN - Executa o comportamento real passando pelo Mapper de verdade
        packageDeliveryService.create(request);

        // THEN - 1. Validações de chamada de contrato
        verify(jpaRepositoryPort, times(1)).save(inputDelivery);
        verify(topicProperties, times(1)).getPackageDeliveryTopic();

        // THEN - 2. Captura o OutboxEvent REAL gerado pelo componente para explodir em asserções
        verify(outboxRepositoryPort, times(1)).save(outboxCaptor.capture());
        OutboxEvent publishedOutboxEvent = outboxCaptor.getValue();

        // ASSERTS DE TUDO: Provando a integridade das propriedades e IDs dinâmicos gerados
        assertNotNull(publishedOutboxEvent, "O evento de outbox não pode ser nulo");
        assertNotNull(publishedOutboxEvent.getId(), "O ID único da linha de outbox deve ter sido gerado (UUID.randomUUID)");
        assertNotNull(publishedOutboxEvent.getCreatedAt(), "O timestamp do createdAt deve ter sido gerado na hora do mapeamento");

        assertEquals(expectedOrderId.toString(), publishedOutboxEvent.getAggregateId(), "O aggregateId deve ser obrigatoriamente o orderId que veio do banco");
        assertEquals(expectedTopic, publishedOutboxEvent.getTopic(), "O tópico gravado deve ser exatamente o que veio das propriedades");
        assertEquals(expectedJsonPayload, publishedOutboxEvent.getPayload(), "O payload JSON deve bater perfeitamente com o mapeamento real do Jackson");
        assertFalse(publishedOutboxEvent.isProcessed(), "O evento obrigatoriamente nasce com status processed = false");

        // Valida que o JSON gerado realmente contém o CorrelationID rastreável lá dentro
        assertTrue(publishedOutboxEvent.getPayload().contains(expectedCorrelationId.toString()), "O payload enviado precisa conter o correlationId original");
    }

    @Test
    void shouldThrowSerializerExceptionWhenMappingFails() throws JsonProcessingException {
        // GIVEN
        UUID expectedCorrelationId = UUID.fromString("e4536e98-3280-4711-93b0-e00a25b569f8");
        UUID expectedOrderId = UUID.fromString("4405857f-af87-470c-88f9-18056b3fd745");

        PackageDeliveryRequest request = PackageDeliveryTestUtils.createValidRequest(expectedCorrelationId);
        PackageDelivery inputDelivery = PackageDeliveryMapper.toDelivery(request);
        PackageDelivery persistedDelivery = PackageDeliveryTestUtils.createSavedEntity(request, expectedOrderId, expectedCorrelationId);

        String expectedTopic = "package-delivery-topic";

        JsonProcessingException mappingException = mock(JsonProcessingException.class);

        doThrow(mappingException).when(objectMapper).writeValueAsString(any(DeliveryRequestedEvent.class));

        when(jpaRepositoryPort.save(inputDelivery)).thenReturn(persistedDelivery);
        when(topicProperties.getPackageDeliveryTopic()).thenReturn(expectedTopic);

        // WHEN & THEN
        SerializerException thrownException = assertThrows(SerializerException.class, () -> {
            packageDeliveryService.create(request);
        });

        assertNotNull(thrownException.getTitle(), "O título da exceção customizada deve estar preenchido");
        verifyNoInteractions(outboxRepositoryPort);
    }
}

//    @Test //  Vou comentar este teste para usos posteriores, quando formos usar métodos estáticos, precisamos de um mockStatic
//    void shouldThrowSerializerExceptionWhenMappingFails() throws JsonProcessingException {
//        // GIVEN - Preparação do cenário com IDs estáveis
//        UUID expectedCorrelationId = UUID.fromString("e4536e98-3280-4711-93b0-e00a25b569f8");
//        UUID expectedOrderId = UUID.fromString("4405857f-af87-470c-88f9-18056b3fd745");
//
//        PackageDeliveryRequest request = PackageDeliveryTestUtils.createValidRequest(expectedCorrelationId);
//        PackageDelivery inputDelivery = PackageDeliveryMapper.toDelivery(request);
//        PackageDelivery savedPackageDelivery = PackageDeliveryTestUtils.createSavedEntity(request, expectedOrderId, expectedCorrelationId);
//
//        DeliveryRequestedEvent expectedEvent = DeliveryRequestedEvent.from(savedPackageDelivery);
//
//        // Instancia o mock da exceção nativa do Jackson que causará o colapso
//        JsonProcessingException mappingException = mock(JsonProcessingException.class);
//
//        // Configuração estrita dos mocks de comportamento do fluxo
//        when(jpaRepositoryPort.save(inputDelivery)).thenReturn(savedPackageDelivery);
//        when(objectMapper.writeValueAsString(expectedEvent)).thenThrow(mappingException);
//
//        // Interceptação do comportamento estático da ExceptionFactory
//        try (MockedStatic<ExceptionFactory> exceptionFactoryMock = mockStatic(ExceptionFactory.class)) {
//
//            SerializerException expectedDomainException = new SerializerException("Falha de Serialização", "Erro formatado");
//
//            exceptionFactoryMock.when(() -> ExceptionFactory.serializerException(mappingException))
//                    .thenReturn(expectedDomainException);
//
//            // WHEN & THEN - Executa a Service e atesta que o erro de domínio foi propagado
//            SerializerException thrownException = assertThrows(SerializerException.class, () -> {
//                packageDeliveryService.create(request);
//            });
//
//            // Asserções de integridade do cenário de falha
//            assertEquals(expectedDomainException, thrownException);
//
//            exceptionFactoryMock.verify(() -> ExceptionFactory.serializerException(mappingException), times(1));
//
//            // Prova real de resiliência: o repositório da Outbox nunca foi acionado
//            verifyNoInteractions(outboxRepositoryPort);
//        }
//    }

