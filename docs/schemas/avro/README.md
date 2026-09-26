# Avro schemas (opt-in)

JSON envelope remains the default Kafka format. Avro schemas and `AvroEventCodec` in `tokenrealty-events` support opt-in binary serialization.

## Local registry

With the Kafka compose profile:

```bash
docker compose --profile kafka up -d kafka schema-registry
# Apicurio Registry UI/API: http://localhost:8092
```

## Schemas

| File | Event type |
|------|------------|
| `PaymentConfirmed.avsc` | `tokenrealty.payment.payment.confirmed.v1` |
| `TradeSettled.avsc` | `tokenrealty.marketplace.trade.settled.v1` |
| `EventEnvelope.avsc` | Wrapper with JSON payload string (hybrid migration) |

Register schemas via Apicurio REST API or CI pipeline before enabling `tokenrealty.kafka.serialization=avro` on a service.
