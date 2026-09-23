import { createHash, randomUUID } from 'node:crypto';
import { commerceConfig, getCart } from './cart.mjs';
import { demand } from './errors.mjs';

function validateCustomer(input) {
  demand(input && typeof input === 'object', 400, 'INVALID_CUSTOMER', 'Enter your delivery details.');
  const customer = {};
  for (const field of ['firstName', 'lastName', 'email', 'address', 'city', 'postalCode', 'country']) {
    demand(typeof input[field] === 'string' && input[field].trim().length > 0 && input[field].trim().length <= 200, 400, 'INVALID_CUSTOMER', `Enter a valid ${field}.`);
    customer[field] = input[field].trim();
  }
  demand(/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(customer.email), 400, 'INVALID_EMAIL', 'Enter a valid email address.');
  demand(commerceConfig.countries.some(country => country.code === customer.country), 400, 'INVALID_COUNTRY', 'Choose a supported delivery country.');
  demand(/^\d{5}(-\d{4})?$/.test(customer.postalCode), 400, 'INVALID_POSTAL_CODE', 'Enter a valid US ZIP code.');
  return customer;
}

export function createOrder(db, sessionId, input, idempotencyKey) {
  demand(typeof idempotencyKey === 'string' && /^[a-zA-Z0-9-]{16,100}$/.test(idempotencyKey), 400, 'INVALID_IDEMPOTENCY_KEY', 'A valid Idempotency-Key header is required.');
  const customer = validateCustomer(input.customer);
  demand(['approved', 'declined'].includes(input.mockPayment), 400, 'INVALID_PAYMENT', 'Choose a mock payment outcome.');
  demand(Number.isSafeInteger(input.expectedTotal) && input.expectedTotal >= 0, 400, 'INVALID_TOTAL', 'Refresh your order total before paying.');
  const shippingMethod = input.shippingMethod || 'standard';
  const fingerprint = createHash('sha256').update(JSON.stringify({ customer, shippingMethod, mockPayment: input.mockPayment, expectedTotal: input.expectedTotal })).digest('hex');
  db.exec('BEGIN IMMEDIATE');
  try {
    const previous = db.prepare('SELECT data, fingerprint FROM orders WHERE session_id = ? AND idempotency_key = ?').get(sessionId, idempotencyKey);
    if (previous) {
      demand(previous.fingerprint === fingerprint, 409, 'IDEMPOTENCY_CONFLICT', 'This payment reference was already used for a different request.');
      db.exec('COMMIT');
      return { order: JSON.parse(previous.data), replayed: true };
    }
    const cart = getCart(db, sessionId, shippingMethod);
    demand(cart.items.length > 0, 400, 'EMPTY_CART', 'Add a tool to your cart before checking out.');
    demand(cart.canCheckout, 409, 'INSUFFICIENT_STOCK', 'Stock has changed. Update the quantities in your cart.');
    demand(cart.total === input.expectedTotal, 409, 'TOTAL_CHANGED', 'Your total has changed. Review your cart before paying.');
    demand(input.mockPayment === 'approved', 402, 'PAYMENT_DECLINED', 'Mock payment declined. No order was created and your cart is unchanged.');
    const order = {
      id: `WB-${randomUUID().toUpperCase()}`,
      createdAt: new Date().toISOString(), status: 'confirmed', customer,
      payment: { provider: 'mock', status: 'paid', reference: `MOCK-${randomUUID()}` },
      items: cart.items.map(({ product, quantity, lineTotal }) => ({ productId: product.id, name: product.name, subtitle: product.subtitle, image: product.image, sku: product.sku, unitPrice: product.price, quantity, lineTotal })),
      subtotal: cart.subtotal, shipping: cart.shipping, tax: cart.tax, total: cart.total,
      currency: cart.currency, shippingMethod
    };
    for (const item of order.items) {
      const result = db.prepare('UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?').run(item.quantity, item.productId, item.quantity);
      demand(result.changes === 1, 409, 'INSUFFICIENT_STOCK', 'Stock changed while checking out. Please review your cart.');
    }
    db.prepare('INSERT INTO orders VALUES (?, ?, ?, ?, ?, ?)').run(order.id, sessionId, idempotencyKey, fingerprint, order.createdAt, JSON.stringify(order));
    db.prepare('DELETE FROM cart_items WHERE session_id = ?').run(sessionId);
    db.exec('COMMIT');
    return { order, replayed: false };
  } catch (error) {
    db.exec('ROLLBACK');
    throw error;
  }
}

export function listOrders(db, sessionId) {
  return { items: db.prepare('SELECT data FROM orders WHERE session_id = ? ORDER BY created_at DESC, rowid DESC').all(sessionId).map(row => JSON.parse(row.data)) };
}

export function getOrder(db, sessionId, orderId) {
  const row = db.prepare('SELECT data FROM orders WHERE session_id = ? AND id = ?').get(sessionId, orderId);
  demand(row, 404, 'ORDER_NOT_FOUND', 'This order could not be found in your local session.');
  return JSON.parse(row.data);
}