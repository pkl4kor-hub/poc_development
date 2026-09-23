# API Guide

Base URL: `http://localhost:3001/api`. The machine-readable source of truth is `/api/openapi.json`, generated from [backend/contract.mjs](../backend/contract.mjs). Request and response bodies are JSON. Monetary values are integer USD cents.

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/health` | API health |
| GET | `/openapi.json` | OpenAPI 3.1 contract |
| GET | `/config` | Currency, tax, delivery choices, supported countries, mock payment choices, live category and product counts |
| GET | `/products` | Catalog search/filter/sort |
| GET | `/products/{productId}` | Product, image path, specifications and current inventory |
| POST | `/sessions` | Create a local session; returns `sessionId` |
| GET | `/cart` | Session cart and server-calculated totals |
| PUT | `/cart/items/{productId}` | Set absolute quantity; zero removes |
| GET | `/orders` | Session order history, newest first |
| POST | `/orders` | Mock payment and atomic order creation |
| GET | `/orders/{orderId}` | Session-scoped order detail |

Cart and order endpoints require the `X-Session-Id` header. A missing or unknown token returns 401. The frontend creates a session automatically and retains its opaque token in localStorage. This is a POC ownership mechanism, not production authentication.

## Catalog

`GET /products` accepts `search`, `category`, `inStock=true`, `featured=true`, and `sort` (`featured`, `price-asc`, `price-desc`, `name`). Results are `{ items, total }`. Categories come from `/config` and match exact category names. Search is case-insensitive across name, subtitle, brand, description and SKU. Empty results return 200 with an empty array. There is no pagination for the eight-product demo catalog.

Images are frontend-relative asset paths, not URLs served by the commerce API. All seeded assets are labeled local placeholders.

## Cart

```json
{ "quantity": 2 }
```

`PUT /cart/items/gsr-18v-55` accepts an integer from 0 to 99, limited by live stock. It is a quantity setter rather than an increment operation. It returns the updated cart using standard shipping. `GET /cart?shippingMethod=express` obtains an express quote. Cart responses include `items`, `itemCount`, `subtotal`, `shipping`, `tax`, `total`, `currency`, `shippingMethod` and `canCheckout`. Availability is rechecked when reading the cart and again when creating the order.

## Checkout

`POST /orders` requires an `Idempotency-Key` header containing 16-100 alphanumeric/hyphen characters; a UUID is suitable.

```json
{
  "customer": {
    "firstName": "Alex",
    "lastName": "Morgan",
    "email": "alex@example.com",
    "address": "24 Workshop Avenue",
    "city": "Austin",
    "postalCode": "78701",
    "country": "US"
  },
  "shippingMethod": "express",
  "mockPayment": "approved",
  "expectedTotal": 17892
}
```

The example total is illustrative for one $149 tool with the current express and tax rules. Always obtain `expectedTotal` from the current cart quote. The server recalculates totals; this value is only a stale-quote guard and never an authoritative price.

An approved payment returns 201 with `{ order, replayed: false }`. The order contains immutable line-item and total snapshots, customer details, timestamp, generated order ID, confirmed status, and an explicitly mock payment reference. In one `BEGIN IMMEDIATE` transaction, the API checks stock and totals, decrements inventory, inserts the order, and clears the cart. Any failure rolls the transaction back.

Retrying an identical successful request using the same session and idempotency key returns 200 with the original order and `replayed: true`, even though its cart is now empty. Reusing that key with different checkout data returns 409. This prevents a second order or second inventory decrement after a lost response. The frontend retains the pending key for in-page retries; a reload does not persist an in-flight payment request. Check history before placing another order after an uncertain response.

`mockPayment: "declined"` returns 402 and leaves inventory, orders and cart unchanged. The payment selector offers only these two explicit test outcomes; no card details are accepted or required.

## Errors

```json
{
  "error": {
    "code": "INSUFFICIENT_STOCK",
    "message": "Only 0 of GOP 30-28 available."
  }
}
```

Typical statuses: 400 invalid data or empty cart; 401 invalid session; 402 declined mock payment; 403 disallowed browser origin; 404 unknown product/order/route; 409 stock, total or idempotency conflict; 413 oversized body; 415 non-JSON mutation body; 500 unexpected server failure. JSON mutation bodies are limited to 16 KB. Browser CORS is restricted to the configured frontend origin. CORS is not authentication and does not prevent direct local API clients.