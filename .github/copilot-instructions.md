# Workbench POC

- [x] Confirm requirements: local tools commerce POC with separated frontend and API; no Docker or deployment.
- [x] Scaffold using Node 24 built-in HTTP, SQLite, and test runner; no external packages required.
- [x] Implement and test commerce domains before adding the frontend.
- [x] Implement and verify the shopping interface.
- [x] No additional editor extensions required.
- [x] Run syntax checks, API tests, and browser verification.
- [x] Create local run task and launch the application.
- [x] Document startup, API contract, limitations, and verification results.

## Conventions

- Keep catalog, carts, and orders in backend domain modules. Frontend consumes the HTTP API.
- Persist commerce data in SQLite. Calculate money in integer cents on the backend.
- Keep mock payment explicit; never request real payment credentials.
- Test each major module before proceeding. Keep stock updates and order creation atomic.
- Use local product images with a local fallback and record image sources.
- No production infrastructure, authentication claims, or deployment steps.