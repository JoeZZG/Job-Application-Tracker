# write-tests

Add targeted tests for backend or frontend changes.

## Goal
Cover critical paths and edge cases without overtesting low-value details.

## Input (provide in prompt)
- Target files / module / endpoint / page
- What was changed
- Backend or frontend
- Any known risks (auth, validation, cache, async)

## Output format
1. Test scope summary
2. Critical test cases (happy path)
3. Validation/error cases
4. Auth/authorization cases (if applicable)
5. Edge cases
6. Mocking/stubbing strategy
7. Exact test files to add/modify
8. What can be deferred

## Rules
- Focus on behavior, not implementation details
- Prioritize correctness and regression risk
- Keep tests readable and maintainable