# SFR - Kafka Kubernetes Event-Oriented

Este projeto é uma arquitetura orientada a eventos (**Event-Driven Architecture**) de alto desempenho e resiliência, projetada para gerenciar e processar fluxos de entrega de encomendas.

O ecossistema é composto por um orquestrador central (**sfr-orchestrator-api**) e microsserviços especialistas (**workers**) que reagem assincronamente a eventos de domínio.

A comunicação entre os serviços é realizada de forma confiável utilizando **Apache Kafka** com serialização de dados estruturada em **Apache Avro** e governança de contratos via **Confluent Schema Registry**.

---

# Arquitetura e Padrões de Projeto

A aplicação foi desenhada sob padrões de engenharia de software corporativo:

## Ports and Adapters (Arquitetura Hexagonal)

Desacoplamento estrito entre a regra de negócio central (`domain` e `application`) e os detalhes de infraestrutura externa (`adapters` de persistência, web e mensageria).

## Transactional Outbox Pattern

Implementado na API do orquestrador para garantir consistência atômica entre a gravação no banco de dados relacional e a publicação de eventos no Kafka.

Isso blinda o sistema contra perda de dados decorrentes de falhas temporárias de rede ou indisponibilidade do broker.

## Idempotência de Mensageria

Configurações de produtor resiliente no Kafka garantindo entrega única (**Exactly-Once Processing**) sem duplicações acidentais.

---

# Tecnologias Utilizadas

* Java 21 (LTS) utilizando recursos modernos como **Records** e **Sealed Interfaces**
* Spring Boot 3.5.x

    * Spring Web
    * Spring Data JPA
    * Spring Kafka
    * Spring Validation
* Apache Kafka 3.7.0 (modo KRaft, sem dependência de ZooKeeper)
* Confluent Schema Registry 7.6.0
* Apache Avro 1.11.3
* PostgreSQL 15
* Docker & Docker Compose
* Project Lombok

---

# Pré-requisitos

Para rodar este projeto localmente, certifique-se de possuir:

* Java Development Kit (JDK) 21 ou superior
* Docker e Docker Compose instalados e com o daemon ativo
* Maven 3.9+ (opcional, pois o projeto inclui o Maven Wrapper `./mvnw`)
* Cliente HTTP para testes de API:

    * Postman
    * Insomnia
    * REST Client (VS Code)

---

# Geração de Código Apache Avro

Os esquemas de dados orientados a eventos estão declarados no formato Avro dentro do diretório src/main/resources/avro/ (ex: RequestStartedEvent.avsc). Como os stubs e classes Java gerados a partir desses contratos não são enviados ao repositório git (ficam isolados na pasta target/), é obrigatório gerar os fontes antes de rodar as aplicações pela primeira vez, caso contrário sua IDE apontará erros de compilação.

Para compilar os esquemas assíncronos e gerar as classes de dados automaticamente, navegue até a pasta do módulo correspondente e utilize o comando do Maven:

```cd sfr-orchestrator-api
mvn clean compile 
```



(Caso esteja utilizando o Maven Wrapper do projeto, execute ./mvnw clean compile).

Este comando invocará o avro-maven-plugin, gerando as classes Java necessárias em target/generated-sources/avro/. Se a sua IDE não reconhecer os arquivos gerados imediatamente, clique com o botão direito na pasta raiz do projeto e selecione a opção Maven -> Reload Project.

#  Como Rodar o Projeto

Existem duas formas distintas de subir e testar o projeto.

## Cenário 1: Rodar a Infraestrutura e Depurar a Aplicação Localmente

Recomendado para desenvolvimento, debugging e testes rápidos.

### 1. Iniciar os Serviços de Infraestrutura

Na raiz do projeto:

```bash
docker compose up -d kafka schema-registry db-orchestrator db-worker-definer db-worker-region
```

Verifique os contêineres:

```bash
docker compose ps
```

### 2. Configurar o Profile Local

Cada aplicação possui um profile customizado chamado:

```text
local-dev
```

Configurado em:

