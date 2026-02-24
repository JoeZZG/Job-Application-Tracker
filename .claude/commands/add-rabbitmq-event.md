# add-rabbitmq-event

Add or modify an event-driven workflow using RabbitMQ.

## Goal
Implement a clear async producer-consumer flow across services.

## Input (provide in prompt)
- Event purpose
- Producer service
- Consumer service
- Trigger action (what causes publish)
- Payload fields
- Expected consumer side effect (e.g., create notification)

## Output format
1. Why async is used here
2. Producer and consumer ownership
3. Exchange / queue / routing key design
4. Event payload schema (JSON)
5. Producer implementation steps
6. Consumer implementation steps
7. Idempotency strategy (duplicate handling)
8. Error/retry handling basics
9. Local test steps
10. Demo scenario

## Rules
- Keep payload focused and versionable
- Make duplicate handling explicit
- Do not couple services through DB access