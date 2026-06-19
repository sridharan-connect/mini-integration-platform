# Mini Integration Platform

A public backend portfolio project demonstrating reliable webhook ingestion, durable event persistence, outbox-based asynchronous publishing, worker processing, retry handling, idempotency, DLQ handling, and failure tracking using Java, Spring Boot, and PostgreSQL.

This project is inspired by integration and workflow automation platforms such as Zapier. The goal is to build backend patterns commonly used in integration-heavy systems: webhooks, async processing, retries, idempotency, DLQ, external API calls, and event-driven workflows.

## Current Status

Work in Progress.

The project currently implements reliable webhook ingestion, durable event persistence, scheduled outbox publishing, worker-based processing, retry handling, DLQ handling, and receiver-level idempotency.

## Implemented So Far

* Webhook receiver API
* Request payload validation
* Durable event persistence in PostgreSQL
* Event status lifecycle
* Receiver-level idempotency using source + eventId
* Duplicate webhook detection with successful duplicate response
* Scheduled outbox publisher
* Retry count tracking
* Failure reason tracking
* Published timestamp tracking
* PUBLISH_FAILED state after max retry attempts
* Mock publisher layer for Kafka-style asynchronous publishing
* Worker processing flow
* PUBLISHED -> PROCESSING -> PROCESSED lifecycle
* Processing retry count tracking
* Processing error tracking
* DLQ handling after repeated processing failures
* Mock external API processing layer

## High-Level Flow

```text
External System
      |
      | POST webhook event
      v
Webhook Receiver API
      |
      | validate request
      | check duplicate using source + eventId
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
      |
      | worker picks event
      v
Worker Processor
      |
      | mark event as PROCESSING
      v
Mock External API / Business Processing
      |
      | success
      v
PROCESSED
```

```text
Duplicate Event Flow:

External System
      |
      | POST same source + same eventId
      v
Webhook Receiver API
      |
      | duplicate detected
      v
Return successful duplicate response
      |
      | no new DB row created
      v
Existing event status returned
```

```text
Publish Failure Flow:

PENDING_PUBLISH
      |
      | publish failure
      v
retry_count incremented
      |
      | retry available
      v
PENDING_PUBLISH
      |
      | max retry reached
      v
PUBLISH_FAILED
```

```text
Worker Processing Failure Flow:

PUBLISHED
      |
      | worker picks event
      v
PROCESSING
      |
      | processing failure
      v
processing_retry_count incremented
      |
      | retry available
      v
PUBLISHED
      |
      | max retry reached
      v
DLQ
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

## Why Receiver-Level Idempotency?

External systems may retry webhook delivery due to network failures, timeout, or missing acknowledgements. Without idempotency, the same event can be stored and processed multiple times.

This project prevents duplicate event persistence using:

```text
Idempotency key = source + eventId
```

If the same source and eventId are received again, the system returns a successful duplicate response with the existing event status instead of creating another row.

Example duplicate response:

```json
{
  "eventId": "evt_1011",
  "status": "PROCESSED",
  "message": "Duplicate event already received",
  "duplicate": true
}
```

This keeps webhook retries safe and prevents duplicate rows from entering the outbox pipeline.

## Event Status Lifecycle

```text
PENDING_PUBLISH
      |
      | publish success
      v
PUBLISHED
      |
      | worker picks event
      v
PROCESSING
      |
      | processing success
      v
PROCESSED
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

```text
PROCESSING
      |
      | processing failure
      v
PUBLISHED with processing_retry_count incremented
      |
      | max retry reached
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
* Business-level idempotency
* DLQ table
* Manual retry API for failed events
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

Publishing rules:

* Fetch pending events in batches
* Publish one event at a time through publisher layer
* Mark event as PUBLISHED on success
* Increment retry count on failure
* Store last error message
* Mark event as PUBLISH_FAILED after max retry attempts
* Ignore PUBLISH_FAILED events in normal scheduler flow

## Current Worker Processing Behavior

The worker processor runs on a schedule and picks events with:

```text
status = PUBLISHED
```

Processing rules:

* Fetch published events in batches
* Mark event as PROCESSING before processing starts
* Process event through mock external API layer
* Mark event as PROCESSED on success
* Increment processing retry count on failure
* Store processing error message
* Move event to DLQ after repeated processing failures
* Ignore DLQ events in normal worker flow

## Current Idempotency Behavior

The webhook receiver checks whether an event already exists using:

```text
source + eventId
```

If the event already exists:

* No new row is created
* Existing event status is returned
* API returns a successful duplicate response
* The event is not published again
* The event is not processed again through duplicate webhook ingestion

## Design Decisions

### 1. Persist before processing

The webhook API stores the event first before any async processing.

### 2. Do not publish directly in request thread

Publishing directly inside the webhook API can increase latency and introduce failure coupling.

### 3. Use status-based lifecycle

Each event has a clear status so failures can be debugged and retried safely.

### 4. Retry with failure tracking

Failures are not silently ignored. Retry count and last error are stored.

### 5. Stop publishing after max retry

After repeated publishing failures, the event moves to PUBLISH_FAILED and is no longer picked by the normal publisher scheduler.

### 6. Stop processing after max retry

After repeated processing failures, the event moves to DLQ and is no longer picked by the normal worker scheduler.

### 7. Handle duplicate webhooks safely

Duplicate webhook events are identified using source + eventId and handled with a successful duplicate response instead of creating duplicate rows.

## Roadmap

* Business-level idempotency for processing side effects
* Manual retry API for failed or DLQ events
* Stale PROCESSING recovery
* Kafka producer integration
* External real API integration
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
* Receiver-level idempotency
* Business-level idempotency design
* DLQ design
* Scalable backend service design