```text
application-local-dev.yml
```

Este profile redireciona as URLs internas do Docker para as portas expostas localmente.

Exemplos:

* PostgreSQL Orchestrator → `localhost:5433`
* Kafka → `localhost:9094`

### 3. Iniciar o Orquestrador Fora do Docker

#### Via Terminal

```bash
cd sfr-orchestrator-api
./mvnw spring-boot:run -Dspring-boot.run.profiles=local-dev
```

#### Via IDE (IntelliJ IDEA / VS Code)

1. Importe o projeto Maven.
2. Abra a configuração de execução da classe:

```text
SfrOrchestratorApiApplication
```

3. Configure o profile:

```text
local-dev
```

4. Execute em modo Debug.

---

## Cenário 2: Rodar Toda a Arquitetura Dentro de Contêineres

Ideal para homologação, demonstrações e validações integradas.

### 1. Build e Inicialização Completa

Na raiz do projeto:

```bash
docker compose up -d --build
```

### 2. Encerrar o Ambiente

```bash
docker compose down
```

---

# Testando o Fluxo de Entrada

Com o orquestrador ativo, envie uma requisição HTTP para registrar uma intenção de entrega.

## Endpoint

```http
POST http://localhost:8080/api/delivery
```

## Headers

```http
Content-Type: application/json
```

## Corpo da Requisição

```json
{
  "height": 15.5,
  "width": 20.0,
  "length": 30.0,
  "weight": 2.5,
  "originZipCode": "01001000",
  "destinationZipCode": "20001000"
}
```

---

#  O que acontece em background?

1. O controller recebe a requisição.
2. Os CEPs e dimensões são validados.
3. A requisição é delegada para o `PackageDeliveryService`.
4. O registro é persistido com:

    * Status `STARTED`
    * `correlationId` global
5. Dentro da mesma transação do banco:

    * Um registro é inserido na tabela `outbox_events`.
6. O `OutboxRelayScheduler` processa eventos pendentes.
7. O evento `RequestStartedEvent` é serializado em Avro.
8. O evento é publicado no tópico Kafka:

```text
package-delivery-topic
```

9. Após confirmação de entrega ao broker:

    * O registro da Outbox é marcado como processado.

---

# Fluxo Resumido

```text
HTTP Request
     │
     ▼
Orchestrator API
     │
     ▼
PostgreSQL
     │
     ├── Delivery Record
     └── Outbox Event
              │
              ▼
     Outbox Relay Scheduler
              │
              ▼
       Apache Kafka
              │
              ▼
         Workers
              │
              ▼
 Processamento Assíncrono
```

SFR Worker Region (Worker 1)

O Worker Region é o primeiro microsserviço especialista da malha assíncrona. Ele é responsável por enriquecer os dados da entrega, calcular métricas logísticas e integrar-se com serviços externos de geolocalização.

Responsabilidades de Negócio

Cálculo de Peso Cubado: Aplica a fórmula oficial de cubagem rodoviária/aérea padrão (Fator 300) com base nas dimensões (altura, largura, comprimento) contidas no evento de entrada.

Mapeamento de Região Geográfica: Determina se a entrega ocorrerá em uma área metropolitana (SAME_REGION) ou demandará transferência interestadual (DISTANT_REGION).

Padrões Arquiteturais Específicos

Enquanto o Orquestrador utiliza o padrão Transactional Outbox por ser o ponto de entrada síncrono (HTTP) do sistema, este Worker é puramente orientado a eventos e utiliza uma abordagem arquitetural diferente:

Exactly-Once Semantics (EOS) / Transações Kafka-Centric

Este serviço resolve o problema da Escrita Dupla (Dual-Write Problem) de forma nativa e sem tabelas auxiliares. O processamento ocorre dentro de uma transação atômica gerenciada pelo KafkaTransactionManager em conjunto com o Spring Data JPA:

A mensagem só é consumida pelo listener.

O processamento e integração externa ocorrem.

O estado é salvo no banco de dados local (db-worker-region).

A mensagem de resposta é publicada no tópico de destino.

