# plan-feature

Plan a feature before implementation.

## Goal
Create a clear implementation plan that fits the current microservices architecture and avoids unnecessary scope.

## Input (provide in prompt)
- Feature name
- What the user should be able to do
- Which service(s) may be involved
- Any constraints (must use Redis / RabbitMQ / JWT / existing APIs, etc.)

## Output format
1. Feature goal
2. Service ownership (which service owns what)
3. API changes (endpoints / request-response)
4. DB changes (tables / fields)
5. Cache impact (Redis)
6. Event impact (RabbitMQ)
7. Frontend impact (pages/components)
8. Test plan
9. Implementation order (step-by-step)
10. Risks / what to defer

## Rules
- Keep it simple and implementation-ready
- Do not introduce new frameworks unless necessary
- Respect current architecture boundaries
- Call out assumptions explicitly