# Commit Messages — TOKEN-NNN Index

Cursor rule: [`.cursor/rules/commit-messages.mdc`](../../.cursor/rules/commit-messages.mdc) (always applied).

## Purpose

Track changes with sequential **TOKEN-001**, **TOKEN-002**, … prefixes in commit subjects. These are repo-local change indexes — not external ticket IDs.

## Format

```text
TOKEN-NNN: Short imperative description.
```

## Agent workflow

1. Check recent commits: `git log --oneline -20`
2. Pick the next unused `TOKEN-NNN` number
3. **Track batch** (e.g. tracks 228–252, one merge): **one commit**, **one TOKEN id** — not one commit per track
4. **Otherwise:** one logical slice per commit; sequential ids
5. Commit only when the user asks; push only when explicitly requested

### Track batch example (preferred)

```text
TOKEN-111: Implement PLATFORM-SPEC tracks 228–252.

Avro outbox on six services, CI E2E 16 specs + nightly Hardhat, Onfido
client, KMS stub fix, ADRs 003–006, gateway OpenAPI, integration tests.
```

## Example log

```text
TOKEN-001: Add shared outbound REST infrastructure in tokenrealty-web.
TOKEN-002: Extend ServiceRestClientBuilder with timeout and trace propagation.
TOKEN-003: Migrate marketplace PaymentClient to DownstreamRestClientSupport.
TOKEN-004: Document RestClientOperations requirement in docs/rules.
TOKEN-005: Add TOKEN-NNN commit message convention rule.
```
