# Kafka Event JSON Schemas

Machine-readable contracts for the TokenRealty Kafka envelope and settlement-flow payloads.

Human catalog: [EVENTS.md](../EVENTS.md) · Conventions: [rules/kafka-messaging.md](../rules/kafka-messaging.md)

## Files

| Schema | Topic |
|--------|-------|
| [envelope.schema.json](envelope.schema.json) | Wrapper for all events |
| [order.matched.schema.json](order.matched.schema.json) | `tokenrealty.marketplace.order.matched.v1` |
| [trade.settled.schema.json](trade.settled.schema.json) | `tokenrealty.marketplace.trade.settled.v1` |
| [payment.confirmed.schema.json](payment.confirmed.schema.json) | `tokenrealty.payment.payment.confirmed.v1` |
| [transfer.completed.schema.json](transfer.completed.schema.json) | `tokenrealty.issuance.transfer.completed.v1` |

## Usage

Validate a payload file against a schema (requires [ajv-cli](https://github.com/ajv-validator/ajv-cli)):

```bash
npm install -g ajv-cli
ajv validate -s docs/schemas/order.matched.schema.json -d payload.json --spec=draft2020
```

Envelope validation wraps the payload:

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "tokenrealty.marketplace.order.matched.v1",
  "occurredAt": "2025-09-25T16:00:00Z",
  "traceId": "abc-123",
  "payload": { }
}
```

## Evolution

- Additive fields only within `.v1` topics.
- Breaking changes require a new topic suffix (`.v2`) and parallel consumer support.
