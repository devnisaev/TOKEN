# Lombok — TokenRealty

Adapted from Titan `lombok.mdc`. Cursor rule: [`.cursor/rules/lombok.mdc`](../../.cursor/rules/lombok.mdc).

---

## What we took from Titan

| Area | TokenRealty |
|------|-------------|
| `@RequiredArgsConstructor` on services | Same |
| `@Getter`/`@Setter` on JPA entities | Same |
| `@NoArgsConstructor(access = PROTECTED)` | Target standard (some legacy entities still use public no-arg) |
| Records for DTOs | Same — primary pattern |
| No `@Data` on domain/entities | Same intent |
| Sensitive field caution | Wallet keys, password hashes, KYC content |

## What we simplified

| Titan | TokenRealty |
|-------|-------------|
| Parent POM inherits Lombok | Per-service `pom.xml` |
| Rich domain in `domain/model/` | Not yet — logic in `service/` + JPA |
| `@Builder` discouraged on DTOs | Optional on records for tests (`AuthDtos`) |
| `cardsystem-common` | `tokenrealty-security` uses plain Java, no Lombok; `BaseEntity` in `tokenrealty-jpa` uses Lombok |

## Migration note

When extracting rich domain models (Payment, Escrow), keep behavior methods explicit — no Lombok `@Data` on aggregates.
