# Refactor Guide

This document captures the project’s expectations for small, safe refactors.

## Goals

- Improve structure or readability in a narrow scope.
- Preserve observable behavior and public APIs.
- Keep all existing tests green.

## Required Inputs (when requesting a refactor)

- File path and specific method/class to refactor.
- A single refactor goal (extract method, remove duplication, improve naming, split class, etc.).
- Constraints (what must NOT change: method signatures, exception types, public API, or other files).
- Verification: which tests must pass after the change.

## Output Checklist

1. Summary of what changed and why.
2. Only the refactored code (no unrelated files touched).
3. Confirmation the specified tests still pass.

## Rules

- One goal only — avoid mixing multiple refactor types in one pass.
- Do not change method signatures, exception types, or public interfaces unless explicitly asked.
- Do not touch files outside the stated scope.
- Do not add new behavior or new abstractions not requested.
- Behavior must be provably unchanged: specified tests must pass.

## Example Prompt

```
File: DeadlineEventConsumer.java
Goal: Extract notification construction into private method buildNotification(DeadlineEventPayload)
Constraints:
  - Do not change handleDeadlineEvent signature
  - Do not alter any assignment logic, only move it
  - Do not modify any other files
Verification: DeadlineEventConsumerTest passes
```