Commit Atômico: O commit do PostgreSQL e o avanço do offset do Kafka acontecem simultaneamente. Se qualquer falha ocorrer (ex: queda de rede, API externa fora do ar, falha no banco), a operação sofre Rollback Automático, o envio da resposta é abortado e a mensagem original é reprocessada, garantindo zero perda ou duplicação de dados.

O consumidor do Worker está configurado com isolation.level=read_committed, e o produtor possui um transaction-id-prefix para garantir resiliência contra crash/reinicialização do Pod.

Integração com OpenFeign (Outbound Adapter)

A consulta das UF (Estados) dos CEPs é feita consultando a API pública do ViaCEP.
Respeitando a Clean Architecture (Ports and Adapters), a regra de negócio central não conhece o OpenFeign. A comunicação HTTP está isolada na classe AddressIntegrationAdapter, que implementa a porta AddressIntegrationPort.

Fluxo de Processamento do Worker

Apache Kafka (Tópico de Entrada)
│
▼
[ Consume ] RequestStartedEvent
│
├── 1. Abre Transação Spring / Kafka
│
├── 2. [ HTTP/GET ] OpenFeign -> ViaCEP API (Consulta UF Origem e Destino)
│
├── 3. Executa Regras de Negócio (Cubagem e Roteamento)
│
├── 4. [ PostgreSQL ] Salva entidade `RegionProcessing`
│
├── 5. [ Produce ] Publica RegionDefinedEvent (UNCOMMITTED)
│
└── 6. Commit Global Atômico (DB Commit + Kafka Offset Sync)
│
▼
Apache Kafka (Tópico de Saída)


Consumo e Produção (Tópicos)

Consome de: package-delivery-topic

Contrato de Entrada: RequestStartedEvent.avsc

Produz para: package-delivery-response-topic

Contrato de Saída: RegionDefinedEvent.avsc (Enriquecido com region e cubedWeight)

Como rodar o Worker isoladamente

Garanta que a infraestrutura (Kafka, Schema Registry, DBs) esteja rodando via Docker.

Certifique-se de compilar os schemas Avro primeiro:

cd sfr-worker-region
./mvnw clean compile -U


Suba a aplicação com o profile de desenvolvimento:

./mvnw spring-boot:run -Dspring-boot.run.profiles=local-dev


Envie o curl no Orquestrador. Você verá os logs do Worker Region capturando o evento, consultando o ViaCEP, calculando o peso cubado e enviando o novo evento para o Kafka na mesma fração de segundo!# SFR Worker Region (Worker 1)

O **Worker Region** é o primeiro microsserviço especialista da malha assíncrona. Ele é responsável por enriquecer os dados da entrega, calcular métricas logísticas e integrar-se com serviços externos de geolocalização.

---

## Responsabilidades de Negócio

### Cálculo de Peso Cubado

Aplica a fórmula oficial de cubagem rodoviária/aérea padrão (**Fator 300**) com base nas dimensões (**altura, largura e comprimento**) contidas no evento de entrada.

### Mapeamento de Região Geográfica

Determina se a entrega ocorrerá em uma área metropolitana (**SAME_REGION**) ou demandará transferência interestadual (**DISTANT_REGION**).

---

## Padrões Arquiteturais Específicos

Enquanto o **Orquestrador** utiliza o padrão **Transactional Outbox** por ser o ponto de entrada síncrono (HTTP) do sistema, este Worker é puramente orientado a eventos e utiliza uma abordagem arquitetural diferente.

### Exactly-Once Semantics (EOS) / Transações Kafka-Centric

Este serviço resolve o problema da **Escrita Dupla (Dual-Write Problem)** de forma nativa e sem tabelas auxiliares.

O processamento ocorre dentro de uma transação atômica gerenciada pelo **KafkaTransactionManager** em conjunto com o **Spring Data JPA**:

1. A mensagem é consumida pelo listener.
2. O processamento e integrações externas são executados.
3. O estado é persistido no banco de dados local (`db-worker-region`).
4. A mensagem de resposta é publicada no tópico de destino.

