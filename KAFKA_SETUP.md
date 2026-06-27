# Kafka Local Setup

This project supports Kafka producer integration for the outbox publisher.

## 1. Prerequisite

Install Docker Desktop and verify Docker is available:

```bash
docker --version
docker compose version
```

## 2. Start Kafka

From the project root:

```bash
docker compose up -d
```

Verify container:

```bash
docker ps
```

Expected container:

```text
mini-integration-kafka
```

## 3. Create Kafka Topic

Create the topic:

```bash
docker exec -it mini-integration-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic webhook.events \
  --partitions 3 \
  --replication-factor 1
```

If the topic already exists, verify it:

```bash
docker exec -it mini-integration-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic webhook.events
```

Expected:

```text
PartitionCount: 3
ReplicationFactor: 1
```

## 4. Run Kafka Consumer

Use this command to read messages from the topic:

```bash
docker exec -it mini-integration-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic webhook.events \
  --from-beginning \
  --property print.key=true \
  --property print.partition=true \
  --property print.offset=true \
  --property key.separator=" | "
```

## 5. Run Application with Kafka

Start the application normally:

```bash
mvn spring-boot:run
```

Default Kafka configuration:

```properties
spring.kafka.bootstrap-servers=localhost:9092
app.kafka.topics.webhook-events=webhook.events
```

## 6. Run Application without Kafka

Use mock publisher mode:

```bash
SPRING_PROFILES_ACTIVE=mock-publisher mvn spring-boot:run
```

In mock mode:

* Kafka is not required
* Events are mock-published through logs
* Outbox retry flow can still be tested locally

## 7. Useful Kafka Commands

List topics:

```bash
docker exec -it mini-integration-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list
```

Describe topic:

```bash
docker exec -it mini-integration-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic webhook.events
```

Get latest offsets:

```bash
docker exec -it mini-integration-kafka /opt/kafka/bin/kafka-run-class.sh \
  kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic webhook.events \
  --time -1
```

Stop Kafka:

```bash
docker compose down
```
