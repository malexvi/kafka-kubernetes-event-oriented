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