import { createServer } from 'node:http';
import { randomUUID } from 'node:crypto';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { openDatabase } from './database.mjs';
import { listProducts } from './catalog.mjs';
import { commerceConfig, getCart, getProduct, setCartItem } from './cart.mjs';
import { createOrder, getOrder, listOrders } from './orders.mjs';
import { ApiError, demand } from './errors.mjs';
import { contract } from './contract.mjs';

async function readBody(request) {
  demand(request.headers['content-type']?.split(';')[0].trim() === 'application/json', 415, 'JSON_REQUIRED', 'Send a JSON request body.');
  let size = 0;
  const chunks = [];
  for await (const chunk of request) {
    size += chunk.length;
    demand(size <= 16384, 413, 'BODY_TOO_LARGE', 'Request body exceeds 16 KB.');
    chunks.push(chunk);
  }
  try {
    const value = JSON.parse(Buffer.concat(chunks).toString());
    demand(value && typeof value === 'object' && !Array.isArray(value), 400, 'INVALID_JSON', 'Send a JSON object.');
    return value;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new ApiError(400, 'INVALID_JSON', 'Request contains invalid JSON.');
  }
}

export function createApiServer(db, { frontendOrigin = 'http://localhost:5173' } = {}) {
  return createServer(async (request, response) => {
    const origin = request.headers.origin;
    response.setHeader('Content-Type', 'application/json; charset=utf-8');
    response.setHeader('Cache-Control', 'no-store');
    response.setHeader('X-Content-Type-Options', 'nosniff');
    response.setHeader('Vary', 'Origin');
    const send = (status, data) => { response.writeHead(status); response.end(JSON.stringify(data)); };
    try {
      demand(!origin || origin === frontendOrigin, 403, 'ORIGIN_NOT_ALLOWED', 'This origin is not allowed.');
      if (origin) response.setHeader('Access-Control-Allow-Origin', origin);
      response.setHeader('Access-Control-Allow-Headers', 'Content-Type, X-Session-Id, Idempotency-Key');
      response.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, OPTIONS');
      if (request.method === 'OPTIONS') { response.writeHead(204); response.end(); return; }
      const url = new URL(request.url, 'http://localhost');
      const path = url.pathname;
      const method = request.method;
      if (method === 'GET' && path === '/api/health') return send(200, { status: 'ok' });
      if (method === 'GET' && path === '/api/openapi.json') return send(200, contract);
      if (method === 'GET' && path === '/api/config') return send(200, {
        ...commerceConfig,
        categories: db.prepare('SELECT category AS name, COUNT(*) AS count FROM products GROUP BY category ORDER BY category').all(),
        productCount: db.prepare('SELECT COUNT(*) AS count FROM products').get().count,
        availableCount: db.prepare('SELECT COUNT(*) AS count FROM products WHERE stock > 0').get().count
      });
      if (method === 'GET' && path === '/api/products') return send(200, listProducts(db, Object.fromEntries(url.searchParams)));
      const productMatch = path.match(/^\/api\/products\/([^/]+)$/);
      if (method === 'GET' && productMatch) return send(200, getProduct(db, decodeURIComponent(productMatch[1])));
      if (method === 'POST' && path === '/api/sessions') {
        const sessionId = randomUUID();
        db.prepare('INSERT INTO sessions VALUES (?, ?)').run(sessionId, new Date().toISOString());
        return send(201, { sessionId });
      }
      const isPrivate = path === '/api/cart' || path.startsWith('/api/cart/') || path === '/api/orders' || path.startsWith('/api/orders/');
      const sessionId = request.headers['x-session-id'];
      if (isPrivate) demand(typeof sessionId === 'string' && db.prepare('SELECT id FROM sessions WHERE id = ?').get(sessionId), 401, 'SESSION_REQUIRED', 'Create a local shopping session first.');
      if (method === 'GET' && path === '/api/cart') return send(200, getCart(db, sessionId, url.searchParams.get('shippingMethod') || 'standard'));
      const itemMatch = path.match(/^\/api\/cart\/items\/([^/]+)$/);
      if (method === 'PUT' && itemMatch) return send(200, setCartItem(db, sessionId, decodeURIComponent(itemMatch[1]), (await readBody(request)).quantity));
      if (method === 'GET' && path === '/api/orders') return send(200, listOrders(db, sessionId));
      if (method === 'POST' && path === '/api/orders') {
        const result = createOrder(db, sessionId, await readBody(request), request.headers['idempotency-key']);
        return send(result.replayed ? 200 : 201, result);
      }
      const orderMatch = path.match(/^\/api\/orders\/([^/]+)$/);
      if (method === 'GET' && orderMatch) return send(200, getOrder(db, sessionId, decodeURIComponent(orderMatch[1])));
      send(404, { error: { code: 'NOT_FOUND', message: 'API endpoint not found.' } });
    } catch (error) {
      if (!(error instanceof ApiError)) console.error(error);
      send(error.status || 500, { error: { code: error.code || 'INTERNAL_ERROR', message: error instanceof ApiError ? error.message : 'An unexpected error occurred. Please try again.' } });
    }
  });
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const port = Number(process.env.API_PORT || 3001);
  const db = openDatabase(process.env.DB_PATH || fileURLToPath(new URL('./data/commerce.sqlite', import.meta.url)));
  const server = createApiServer(db, { frontendOrigin: process.env.FRONTEND_ORIGIN || `http://localhost:${process.env.WEB_PORT || 5173}` });
  server.listen(port, '127.0.0.1', () => console.log(`Commerce API: http://localhost:${port}/api`));
  const close = () => server.close(() => { db.close(); process.exit(0); });
  process.on('SIGINT', close);
  process.on('SIGTERM', close);
}