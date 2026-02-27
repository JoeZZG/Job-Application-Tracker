# refactor

Refactor a specific, scoped piece of code without changing observable behaviour.

## Goal

Improve code structure in one focused dimension (e.g. extract method, remove duplication,
improve readability) while keeping all existing tests green.

## Input (provide in prompt)

- File path and method/class to refactor
- A single refactor goal (extract method / remove duplication / improve naming / split class / etc.)
- What must NOT change (method signatures, exception types, public API, other files)
- Verification: which test(s) must still pass after the change

## Output format

1. Summary of what will change and why
2. The refactored code (only the changed file/method — no other files touched)
3. Confirmation that the stated tests still pass

## Rules

- **One goal only** — do not combine multiple refactor types in one pass
- Do not change method signatures, exception types, or public interfaces unless explicitly asked
- Do not touch files outside the stated scope
- Do not add new behaviour, new error handling, or new abstractions not asked for
- Do not add comments or docstrings to code you did not change
- Behaviour must be provably unchanged: the specified tests must pass

## Example prompt

```
文件：DeadlineEventConsumer.java
目标：把 Notification 对象构建逻辑提取为私有方法 buildNotification(DeadlineEventPayload)
约束：
  - 不改 handleDeadlineEvent 的方法签名
  - 不改任何字段赋值逻辑，只是搬移代码
  - 不要修改其他文件
验收：DeadlineEventConsumerTest 全部通过
```
