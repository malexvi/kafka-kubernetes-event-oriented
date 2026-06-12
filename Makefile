# Makefile para automação de tarefas do projeto de mensageria SFR

.PHONY: help infra-total infra-dev down avro-gen run-orch status

# Comando padrão caso digite apenas 'make'
help:
	@echo "=========================================================================="
	@echo "                      SFR - COMANDOS DE ATALHOS                           "
	@echo "=========================================================================="
	@echo "  make infra-total   - Builda e sobe TODOS os containers (incluindo as APIs)"
	@echo "  make infra-dev     - Sobe apenas os bancos, Kafka e Schema Registry"
	@echo "  make down          - Derruba todos os containers e remove as redes"
	@echo "  make avro-gen      - Compila e gera as classes Java a partir do Avro"
	@echo "  make run-orch      - Roda o Orquestrador fora do container (local-dev)"
	@echo "  make status        - Exibe o status atual de execução dos containers"
	@echo "  make valid-request  - Envia uma requisição válida para a API"
    @echo "  make invalid-request - Envia uma requisição inválida para testar validações"
	@echo "=========================================================================="

# 1 - Rodar infra total (Cenário 2)
infra-total:
	docker compose up -d --build

# 2 - Rodar infra só banco e kafka (Cenário 1)
infra-dev:
	docker compose up -d kafka schema-registry db-orchestrator db-worker-definer db-worker-region

# Parar a infraestrutura
down:
	docker compose down

# 3 - Rodar compilação do Avro (Gera os stubs Java a partir do .avsc)
avro-gen:
	mvn -f sfr-orchestrator-api/pom.xml clean compile

# Executar a aplicação Orquestradora localmente com o profile local-dev
run-orch:
	mvn -f sfr-orchestrator-api/pom.xml spring-boot:run -Dspring-boot.run.profiles=local-dev

# Status dos containers
status:
	docker compose ps

# Monitorar mensagens trafegadas no topico do kafka
watch-topic:
	docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic package-delivery-topic --from-beginning

# Monitorar mensagens decodificando o formato Avro via Schema Registry
watch-avro:
	docker exec -it schema-registry kafka-avro-console-consumer --bootstrap-server kafka:9092 --topic package-delivery-topic --from-beginning --property schema.registry.url=http://localhost:8081

# Envia uma requisição válida
valid-request:
	curl -i -X POST http://localhost:8080/api/delivery \
	-H "Content-Type: application/json" \
	-d '{
		"height": 99.5,
		"width": 99.0,
		"length": 99.0,
		"weight": 99.5,
		"originZipCode": "99001000",
		"destinationZipCode": "99001000"
	}'

# Envia uma requisição inválida (peso negativo e CEP inválido)
invalid-request:
	curl -i -X POST http://localhost:8080/api/delivery \
	-H "Content-Type: application/json" \
	-d '{
		"height": -10.0,
		"width": 0,
		"length": -5.0,
		"weight": -1.0,
		"originZipCode": "123",
		"destinationZipCode": ""
	}'