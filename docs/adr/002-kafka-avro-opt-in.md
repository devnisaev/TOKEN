# ADR 002: Kafka Avro Serialization (Opt-In)

**Status:** Accepted (2026-09-26)  
**Context:** JSON envelopes are default; Apicurio Schema Registry is available in compose for production hardening.

## Decision

Keep **JSON** as the default Kafka/outbox serialization. Enable **Avro** per service via `tokenrealty.kafka.serialization=avro` using shared `AvroOutboxPayloadEncoder` and `AvroSchemaRegistrar`.

## Rationale

- Zero breaking change for existing consumers and CI Kafka ITs.
- Marketplace and Payment outbox relays can opt in independently.
- Schema registry (Apicurio) registers envelope + domain schemas on startup when Avro mode is active.

## Consequences

- Producers set `serialization=avro` only after consumers support Avro or dual-read.
- CI continues to run JSON-based Kafka integration tests unless a dedicated Avro IT job is added.
