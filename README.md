# Workbench Tools POC

A locally runnable, API-first tools store with a separate vanilla JavaScript frontend and Node HTTP API. Catalog, carts, and orders are separate domain modules; commerce data persists in SQLite. Node 24 supplies the HTTP server, SQLite driver, and test runner. There are no external packages, Docker containers, cloud services, or deployment steps.

## Run Locally

Prerequisite: Node.js 24 or newer.

```powershell
npm.cmd start
```

Alternatively, run the **Run Workbench POC** VS Code task, or execute `node scripts/dev.mjs`. No package installation is needed.

- Storefront: http://localhost:5173
- API health: http://localhost:3001/api/health
- OpenAPI 3.1 contract: http://localhost:3001/api/openapi.json

The servers bind to loopback only. Stop the terminal task with Ctrl+C before starting another instance.

To use different ports in PowerShell:

```powershell
$env:WEB_PORT = '5174'
$env:API_PORT = '3002'
npm.cmd start
```

The launcher passes both ports to the processes. The frontend gets its API URL from a generated `/config.js`, not a hardcoded browser URL. For separate terminals use `npm.cmd run start:api` and `npm.cmd run start:web`; keep their `WEB_PORT` and `API_PORT` values consistent. Optional overrides are `API_URL` on the frontend, `FRONTEND_ORIGIN` on the API, and `DB_PATH` for the SQLite file. Use the advertised `localhost` storefront URL to match the configured CORS origin.

## Demonstrate The Flow

1. Open the homepage and select **Browse tools**.
2. Search for `brushless`, choose **Drills & drivers**, and open **GSR 18V-55**.
3. View its image, open the enlarged image, and inspect its specifications and available stock.
4. Add the tool to the cart. Adjust quantities or remove items; totals come from the API.
5. Continue to checkout. Enter fictional delivery details, such as Alex Morgan, `alex@example.com`, 24 Workshop Avenue, Austin, ZIP 78701.
6. Choose standard or express delivery. The API recalculates the shipping charge and total.
7. Continue to **Review & mock payment**. First choose **Decline payment** and submit: no order is created, stock stays unchanged, and the cart remains intact.
8. Choose **Approve payment** and submit. The API creates the order, decrements inventory, and clears the cart in one SQLite transaction.
9. Inspect the confirmation and **Order history**. Reload the browser to verify the order remains available. Revisit the product to see updated inventory.

Do not enter real payment credentials. There is no card form or real payment integration.

## Dynamic Behavior

The frontend does not contain a product array or manufacture commerce results. It retrieves products, specifications, prices, stock, category counts, featured products, search/filter results, delivery options, cart quantities, totals, confirmation, and order history from the API. Product counts on the homepage and cart badges are derived from actual data. Orders have generated IDs, timestamps, payment references, and snapshots of purchased items.

Sample product data is seeded from [backend/catalog.mjs](backend/catalog.mjs) on first use. Existing products are not overwritten on startup, so stock changes survive restarts. A changed seed price does not automatically update an existing database. The deliberately fixed demo rules live in [backend/cart.mjs](backend/cart.mjs): USD, 8% tax on merchandise, standard delivery at $8.50, free standard delivery at $150, express delivery at $18, and US-only delivery. There is no fake external tax, shipping, or payment service.

The eight products have realistic model names, descriptions, specifications, and inclusions. Prices and quantities are sample data, not live manufacturer offers. Product specifications should be verified against the intended regional SKU before real commercial use.

## Images

Every product has a valid local, model-labeled 960 x 720 PNG placeholder. Actual product-photography retrieval was blocked by this environment's network/DNS restrictions. The placeholders explicitly identify themselves; they are not represented as photographs. A further model-named HTML fallback appears if an image fails to load.

See [docs/image-sources.md](docs/image-sources.md) for asset provenance, attempted retrieval, and rights notes. Replace these assets with verified, licensed product photography before presenting this as a real store. The local demo requires no external images, fonts, or CDN access.

## Structure

- [backend/catalog.mjs](backend/catalog.mjs): seed catalog, search, filtering and sorting.
- [backend/cart.mjs](backend/cart.mjs): cart quantities, stock validation, pricing and delivery rules.
- [backend/orders.mjs](backend/orders.mjs): checkout validation, mock payment, idempotency and atomic order creation.
- [backend/database.mjs](backend/database.mjs): schema and persistence.
- [backend/server.mjs](backend/server.mjs): HTTP transport, sessions, validation and CORS.
- [backend/contract.mjs](backend/contract.mjs): served OpenAPI contract.
- [frontend/app.js](frontend/app.js): headless storefront consuming the API.
- [frontend/server.mjs](frontend/server.mjs): static files and runtime API configuration.

This is MACH-style in its headless, API-first separation and bounded commerce modules, not a claim of production microservices or cloud-native infrastructure. The backend is intentionally one modular process and one database. Real payments can later replace the explicit mock behavior; search, caching and events can be added behind domain APIs when actually needed.

## Verify

```powershell
npm.cmd test
npm.cmd run check
```

The automated suite covers HTTP purchase flow, catalog behavior, quantity validation, server totals, payment decline, order creation, stock updates, duplicate submission replay, session isolation, competing carts, transaction rollback, database reopen persistence, valid local images, CORS, request parsing and contract availability. Tests use isolated in-memory or temporary databases and do not alter your demo database.

See [docs/verification.md](docs/verification.md) for the executed browser flow and results. Browser checks were performed using Playwright through the editor's integrated browser; no browser testing package is installed in this repo.

## Persistence And POC Limits

The API creates `backend/data/commerce.sqlite` and SQLite sidecars automatically. The browser stores only an opaque local session token in localStorage. Carts and orders persist in SQLite; delivery form drafts remain in browser memory and must be re-entered after a reload before payment. Clearing browser storage or changing the storefront port creates a different session and hides previous session history, without deleting those orders.

To get a fresh catalog without deleting existing data, stop the app and start it with `DB_PATH` pointing to a new file. Resetting the original database requires deliberately removing its files while the servers are stopped. Do not delete a running SQLite database.

There are no accounts, production authentication, email sending, fulfillment, refunds, real card processing, tax compliance, stock reservations, admin catalog editor, rate limiting, or deployment configuration. Session tokens are demo bearer credentials, not a production identity design. Use fictional customer data. The mock provider and order/stock transaction are synchronous; real payments require a different lifecycle with authorization, webhooks, retries and reconciliation. Cart updates use absolute quantities, with last-write-wins behavior across tabs; checkout always revalidates availability and totals.