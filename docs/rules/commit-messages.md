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
3. One logical slice per commit; sequential ids within a batch
4. Commit only when the user asks; push only when explicitly requested

## Example log

```text
TOKEN-001: Add shared outbound REST infrastructure in tokenrealty-web.
TOKEN-002: Extend ServiceRestClientBuilder with timeout and trace propagation.
TOKEN-003: Migrate marketplace PaymentClient to DownstreamRestClientSupport.
TOKEN-004: Document RestClientOperations requirement in docs/rules.
TOKEN-005: Add TOKEN-NNN commit message convention rule.
```
