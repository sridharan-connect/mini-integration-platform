# Mini Integration Platform

A backend portfolio project demonstrating reliable webhook ingestion, durable event persistence, outbox-style asynchronous processing, retries, idempotency, DLQ handling, stale-event recovery, and external API processing using Java, Spring Boot, and PostgreSQL.

This project is inspired by integration and workflow automation platforms such as Zapier. The goal is to demonstrate backend patterns commonly used in integration-heavy systems: webhooks, async processing, retries, idempotency, failure recovery, and event-driven workflows.

## Current Status

Work in Progress.

Implemented so far:

* Webhook receiver API
* Request validation
* Durable event persistence in PostgreSQL
* Outbox-style scheduled publisher
* Worker-based event processing
* Receiver-level idempotency using `source + eventId`
* Business-level idempotency for processing side effects
* Retry handling for publish and processing failures
* `PUBLISH_FAILED` handling after publish retry limit
* `DLQ` handling after processing retry limit
* Manual retry API for failed events
* Stale `PROCESSING` recovery
* Mock publisher layer
* Mock external API processing layer

## Tech Stack

* Java
* Spring Boot
* PostgreSQL
* Spring Data JPA
* Scheduled background workers
* Maven

## High-Level Flow

```text
External System
      |
      | POST webhook event
      v
Webhook Receiver API
      |
      | validate request
      | receiver-level idempotency check
      v
PostgreSQL
      |
      | store event as PENDING_PUBLISH
      v
Scheduled Outbox Publisher
      |
      | publish event
      v
PUBLISHED
      |
      | worker picks event
      v
PROCESSING
      |
      | business idempotency check
      | call mock external API
      v
PROCESSED
```

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

Failure paths:

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

## Idempotency

This project implements two levels of idempotency.

### 1. Receiver-Level Idempotency

Receiver-level idempotency prevents duplicate webhook ingestion.

Key:

```text
source + eventId
```

If the same source and eventId are received again:

* No new webhook event row is created
* Existing event status is returned
* Duplicate response is returned successfully
* Event is not published or processed again through duplicate ingestion

Example duplicate response:

```json
{
  "eventId": "evt_1001",
  "status": "PROCESSED",
  "message": "Duplicate event already received",
  "duplicate": true
}
```

### 2. Business-Level Idempotency

Business-level idempotency prevents duplicate business side effects during worker processing.

This protects cases where:

```text
1. Worker marks event as PROCESSING
2. External API call succeeds
3. Application crashes before marking event as PROCESSED
4. Stale recovery moves event back to PUBLISHED
5. Worker picks the event again
```

Without business idempotency, the external API could be called twice for the same business action.

Current business idempotency key strategy:

```text
ORDER_CREATED:
source + eventType + orderId

ORDER_UPDATED:
source + eventType + orderId + eventId
```

Examples:

```text
shopify:ORDER_CREATED:1001
shopify:ORDER_UPDATED:1001:EVT-2001
```

This allows duplicate create actions for the same order to be skipped, while still allowing multiple valid update events for the same order.

Key design point:

```text
Webhook event status controls workflow.
Business idempotency controls duplicate side effects.
```

## Manual Retry API

Manual retry is supported for failed webhook events.

Endpoint:

```http
POST /api/v1/webhook-events/{id}/retry
```

Supported retry flows:

```text
PUBLISH_FAILED -> PENDING_PUBLISH
DLQ            -> PUBLISHED
```

Invalid retry attempts for active or completed states such as `PENDING_PUBLISH`, `PUBLISHED`, `PROCESSING`, and `PROCESSED` return a conflict response.

## Stale PROCESSING Recovery

A scheduled recovery job handles webhook events stuck in `PROCESSING`.

This can happen if the worker crashes after marking an event as `PROCESSING`, but before marking it as `PROCESSED`, `PUBLISHED`, or `DLQ`.

Recovery condition:

```text
status = PROCESSING
processingStartedAt < configured threshold
```

Recovery behavior:

* If retry limit is not reached, event moves back to `PUBLISHED`
* If retry limit is reached, event moves to `DLQ`

Business-level idempotency helps prevent duplicate side effects when recovered events are processed again.

## Sample Webhook Request

```http
POST /api/v1/webhooks/order
Content-Type: application/json
```

```json
{
  "eventId": "EVT-1001",
  "source": "shopify",
  "eventType": "ORDER_CREATED",
  "payload": {
    "orderId": "1001",
    "customerId": "CUST-1",
    "amount": 2500
  }
}
```

## Design Decisions

### Persist before processing

The webhook API first stores the event durably before any asynchronous processing starts.

### Do not publish in the request thread

Publishing directly inside the webhook request can increase latency and tightly couple the API to downstream failures.

### Use status-based workflow

Each event has a clear status, making failures easier to track, debug, and retry.

### Retry with failure tracking

Publish and processing failures store retry count and last error message.

### Stop automatic retry after max attempts

After max publish failures, events move to `PUBLISH_FAILED`.
After max processing failures, events move to `DLQ`.

### Separate workflow status from business idempotency

Webhook event status controls the processing workflow.
Business idempotency protects external side effects from duplicate execution.

## Roadmap

Planned improvements:

* Kafka producer integration
* Retry backoff using `nextRetryAt`
* Dedicated DLQ table
* Real external API integration
* Webhook signature validation improvements
* OAuth integration flow
* Redis-based caching or locking exploration
* Metrics and alerting
* Architecture diagram

## Learning Focus

This project demonstrates backend engineering concepts commonly used in integration-platform and distributed-system roles:

* Webhook ingestion
* Durable event persistence
* Outbox pattern
* Asynchronous processing
* Retry handling
* Failure recovery
* Receiver-level idempotency
* Business-level idempotency
* DLQ handling
* Manual retry design
* Stale-event recovery
* External API processing safety
