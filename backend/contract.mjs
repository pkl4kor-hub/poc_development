const reference = name => ({ $ref: `#/components/schemas/${name}` });
const response = (description, schema) => ({ description, content: { 'application/json': { schema } } });
const body = schema => ({ required: true, content: { 'application/json': { schema } } });
const integer = { type: 'integer', minimum: 0 };
const money = { ...integer, description: 'Amount in integer USD cents.' };
const text = { type: 'string' };
const totals = { subtotal: money, shipping: money, tax: money, total: money, currency: { const: 'USD' }, shippingMethod: { enum: ['standard', 'express'] } };
const errorResponses = Object.fromEntries([400, 401, 402, 403, 404, 409, 413, 415, 500].map(status => [status, response('Request failed; inspect error.code and error.message.', reference('Error'))]));
const operation = (summary, schema, extras = {}) => ({ summary, responses: { 200: response('Success', schema), ...errorResponses }, ...extras });
const pathId = name => ({ name, in: 'path', required: true, schema: text });
const session = [{ localSession: [] }];

export const contract = {
  openapi: '3.1.0',
  info: { title: 'Workbench Commerce POC API', version: '1.0.0', description: 'Local API-first tools commerce. Mock payments only. Money is integer USD cents. Session tokens isolate demo carts and orders; they are not production authentication.' },
  servers: [{ url: '/api' }],
  paths: {
    '/health': { get: operation('Check API health', { type: 'object', properties: { status: { const: 'ok' } } }) },
    '/config': { get: operation('Get delivery, currency, mock payment options and live category counts', reference('Config')) },
    '/products': { get: operation('Search, filter and sort the catalog', { type: 'object', properties: { items: { type: 'array', items: reference('Product') }, total: integer } }, { parameters: ['search', 'category', 'sort', 'inStock', 'featured'].map(name => ({ name, in: 'query', schema: name === 'sort' ? { enum: ['featured', 'price-asc', 'price-desc', 'name'] } : name === 'inStock' || name === 'featured' ? { enum: ['true', 'false'] } : text })) }) },
    '/products/{productId}': { get: operation('Get product, specifications and live inventory', reference('Product'), { parameters: [pathId('productId')] }) },
    '/sessions': { post: { summary: 'Create a persistent local shopping session', responses: { 201: response('Session created', { type: 'object', required: ['sessionId'], properties: { sessionId: { type: 'string', format: 'uuid' } } }) } } },
    '/cart': { get: operation('Read cart with server-calculated totals', reference('Cart'), { security: session, parameters: [{ name: 'shippingMethod', in: 'query', schema: { enum: ['standard', 'express'], default: 'standard' } }] }) },
    '/cart/items/{productId}': { put: operation('Set an absolute quantity; zero removes the item', reference('Cart'), { security: session, parameters: [pathId('productId')], requestBody: body({ type: 'object', required: ['quantity'], properties: { quantity: { type: 'integer', minimum: 0, maximum: 99 } } }) }) },
    '/orders': {
      get: operation('Read this session\'s order history, newest first', { type: 'object', properties: { items: { type: 'array', items: reference('Order') } } }, { security: session }),
      post: {
        summary: 'Mock payment, create order, decrement stock and clear cart in one transaction', security: session,
        parameters: [{ name: 'Idempotency-Key', in: 'header', required: true, schema: { type: 'string', pattern: '^[a-zA-Z0-9-]{16,100}$' } }],
        requestBody: body({ type: 'object', required: ['customer', 'mockPayment', 'expectedTotal'], properties: { customer: reference('Customer'), mockPayment: { enum: ['approved', 'declined'] }, shippingMethod: { enum: ['standard', 'express'], default: 'standard' }, expectedTotal: money } }),
        responses: { 201: response('New confirmed order', reference('OrderResult')), 200: response('Idempotent replay of the original order', reference('OrderResult')), ...errorResponses }
      }
    },
    '/orders/{orderId}': { get: operation('Read one order in this session', reference('Order'), { security: session, parameters: [pathId('orderId')] }) }
  },
  components: {
    securitySchemes: { localSession: { type: 'apiKey', in: 'header', name: 'X-Session-Id', description: 'Opaque token from POST /sessions. Treat as a local demo bearer credential.' } },
    schemas: {
      Error: { type: 'object', required: ['error'], properties: { error: { type: 'object', required: ['code', 'message'], properties: { code: text, message: text } } } },
      Product: { type: 'object', required: ['id', 'name', 'price', 'stock', 'image', 'specs'], properties: { id: text, sku: text, name: text, subtitle: text, brand: text, category: text, description: text, price: money, stock: integer, featured: { type: 'boolean' }, image: text, specs: { type: 'object', additionalProperties: text } } },
      Customer: { type: 'object', required: ['firstName', 'lastName', 'email', 'address', 'city', 'postalCode', 'country'], properties: Object.fromEntries(['firstName', 'lastName', 'email', 'address', 'city', 'postalCode', 'country'].map(name => [name, { type: 'string', minLength: 1, maxLength: 200, ...(name === 'email' ? { format: 'email' } : name === 'country' ? { enum: ['US'] } : name === 'postalCode' ? { pattern: '^\\d{5}(-\\d{4})?$' } : {}) }])) },
      Cart: { type: 'object', properties: { ...totals, itemCount: integer, canCheckout: { type: 'boolean' }, items: { type: 'array', items: { type: 'object', properties: { product: reference('Product'), quantity: integer, lineTotal: money, available: { type: 'boolean' } } } } } },
      OrderResult: { type: 'object', properties: { order: reference('Order'), replayed: { type: 'boolean' } } },
      Order: { type: 'object', properties: { ...totals, id: text, createdAt: { type: 'string', format: 'date-time' }, status: { const: 'confirmed' }, customer: reference('Customer'), payment: { type: 'object', properties: { provider: { const: 'mock' }, status: { const: 'paid' }, reference: text } }, items: { type: 'array', items: { type: 'object', properties: { productId: text, name: text, subtitle: text, image: text, sku: text, unitPrice: money, quantity: integer, lineTotal: money } } } } },
      Config: { type: 'object', properties: { currency: { const: 'USD' }, taxRate: { type: 'number' }, freeShippingThreshold: money, shippingMethods: { type: 'array', items: { type: 'object', properties: { id: text, name: text, estimate: text, price: money } } }, countries: { type: 'array', items: { type: 'object', properties: { code: text, name: text } } }, paymentModes: { type: 'array', items: { type: 'object', properties: { id: text, name: text } } }, categories: { type: 'array', items: { type: 'object', properties: { name: text, count: integer } } }, productCount: integer, availableCount: integer } }
    }
  }
};