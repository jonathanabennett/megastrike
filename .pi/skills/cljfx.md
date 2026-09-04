# CLJFX Game Development Rules

When editing code in this project:
1. Always keep component maps pure (`:fx/type`).
2. Run project tests after every major refactor using the bash tool: `just test`.
3. Wrap UI mutations in `cljfx.api/on-fx-thread` if side-effects are present.
4. If a test fails, read the stack trace, edit the file, and re-run the test command.

