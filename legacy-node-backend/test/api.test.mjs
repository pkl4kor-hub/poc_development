import { test } from 'node:test';
import assert from 'node:assert/strict';
import { once } from 'node:events';
import { randomUUID } from 'node:crypto';
import { openDatabase } from '../database.mjs';
import { createApiServer } from '../server.mjs';

async function fixture(context) {
  const db = openDatabase();
  const server = createApiServer(db);
  server.listen(0, '127.0.0.1');
  await once(server, 'listening');
  context.after(async () => { server.closeAllConnections(); await new Promise(resolve => server.close(resolve)); db.close(); });
  const base = `http://127.0.0.1:${server.address().port}/api`;
  const sessionResponse = await fetch(`${base}/sessions`, { method: 'POST' });
  const { sessionId } = await sessionResponse.json();
  const headers = { 'Content-Type': 'application/json', 'X-Session-Id': sessionId };
  return { base, headers, db };
}

test('HTTP complete flow: browse, filter, detail, cart, checkout, confirmation, inventory and history', async context => {
  const { base, headers } = await fixture(context);
  const config = await (await fetch(`${base}/config`)).json();
  assert.equal(config.productCount, 8);
  assert.equal(config.categories.reduce((total, category) => total + category.count, 0), config.productCount);
  const catalog = await (await fetch(`${base}/products?search=drill&category=Drills%20%26%20drivers&inStock=true`)).json();
  assert.ok(catalog.total > 0);
  const product = await (await fetch(`${base}/products/${catalog.items[0].id}`)).json();
  assert.ok(product.image);
  assert.ok(product.specs);
  let response = await fetch(`${base}/cart/items/${product.id}`, { method: 'PUT', headers, body: JSON.stringify({ quantity: 1 }) });
  assert.equal(response.status, 200);
  const cart = await response.json();
  const body = { customer: { firstName: 'Alex', lastName: 'Morgan', email: 'alex@example.com', address: '24 Workshop Avenue', city: 'Austin', postalCode: '78701', country: 'US' }, mockPayment: 'approved', expectedTotal: cart.total };
  headers['Idempotency-Key'] = randomUUID();
  response = await fetch(`${base}/orders`, { method: 'POST', headers, body: JSON.stringify(body) });
  assert.equal(response.status, 201);
  const { order } = await response.json();
  assert.equal((await (await fetch(`${base}/products/${product.id}`)).json()).stock, product.stock - 1);
  assert.equal((await (await fetch(`${base}/cart`, { headers })).json()).itemCount, 0);
  assert.equal((await (await fetch(`${base}/orders/${order.id}`, { headers })).json()).id, order.id);
  assert.equal((await (await fetch(`${base}/orders`, { headers })).json()).items.length, 1);
  assert.equal((await fetch(`${base}/orders`, { method: 'POST', headers, body: JSON.stringify(body) })).status, 200);
});

test('HTTP validates sessions, malformed bodies, unknown resources and origins', async context => {
  const { base, headers } = await fixture(context);
  assert.equal((await fetch(`${base}/cart`)).status, 401);
  assert.equal((await fetch(`${base}/products/unknown`)).status, 404);
  assert.equal((await fetch(`${base}/missing`)).status, 404);
  assert.equal((await fetch(`${base}/cart/items/gsr-18v-55`, { method: 'PUT', headers, body: '{' })).status, 400);
  assert.equal((await fetch(`${base}/cart/items/gsr-18v-55`, { method: 'PUT', headers, body: 'null' })).status, 400);
  assert.equal((await fetch(`${base}/cart/items/gsr-18v-55`, { method: 'PUT', headers: { 'X-Session-Id': headers['X-Session-Id'] }, body: '{}' })).status, 415);
  assert.equal((await fetch(`${base}/config`, { headers: { Origin: 'http://untrusted.invalid' } })).status, 403);
  const preflight = await fetch(`${base}/orders`, { method: 'OPTIONS', headers: { Origin: 'http://localhost:5173' } });
  assert.equal(preflight.status, 204);
  assert.equal(preflight.headers.get('Access-Control-Allow-Origin'), 'http://localhost:5173');
});

test('OpenAPI contract is served and covers implemented commerce resources', async context => {
  const { base } = await fixture(context);
  const specification = await (await fetch(`${base}/openapi.json`)).json();
  assert.equal(specification.openapi, '3.1.0');
  for (const path of ['/products', '/products/{productId}', '/cart', '/cart/items/{productId}', '/orders', '/orders/{orderId}', '/sessions', '/config']) assert.ok(specification.paths[path]);
  assert.equal(specification.components.securitySchemes.localSession.name, 'X-Session-Id');
});