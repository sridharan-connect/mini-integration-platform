# Mini Integration Platform

A public backend portfolio project demonstrating reliable webhook ingestion, durable event persistence, outbox-based asynchronous publishing, retry handling, and failure tracking using Java, Spring Boot, and PostgreSQL.

This project is inspired by integration and workflow automation platforms such as Zapier. The goal is to build backend patterns commonly used in integration-heavy systems: webhooks, async processing, retries, idempotency, DLQ, external API calls, and event-driven workflows.

## Current Status

Work in Progress.

The project currently implements the first reliable ingestion and outbox-publishing flow.

## Implemented So Far

* Webhook receiver API
* Request payload validation
* Durable event persistence in PostgreSQL
* Event status lifecycle
* Scheduled outbox publisher
* Retry count tracking
* Failure reason tracking
* Published timestamp tracking
* PUBLISH_FAILED state after max retry attempts
* Mock publisher layer for Kafka-style asynchronous publishing

## High-Level Flow

```text
External System
      |
      | POST webhook event
      v
Webhook Receiver API
      |
      | validate request
      v
PostgreSQL
      |
      | store event as PENDING_PUBLISH
      v
Scheduled Outbox Publisher
      |
      | publish event asynchronously
      v
Publisher Layer
      |
      | success
      v
PUBLISHED

Failure Flow:

PENDING_PUBLISH
      |
      | publish failure
      v
retry_count incremented
      |
      | max retry reached
      v
PUBLISH_FAILED
```

## Why Outbox Pattern?

Webhook APIs should respond quickly after safely accepting the event. Directly publishing to a queue or downstream system inside the webhook request can cause slow responses or event loss when the queue, network, or downstream service is unavailable.

This project uses an outbox-style approach:

1. Validate and persist the webhook event first.
2. Return a successful response after durable persistence.
3. Publish the event asynchronously using a scheduled publisher.
4. Track retry count and failure reason.
5. Stop automatic retries after the max retry limit.

This helps avoid event loss and makes failure recovery easier.

## Event Status Lifecycle

```text
PENDING_PUBLISH
      |
      | publish success
      v
PUBLISHED
```

```text
PENDING_PUBLISH
      |
      | publish failure
      v
PENDING_PUBLISH with retry_count incremented
      |
      | max retry reached
      v
PUBLISH_FAILED
```

Planned future lifecycle:

```text
PUBLISHED
      |
      v
PROCESSING
      |
      | success
      v
PROCESSED
```

```text
PROCESSING
      |
      | repeated processing failure
      v
DLQ
```

## Tech Stack

* Java
* Spring Boot
* PostgreSQL
* Spring Data JPA
* Scheduler-based background processing
* Maven

Planned additions:

* Kafka
* Redis
* Idempotency store
* DLQ table
* External API integration
* OAuth flow
* Observability with logs, metrics, and traces

## Sample Webhook Request

```http
POST /api/v1/webhooks/order
Content-Type: application/json
```

```json
{
  "eventId": "evt_1001",
  "eventType": "ORDER_CREATED",
  "source": "SHOPIFY",
  "payload": {
    "orderId": "ord_1001",
    "amount": 1299,
    "currency": "INR"
  }
}
```

## Current Outbox Publishing Behavior

The outbox publisher runs on a schedule and picks events with:

```text
status = PENDING_PUBLISH
```

Processing rules:

* Fetch pending events in batches
* Publish one event at a time through publisher layer
* Mark event as PUBLISHED on success
* Increment retry count on failure
* Store last error message
* Mark event as PUBLISH_FAILED after max retry attempts
* Ignore PUBLISH_FAILED events in normal scheduler flow

## Design Decisions

### 1. Persist before processing

The webhook API stores the event first before any async processing.

### 2. Do not publish directly in request thread

Publishing directly inside the webhook API can increase latency and introduce failure coupling.

### 3. Use status-based lifecycle

Each event has a clear status so failures can be debugged and retried safely.

### 4. Retry with failure tracking

Failures are not silently ignored. Retry count and last error are stored.

### 5. Stop after max retry

After repeated failures, the event moves to PUBLISH_FAILED and is no longer picked by the normal scheduler.

## Roadmap

* Worker processing flow
* Idempotency table
* DLQ table
* Manual retry API for failed events
* Kafka producer integration
* External mock API call
* Webhook signature validation improvements
* OAuth integration flow
* Retry backoff using nextRetryAt
* Metrics and alerting
* Architecture diagram

## Learning Focus

This project is built to demonstrate backend engineering concepts commonly used in integration-platform and distributed-system roles:

* Webhook ingestion
* Event persistence
* Outbox pattern
* Async processing
* Retry handling
* Failure recovery
* Idempotency
* DLQ design
* Scalable backend service design
