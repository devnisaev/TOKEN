# Search Service

PostgreSQL-backed search index for **TokenRealty** listings and buildings (v1 uses JPA + `LIKE` / case-insensitive text match — not OpenSearch).

Port **8098** · Database **`search_service`** · Context path **`/api`**

## Run

```bash
# From repo root — install shared libs if needed
./token-realty-app/mvnw -pl tokenrealty-security,tokenrealty-web,tokenrealty-jpa,tokenrealty-kafka,tokenrealty-events install

# Start PostgreSQL (creates DB on first connect with ddl-auto: update)
docker compose -f token-realty-app/docker-compose.yml up postgres -d

# Run service
./token-realty-app/mvnw -f search-service/pom.xml spring-boot:run
```

Kafka ingest (local):

```bash
KAFKA_ENABLED=true spring.profiles.active=local ./token-realty-app/mvnw -f search-service/pom.xml spring-boot:run
```

## Test

```bash
./token-realty-app/mvnw -f search-service/pom.xml test
```

## API

All endpoints require JWT (`Authorization: Bearer …`). Login via Auth Service `:8083`.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/v1/search/listings?q=` | Search listings (optional `listingType`, `minPrice`, `maxPrice`, pagination) |
| GET | `/v1/search/buildings?q=` | Search buildings (pagination) |

Example:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8098/api/v1/search/listings?q=PRIMARY&listingType=PRIMARY&minPrice=50&maxPrice=200&page=0&size=20"
```

## Kafka consumers

| Topic | Index update |
|-------|----------------|
| `tokenrealty.marketplace.listing.created.v1` | Upsert `listing_index` by `listingId` |
| `tokenrealty.registry.flat.tokenized.v1` | Enrich listings by `flatId`; upsert building + increment `flatCount` |
| `tokenrealty.registry.building.approved.v1` | Upsert `building_index` with `approvedAt` |
| `tokenrealty.valuation.updated.v1` | Set NAV on listings by `flatId`; update building latest NAV |

Idempotency: `KafkaEventConsumer` + `processed_events` dedupe by `eventId`.

## Notes

- v1 search text is built from indexed IDs and numeric fields (building/flat names are not in Kafka payloads).
- Gateway route + BFF wrapper are planned in PLATFORM-SPEC §10.17.