#### Commit Atômico

O commit do PostgreSQL e o avanço do offset do Kafka acontecem simultaneamente.

Se qualquer falha ocorrer (queda de rede, API externa indisponível, falha no banco de dados etc.), toda a operação sofre **rollback automático**, o envio da resposta é cancelado e a mensagem original é reprocessada, garantindo:

* Zero perda de dados;
* Zero duplicação de eventos;
* Consistência transacional entre banco e mensageria.

O consumidor está configurado com:

```properties
isolation.level=read_committed
```

O produtor utiliza:

```properties
transaction-id-prefix
```

garantindo resiliência em cenários de falha, reinicialização da aplicação ou recriação de Pods.

---

## Integração com OpenFeign (Outbound Adapter)

A consulta das UFs (Estados) dos CEPs é realizada através da API pública do **ViaCEP**.

Seguindo os princípios da **Clean Architecture (Ports and Adapters)**, a regra de negócio central não possui dependência direta do OpenFeign.

Toda a comunicação HTTP está isolada no adapter:

```java
AddressIntegrationAdapter
```

que implementa a porta:

```java
AddressIntegrationPort
```

---

## Fluxo de Processamento do Worker

```text
Apache Kafka (Tópico de Entrada)
     │
     ▼
[ Consume ] RequestStartedEvent
     │
     ├── 1. Abre Transação Spring / Kafka
     │
     ├── 2. [ HTTP/GET ] OpenFeign → ViaCEP API
     │        (Consulta UF Origem e Destino)
     │
     ├── 3. Executa Regras de Negócio
     │        • Cubagem
     │        • Roteamento Regional
     │
     ├── 4. [ PostgreSQL ]
     │        Salva entidade RegionProcessing
     │
     ├── 5. [ Produce ]
     │        Publica RegionDefinedEvent
     │        (UNCOMMITTED)
     │
     └── 6. Commit Global Atômico
               (DB Commit + Kafka Offset Sync)
               │
               ▼
Apache Kafka (Tópico de Saída)
```

---

## Consumo e Produção (Tópicos)

### Consome de

```text
package-delivery-topic
```

**Contrato de Entrada**

```text
RequestStartedEvent.avsc
```

### Produz para

```text
package-delivery-response-topic
```

**Contrato de Saída**

```text
RegionDefinedEvent.avsc
```

Evento enriquecido com:

* `region`
* `cubedWeight`

---

## Como Rodar o Worker Isoladamente

### 1. Suba a Infraestrutura

Garanta que a infraestrutura esteja em execução:

* Apache Kafka
* Schema Registry
* PostgreSQL

Todos os componentes podem ser iniciados via Docker.

---

### 2. Compile os Schemas Avro

```bash
cd sfr-worker-region
./mvnw clean compile -U
```

---

### 3. Execute a Aplicação

Utilizando o profile de desenvolvimento:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local-dev
```

---

### 4. Teste o Fluxo

Envie uma requisição para o **SFR Orchestrator API**.

Nos logs do Worker Region será possível acompanhar:

* Consumo do evento de entrada;
* Consulta dos CEPs via ViaCEP;
* Cálculo do peso cubado;
* Definição da região logística;
* Persistência dos dados;
* Publicação do novo evento no Kafka.

Todo o fluxo ocorre dentro de uma única transação atômica, garantindo consistência e confiabilidade no processamento orientado a eventos.

---

## Tecnologias Utilizadas

* Java 21
* Spring Boot
* Spring Kafka
* Apache Kafka
* Apache Avro
* Schema Registry
* PostgreSQL
* Spring Data JPA
* OpenFeign
* Docker
* Clean Architecture
* Event-Driven Architecture (EDA)
* Exactly-Once Semantics (EOS)


curl -i -X POST http://localhost:8080/api/delivery \
-H "Content-Type: application/json" \
-d '{
"height": 18.5,
"width": 18.0,
"length": 18.0,
"weight": 18.5,
"originZipCode": "01001000",
"destinationZipCode": "20001000"
}'