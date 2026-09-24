import { test } from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { openDatabase } from '../database.mjs';
import { listProducts } from '../catalog.mjs';
import { getCart, getProduct, setCartItem } from '../cart.mjs';
import { createOrder, getOrder, listOrders } from '../orders.mjs';

const customer = { firstName: 'Alex', lastName: 'Morgan', email: 'alex@example.com', address: '24 Workshop Avenue', city: 'Austin', postalCode: '78701', country: 'US' };
function fixture(context) {
  const db = openDatabase();
  context.after(() => db.close());
  const sessionId = randomUUID();
  db.prepare('INSERT INTO sessions VALUES (?, ?)').run(sessionId, new Date().toISOString());
  return { db, sessionId };
}
function request(db, sessionId, overrides = {}) {
  return { customer, shippingMethod: 'standard', mockPayment: 'approved', expectedTotal: getCart(db, sessionId).total, ...overrides };
}
function hasCode(code) { return error => error.code === code; }

test('catalog searches, filters, sorts and contains images and specifications', context => {
  const { db } = fixture(context);
  assert.equal(listProducts(db, { search: 'brushless' }).items[0].id, 'gsr-18v-55');
  assert.equal(listProducts(db, { category: 'Saws' }).total, 2);
  assert.equal(listProducts(db, { search: 'nonexistent' }).total, 0);
  const sorted = listProducts(db, { inStock: 'true', sort: 'price-asc' }).items;
  assert.ok(sorted.every(product => product.stock > 0));
  assert.ok(sorted.every((product, index) => index === 0 || product.price >= sorted[index - 1].price));
  assert.ok(listProducts(db).items.every(product => product.image && Object.keys(product.specs).length >= 5));
});

test('cart computes totals and supports quantity changes and removal', context => {
  const { db, sessionId } = fixture(context);
  let cart = setCartItem(db, sessionId, 'gsr-18v-55', 1);
  assert.equal(cart.subtotal, 14900);
  assert.equal(cart.shipping, 850);
  assert.equal(cart.tax, 1192);
  assert.equal(cart.total, 16942);
  cart = setCartItem(db, sessionId, 'gsr-18v-55', 2);
  assert.equal(cart.shipping, 0);
  assert.equal(getCart(db, sessionId, 'express').shipping, 1800);
  assert.equal(setCartItem(db, sessionId, 'gsr-18v-55', 0).total, 0);
});

test('invalid quantity, shipping and unavailable stock are rejected', context => {
  const { db, sessionId } = fixture(context);
  for (const quantity of [-1, 1.5, '1', 100]) assert.throws(() => setCartItem(db, sessionId, 'gsr-18v-55', quantity), hasCode('INVALID_QUANTITY'));
  assert.throws(() => setCartItem(db, sessionId, 'gop-30-28', 1), hasCode('INSUFFICIENT_STOCK'));
  assert.throws(() => getCart(db, sessionId, 'teleport'), hasCode('INVALID_SHIPPING'));
});

test('approved payment atomically creates an order, updates stock and clears cart', context => {
  const { db, sessionId } = fixture(context);
  setCartItem(db, sessionId, 'gsr-18v-55', 2);
  const result = createOrder(db, sessionId, request(db, sessionId), randomUUID());
  assert.equal(result.order.status, 'confirmed');
  assert.equal(result.order.payment.provider, 'mock');
  assert.equal(result.order.items[0].quantity, 2);
  assert.equal(getProduct(db, 'gsr-18v-55').stock, 10);
  assert.equal(getCart(db, sessionId).items.length, 0);
  assert.equal(listOrders(db, sessionId).items[0].id, result.order.id);
  assert.equal(getOrder(db, sessionId, result.order.id).total, 32184);
});

test('declined payment preserves stock and cart and creates no order', context => {
  const { db, sessionId } = fixture(context);
  setCartItem(db, sessionId, 'gsr-18v-55', 1);
  assert.throws(() => createOrder(db, sessionId, request(db, sessionId, { mockPayment: 'declined' }), randomUUID()), hasCode('PAYMENT_DECLINED'));
  assert.equal(getProduct(db, 'gsr-18v-55').stock, 12);
  assert.equal(getCart(db, sessionId).itemCount, 1);
  assert.equal(listOrders(db, sessionId).items.length, 0);
});

test('duplicate payment submissions replay the same order without decrementing stock twice', context => {
  const { db, sessionId } = fixture(context);
  setCartItem(db, sessionId, 'gsr-18v-55', 1);
  const body = request(db, sessionId);
  const key = randomUUID();
  const first = createOrder(db, sessionId, body, key);
  const second = createOrder(db, sessionId, body, key);
  assert.equal(first.order.id, second.order.id);
  assert.equal(second.replayed, true);
  assert.equal(getProduct(db, 'gsr-18v-55').stock, 11);
  assert.equal(listOrders(db, sessionId).items.length, 1);
  assert.throws(() => createOrder(db, sessionId, { ...body, expectedTotal: 1 }, key), hasCode('IDEMPOTENCY_CONFLICT'));
});

