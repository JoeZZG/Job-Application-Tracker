# add-cache

Add Redis caching to a read endpoint safely.

## Goal
Improve performance for a suitable read path while keeping cache behavior predictable.

## Input (provide in prompt)
- Service name
- Endpoint to cache
- Why this endpoint is a good cache target
- Current response shape
- Related write endpoints (for invalidation)

## Output format
1. Cache target summary
2. Cache key design
3. TTL recommendation
4. Serialization approach
5. Read path changes
6. Invalidation strategy
7. Failure behavior if Redis is unavailable
8. Staleness risks
9. Test / verification steps
10. README/docs note (what to document)

## Rules
- Keep cache keys consistent and explicit
- Prefer simple invalidation strategy
- Do not cache sensitive data without calling it out