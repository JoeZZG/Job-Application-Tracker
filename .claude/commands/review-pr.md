# review-pr

Review a set of code changes before commit/PR.

## Goal
Find correctness, architecture, and maintainability issues with practical feedback.

## Input (provide in prompt)
- Changed files (or diff)
- Summary of what was intended
- Any areas you want extra focus on (security, cache, async, auth, etc.)

## Output format
1. Summary of what changed
2. Blocking issues
3. Important improvements
4. Nice-to-have polish
5. Regression risks
6. Architecture consistency check
7. Docs/README updates needed
8. Final recommendation (ready / needs fixes)

## Rules
- Prioritize high-impact feedback
- Avoid low-value nitpicks
- Check alignment with project architecture and scope