test('stale stock fails without partial inventory changes', context => {
  const { db, sessionId } = fixture(context);
  setCartItem(db, sessionId, 'gsr-18v-55', 1);
  setCartItem(db, sessionId, 'gbh-2-26', 4);
  const body = request(db, sessionId);
  db.prepare('UPDATE products SET stock = 0 WHERE id = ?').run('gbh-2-26');
  assert.throws(() => createOrder(db, sessionId, body, randomUUID()), hasCode('INSUFFICIENT_STOCK'));
  assert.equal(getProduct(db, 'gsr-18v-55').stock, 12);
  assert.equal(listOrders(db, sessionId).items.length, 0);
});

test('server rejects total tampering, invalid customer and empty cart', context => {
  const { db, sessionId } = fixture(context);
  assert.throws(() => createOrder(db, sessionId, request(db, sessionId), randomUUID()), hasCode('EMPTY_CART'));
  setCartItem(db, sessionId, 'gsr-18v-55', 1);
  assert.throws(() => createOrder(db, sessionId, request(db, sessionId, { expectedTotal: 1 }), randomUUID()), hasCode('TOTAL_CHANGED'));
  assert.throws(() => createOrder(db, sessionId, request(db, sessionId, { customer: { ...customer, email: 'bad' } }), randomUUID()), hasCode('INVALID_EMAIL'));
  assert.throws(() => createOrder(db, sessionId, request(db, sessionId, { customer: { ...customer, postalCode: 'bad' } }), randomUUID()), hasCode('INVALID_POSTAL_CODE'));
});

test('orders and carts are scoped to their session', context => {
  const { db, sessionId } = fixture(context);
  const other = randomUUID();
  db.prepare('INSERT INTO sessions VALUES (?, ?)').run(other, new Date().toISOString());
  setCartItem(db, sessionId, 'gsr-18v-55', 1);
  const { order } = createOrder(db, sessionId, request(db, sessionId), randomUUID());
  assert.equal(listOrders(db, other).items.length, 0);
  assert.equal(getCart(db, other).itemCount, 0);
  assert.throws(() => getOrder(db, other, order.id), hasCode('ORDER_NOT_FOUND'));
});

test('order, inventory and session survive database close and reopen', context => {
  const directory = mkdtempSync(join(tmpdir(), 'workbench-test-'));
  context.after(() => rmSync(directory, { recursive: true, force: true }));
  const path = join(directory, 'commerce.sqlite');
  const db = openDatabase(path);
  const sessionId = randomUUID();
  let order;
  try {
    db.prepare('INSERT INTO sessions VALUES (?, ?)').run(sessionId, new Date().toISOString());
    setCartItem(db, sessionId, 'gsr-18v-55', 1);
    order = createOrder(db, sessionId, request(db, sessionId), randomUUID()).order;
  } finally { db.close(); }
  const reopened = openDatabase(path);
  try {
    assert.equal(getProduct(reopened, 'gsr-18v-55').stock, 11);
    assert.equal(getOrder(reopened, sessionId, order.id).total, order.total);
    assert.equal(getCart(reopened, sessionId).itemCount, 0);
    assert.equal(listOrders(reopened, sessionId).items.length, 1);
  } finally { reopened.close(); }
});

test('competing carts cannot buy the same final unit', context => {
  const { db, sessionId } = fixture(context);
  const other = randomUUID();
  db.prepare('INSERT INTO sessions VALUES (?, ?)').run(other, new Date().toISOString());
  db.prepare('UPDATE products SET stock = 1 WHERE id = ?').run('gsr-18v-55');
  setCartItem(db, sessionId, 'gsr-18v-55', 1);
  setCartItem(db, other, 'gsr-18v-55', 1);
  const otherRequest = request(db, other);
  createOrder(db, sessionId, request(db, sessionId), randomUUID());
  assert.throws(() => createOrder(db, other, otherRequest, randomUUID()), hasCode('INSUFFICIENT_STOCK'));
  assert.equal(getProduct(db, 'gsr-18v-55').stock, 0);
  assert.equal(getCart(db, other).itemCount, 1);
  assert.equal(listOrders(db, other).items.length, 0);
});

test('database failure after stock mutation rolls back inventory and preserves cart', context => {
  const { db, sessionId } = fixture(context);
  setCartItem(db, sessionId, 'gsr-18v-55', 1);
  db.exec("CREATE TRIGGER simulate_failure BEFORE INSERT ON orders BEGIN SELECT RAISE(ABORT, 'simulated write failure'); END;");
  assert.throws(() => createOrder(db, sessionId, request(db, sessionId), randomUUID()), /simulated write failure/);
  assert.equal(getProduct(db, 'gsr-18v-55').stock, 12);
  assert.equal(getCart(db, sessionId).itemCount, 1);
  assert.equal(listOrders(db, sessionId).items.length, 0);
});

test('every seeded product has a valid local PNG asset', context => {
  const { db } = fixture(context);
  for (const product of listProducts(db).items) {
    const asset = readFileSync(new URL(`../../frontend${product.image}`, import.meta.url));
    assert.equal(asset.subarray(0, 8).toString('hex'), '89504e470d0a1a0a', product.id);
    assert.equal(asset.readUInt32BE(16), 960);
    assert.equal(asset.readUInt32BE(20), 720);
  }
});