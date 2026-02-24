# implement-endpoint

Implement one backend endpoint end-to-end in a specific service.

## Goal
Add a complete endpoint with validation, service logic, and clear API contract.

## Input (provide in prompt)
- Service name
- Endpoint path + method
- Business purpose
- Request fields
- Response fields
- Auth requirement (public / JWT)
- Any DB/cache/event side effects

## Output format
1. Endpoint summary
2. Request DTO / response DTO
3. Validation rules
4. Controller changes
5. Service-layer logic
6. Repository/data access changes
7. Entity/schema changes (if any)
8. Error cases and status codes
9. Cache invalidation impact
10. Event publishing/consumption impact
11. Tests to add
12. Sample request/response JSON

## Rules
- Keep service ownership correct
- Do not expose entities directly as API responses
- Be explicit about authorization checks