# rules.md

## Code Rules
1. Keep it simple. No libraries or patterns I cannot explain in an interview.
2. Layers stay separate: controllers never touch repositories.
3. Never return entities from controllers; use DTOs.
4. All business validation lives in the service layer.
5. All error handling goes through `GlobalExceptionHandler`; no try/catch in controllers.
6. Use `java.time` only (no `Date`, no `Calendar`).
7. Constructor injection (via Lombok `@RequiredArgsConstructor`), no field `@Autowired`.
8. Comments explain WHY, not WHAT.

## Naming
- Classes: PascalCase, methods/variables: camelCase, tables: snake_case
- DTOs end with `Request` / `Response`
- Exceptions end with `Exception`

## Git Rules
- Small commits, meaningful messages: `feat: add slot generator`, `fix: ...`, `test: ...`
- One feature per commit; never one giant commit
- Branch per feature if possible

## Do Not Add
Microservices, Kafka, Redis, JWT/OAuth, complex frontend frameworks (React/Vue/Angular), Docker orchestration.
(Plain static HTML/CSS/JS frontend is permitted for visual demonstration).

## Definition of Done (per feature)
- Code compiles and follows the rules above
- Has at least one test
- README updated if an API changed
- I can explain it without looking at the code
