import { decodeProduct } from './catalog.mjs';
import { demand } from './errors.mjs';

export const commerceConfig = {
  currency: 'USD',
  taxRate: 0.08,
  freeShippingThreshold: 15000,
  shippingMethods: [
    { id: 'standard', name: 'Standard delivery', estimate: '3-5 business days', price: 850 },
    { id: 'express', name: 'Express delivery', estimate: '1-2 business days', price: 1800 }
  ],
  countries: [{ code: 'US', name: 'United States' }],
  paymentModes: [ { id: 'approved', name: 'Approve payment' }, { id: 'declined', name: 'Decline payment' } ]
};

export function getProduct(db, id) {
  const row = db.prepare('SELECT * FROM products WHERE id = ?').get(id);
  demand(row, 404, 'PRODUCT_NOT_FOUND', 'This tool could not be found.');
  return decodeProduct(row);
}

export function getCart(db, sessionId, shippingMethod = 'standard') {
  const method = commerceConfig.shippingMethods.find(option => option.id === shippingMethod);
  demand(method, 400, 'INVALID_SHIPPING', 'Choose an available delivery method.');
  const items = db.prepare('SELECT product_id, quantity FROM cart_items WHERE session_id = ? ORDER BY rowid').all(sessionId).map(row => {
    const product = getProduct(db, row.product_id);
    return { product, quantity: row.quantity, lineTotal: product.price * row.quantity, available: row.quantity <= product.stock };
  });
  const subtotal = items.reduce((total, item) => total + item.lineTotal, 0);
  const shipping = !items.length || (method.id === 'standard' && subtotal >= commerceConfig.freeShippingThreshold) ? 0 : method.price;
  const tax = Math.round(subtotal * commerceConfig.taxRate);
  return { items, itemCount: items.reduce((total, item) => total + item.quantity, 0), subtotal, shipping, tax, total: subtotal + shipping + tax, currency: commerceConfig.currency, shippingMethod, canCheckout: items.length > 0 && items.every(item => item.available) };
}

export function setCartItem(db, sessionId, productId, quantity) {
  demand(Number.isInteger(quantity) && quantity >= 0 && quantity <= 99, 400, 'INVALID_QUANTITY', 'Quantity must be a whole number between 0 and 99.');
  const product = getProduct(db, productId);
  demand(quantity <= product.stock, 409, 'INSUFFICIENT_STOCK', `Only ${product.stock} of ${product.name} available.`);
  if (quantity === 0) db.prepare('DELETE FROM cart_items WHERE session_id = ? AND product_id = ?').run(sessionId, productId);
  else db.prepare('INSERT INTO cart_items VALUES (?, ?, ?) ON CONFLICT(session_id, product_id) DO UPDATE SET quantity = excluded.quantity').run(sessionId, productId, quantity);
  return getCart(db, sessionId);
}