const API = window.WORKBENCH_CONFIG.apiUrl;
const main = document.querySelector('#main');
const state = { config: null, cart: null, checkout: null, routeVersion: 0, pendingPayment: null };
let sessionId = localStorage.getItem('workbench-session');
let sessionPromise;
const escape = value => String(value ?? '').replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[character]));
const money = value => new Intl.NumberFormat('en-US', { style: 'currency', currency: state.config?.currency || 'USD' }).format(value / 100);
const date = value => new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
const itemLabel = items => {
  const count = items.reduce((total, item) => total + item.quantity, 0);
  return `${count} ${count === 1 ? 'item' : 'items'}`;
};
const link = (path, label, classes = 'button') => `<a class="${classes}" href="#${path}">${label}</a>`;
const image = (product, className = '') => `<div class="product-image ${className}"><img src="${escape(product.image)}" alt="${escape(product.name)} - labeled product image placeholder" loading="lazy"><div class="image-fallback" hidden>${escape(product.name)}<small>Product image unavailable</small></div></div>`;

async function ensureSession() {
  if (sessionId) return sessionId;
  if (!sessionPromise) sessionPromise = fetch(`${API}/sessions`, { method: 'POST' }).then(async response => {
    if (!response.ok) throw new Error('Unable to start a shopping session.');
    sessionId = (await response.json()).sessionId;
    localStorage.setItem('workbench-session', sessionId);
    return sessionId;
  }).finally(() => { sessionPromise = null; });
  return sessionPromise;
}

async function api(path, options = {}, retry = true) {
  const privatePath = path.startsWith('/cart') || path.startsWith('/orders');
  const headers = { ...options.headers };
  if (privatePath) headers['X-Session-Id'] = await ensureSession();
  if (options.body) headers['Content-Type'] = 'application/json';
  let response;
  try { response = await fetch(`${API}${path}`, { ...options, headers }); }
  catch { throw new Error('The commerce API is unavailable. Check that the local server is running, then retry.'); }
  const data = await response.json();
  if (response.status === 401 && retry) {
    sessionId = null;
    localStorage.removeItem('workbench-session');
    return api(path, options, false);
  }
  if (!response.ok) {
    const error = new Error(data.error?.message || 'Something went wrong. Please retry.');
    error.code = data.error?.code;
    throw error;
  }
  return data;
}

function updateCart(cart) {
  state.cart = cart;
  document.querySelector('#cart-count').textContent = cart.itemCount;
}

function toast(message) {
  const element = document.querySelector('#toast');
  element.textContent = message;
  element.hidden = false;
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => { element.hidden = true; }, 4500);
}

function showError(error) {
  const element = document.querySelector('#action-error');
  if (element) { element.textContent = error.message; element.hidden = false; element.focus(); }
  else toast(error.message);
}

function errors() { return '<div id="action-error" class="error" role="alert" tabindex="-1" hidden></div>'; }
function heading(eyebrow, title, detail = '') { return `<div class="page-heading"><p class="eyebrow">${eyebrow}</p><h1>${title}</h1>${detail ? `<p class="muted">${detail}</p>` : ''}</div>`; }
function stock(product) { return product.stock ? `<span class="stock"><span aria-hidden="true">&#9679;</span> ${product.stock} in stock</span>` : '<span class="out-of-stock">Out of stock</span>'; }
function summary(cart, action = '') {
  return `<aside class="order-summary"><h2>Order summary</h2><dl class="totals"><div><dt>Subtotal</dt><dd>${money(cart.subtotal)}</dd></div><div><dt>Delivery</dt><dd>${cart.shipping ? money(cart.shipping) : 'Free'}</dd></div><div><dt>Estimated tax (${Math.round(state.config.taxRate * 100)}%)</dt><dd>${money(cart.tax)}</dd></div><div class="total"><dt>Total <small>${escape(cart.currency)}</small></dt><dd>${money(cart.total)}</dd></div></dl>${action}<p class="fine-print">Demo prices and tax. No real charges or shipments.</p></aside>`;
}

function productCard(product) {
  return `<article class="product-card"><a href="#/product/${encodeURIComponent(product.id)}" class="product-media" aria-label="View ${escape(product.name)}">${image(product)}${!product.stock ? '<span class="media-label">Out of stock</span>' : ''}</a><div class="product-copy"><p class="eyebrow">${escape(product.brand)} / ${escape(product.category)}</p><a class="product-title" href="#/product/${encodeURIComponent(product.id)}"><h3>${escape(product.name)}</h3></a><p class="product-subtitle">${escape(product.subtitle)}</p><div class="product-bottom"><strong>${money(product.price)}</strong><button class="add-button" data-add="${escape(product.id)}" ${!product.stock ? 'disabled' : ''} aria-label="Add ${escape(product.name)} to cart" title="Add ${escape(product.name)} to cart">+</button></div></div></article>`;
}

async function home() {
  const { items } = await api('/products?featured=true');
  return `<section class="shop-intro"><div><p class="eyebrow accent">READY FOR THE NEXT PROJECT</p><h1>Good work.<br>Starts here.</h1><p>Power tools, workshop essentials, and the right tool for your next job.</p>${link('/catalog', 'Browse tools <span aria-hidden="true">&#8599;</span>', 'button primary')}</div><div class="intro-index"><span class="index-number">${String(state.config.productCount).padStart(2, '0')}</span><span>Tools in the workshop</span><div class="intro-rule"></div><strong>${state.config.categories.length} categories</strong><span>${state.config.availableCount} tools ready to order</span></div></section><section class="category-band" aria-label="Tool categories">${state.config.categories.map(category => link(`/catalog?category=${encodeURIComponent(category.name)}`, `${escape(category.name)} <span>${category.count} &#8599;</span>`, 'category-link')).join('')}</section><section class="featured"><div class="section-heading"><div><p class="eyebrow">THE WORKSHOP EDIT</p><h2>Tools worth reaching for.</h2></div>${link('/catalog', 'View all tools &#8594;', 'text-link')}</div><div class="product-grid">${items.map(productCard).join('')}</div></section><section class="service-strip"><div><strong>Built for real work</strong><span>Detailed specifications on every tool</span></div><div><strong>Keep the project moving</strong><span>Free standard delivery from ${money(state.config.freeShippingThreshold)}</span></div><div><strong>Your workshop, on record</strong><span>Orders saved in this browser's local session</span></div></section>`;
}

async function catalog(params) {
  const search = params.get('search') || '';
  const selectedCategory = params.get('category') || '';
  const selectedSort = params.get('sort') || 'featured';
  const { items, total } = await api(`/products?${params}`);
  return `${heading('THE TOOL ROOM', 'Browse tools', 'Find the right fit for the work ahead.')}<form id="catalog-form" class="catalog-controls"><label class="search-label">Search tools<div class="search-group"><input name="search" type="search" value="${escape(search)}" placeholder="Model, tool, or keyword" aria-label="Search tools"><button class="button primary" type="submit">Search</button></div></label><label>Category<select name="category"><option value="">All categories</option>${state.config.categories.map(category => `<option value="${escape(category.name)}" ${category.name === selectedCategory ? 'selected' : ''}>${escape(category.name)} (${category.count})</option>`).join('')}</select></label><label>Sort by<select name="sort">${[['featured', 'Featured'], ['price-asc', 'Price: low to high'], ['price-desc', 'Price: high to low'], ['name', 'Name']].map(([value, label]) => `<option value="${value}" ${value === selectedSort ? 'selected' : ''}>${label}</option>`).join('')}</select></label><label class="checkbox-label"><input name="inStock" type="checkbox" ${params.get('inStock') === 'true' ? 'checked' : ''}> In stock only</label></form><div class="results-line"><span>${total} ${total === 1 ? 'tool' : 'tools'}${search ? ` matching &quot;${escape(search)}&quot;` : ''}</span>${params.size ? link('/catalog', 'Clear filters', 'text-link') : ''}</div>${total ? `<div class="product-grid catalog-grid">${items.map(productCard).join('')}</div>` : `<div class="empty-state"><h2>No tools found.</h2><p>Try a different keyword or category.</p>${link('/catalog', 'Clear filters', 'button secondary')}</div>`}`;
}

async function productDetail(id) {
  const product = await api(`/products/${encodeURIComponent(id)}`);
  return `<nav class="breadcrumbs" aria-label="Breadcrumb">${link('/catalog', 'All tools', '')}<span>/</span>${link(`/catalog?category=${encodeURIComponent(product.category)}`, escape(product.category), '')}<span>/</span><span>${escape(product.name)}</span></nav><section class="product-detail"><div class="detail-media"><button class="zoom-image" data-zoom="${escape(product.image)}" data-name="${escape(product.name)}" title="Enlarge product image" aria-label="Enlarge ${escape(product.name)} image">${image(product)}<span class="zoom-label">&#8599; View image</span></button><p class="fine-print">Labeled placeholder. Product photography is unavailable in this local demo.</p></div><div class="detail-copy"><p class="eyebrow accent">${escape(product.brand)} PROFESSIONAL</p><h1>${escape(product.name)}</h1><p class="detail-subtitle">${escape(product.subtitle)}</p><p class="sku">${escape(product.sku)}</p><div class="detail-price">${money(product.price)}</div>${stock(product)}<p class="description">${escape(product.description)}</p>${errors()}<form id="add-form" data-product="${escape(product.id)}" class="add-form"><label>Quantity<input name="quantity" type="number" min="1" max="${Math.min(product.stock, 99)}" value="1" required ${!product.stock ? 'disabled' : ''}></label><button class="button primary" ${!product.stock ? 'disabled' : ''}>${product.stock ? 'Add to cart' : 'Out of stock'} <span aria-hidden="true">+</span></button></form><p class="fine-print">${escape(product.specs.Included)}</p><div class="delivery-note">Free standard delivery from ${money(state.config.freeShippingThreshold)}</div></div></section><section class="spec-section"><h2>Know your tool.</h2><div><p class="eyebrow">TECHNICAL SPECIFICATIONS</p><dl class="specs">${Object.entries(product.specs).map(([key, value]) => `<div><dt>${escape(key)}</dt><dd>${escape(value)}</dd></div>`).join('')}</dl></div></section>`;
}

async function cartPage() {
  const cart = await api('/cart');
  updateCart(cart);
  if (!cart.items.length) return `${heading('YOUR WORKSHOP', 'Your cart')}<div class="empty-state"><h2>A project starts with the right tools.</h2><p>Your cart is empty.</p>${link('/catalog', 'Browse tools', 'button primary')}</div>`;
  return `${heading('YOUR WORKSHOP', 'Your cart', `${cart.itemCount} ${cart.itemCount === 1 ? 'item' : 'items'} ready for the next job.`)}${errors()}<div class="commerce-layout"><section class="cart-items">${cart.items.map(item => `<article class="cart-row">${link(`/product/${item.product.id}`, image(item.product), 'cart-image')}<div class="cart-item-copy"><p class="eyebrow">${escape(item.product.brand)}</p>${link(`/product/${item.product.id}`, escape(item.product.name), 'product-title')}<p>${escape(item.product.subtitle)}</p><span class="muted">${money(item.product.price)} each</span>${!item.available ? `<p class="error-inline">Only ${item.product.stock} available. Adjust your quantity.</p>` : ''}<button class="text-button remove-button" data-remove="${item.product.id}" aria-label="Remove ${escape(item.product.name)}">Remove</button></div><div class="cart-item-actions"><div class="quantity-stepper"><button data-quantity="${item.quantity - 1}" data-product="${item.product.id}" aria-label="Decrease ${escape(item.product.name)} quantity" title="Decrease quantity">&minus;</button><span aria-label="Quantity">${item.quantity}</span><button data-quantity="${item.quantity + 1}" data-product="${item.product.id}" ${item.quantity >= Math.min(item.product.stock, 99) ? 'disabled' : ''} aria-label="Increase ${escape(item.product.name)} quantity" title="Increase quantity">+</button></div><strong>${money(item.lineTotal)}</strong></div></article>`).join('')}${link('/catalog', '&#8592; Continue browsing', 'text-link')}</section>${summary(cart, cart.canCheckout ? link('/checkout', 'Continue to checkout &#8594;', 'button primary full') : '<p class="error-inline">Update unavailable items before checkout.</p>')}</div>`;
}

const steps = current => `<ol class="checkout-steps"><li class="${current === 1 ? 'current' : ''}"><span>1</span> Delivery</li><li class="${current === 2 ? 'current' : ''}"><span>2</span> Mock payment</li><li class="${current === 3 ? 'current' : ''}"><span>3</span> Confirmation</li></ol>`;
function customerField(name, label, autocomplete, type = 'text', extra = '') {
  return `<label>${label}<input name="${name}" type="${type}" autocomplete="${autocomplete}" value="${escape(state.checkout?.customer?.[name] || '')}" required maxlength="200" ${extra}></label>`;
}

async function checkout() {
  const shippingMethod = state.checkout?.shippingMethod || state.config.shippingMethods[0].id;
  const cart = await api(`/cart?shippingMethod=${encodeURIComponent(shippingMethod)}`);
  updateCart(cart);
  if (!cart.canCheckout) return `${heading('CHECKOUT', 'Your cart needs attention.')}<div class="empty-state"><p>${cart.items.length ? 'Stock has changed. Review your quantities before continuing.' : 'Add a tool before checking out.'}</p>${link('/cart', 'Return to cart', 'button primary')}</div>`;
  return `${heading('THE FINISHING TOUCHES', 'Checkout')}${steps(1)}${errors()}<div class="commerce-layout"><form id="checkout-form" class="checkout-form"><h2>Delivery details</h2><p class="muted small">Demo orders only. Use fictional details.</p><div class="form-grid">${customerField('firstName', 'First name', 'given-name')}${customerField('lastName', 'Last name', 'family-name')}<div class="span-two">${customerField('email', 'Email address', 'email', 'email')}</div><div class="span-two">${customerField('address', 'Street address', 'street-address')}</div>${customerField('city', 'City', 'address-level2')}${customerField('postalCode', 'ZIP code', 'postal-code', 'text', 'pattern="[0-9]{5}(-[0-9]{4})?" title="Five-digit US ZIP code, optionally followed by four digits"')}<label class="span-two">Country<select name="country">${state.config.countries.map(country => `<option value="${escape(country.code)}">${escape(country.name)}</option>`).join('')}</select></label></div><fieldset class="delivery-options"><legend>Delivery method</legend>${state.config.shippingMethods.map(method => `<label class="radio-option"><input type="radio" name="shippingMethod" value="${method.id}" ${method.id === shippingMethod ? 'checked' : ''}><span><strong>${escape(method.name)}</strong><small>${escape(method.estimate)}</small></span><strong>${method.id === 'standard' && cart.subtotal >= state.config.freeShippingThreshold ? 'Free' : money(method.price)}</strong></label>`).join('')}</fieldset><button class="button primary" type="submit">Review mock payment &#8594;</button></form><div id="checkout-summary">${summary(cart)}</div></div>`;
}

async function payment() {
  if (!state.checkout) return `${heading('CHECKOUT', 'Delivery details needed')}<div class="empty-state"><p>Complete delivery details before reviewing payment.</p>${link('/checkout', 'Continue to delivery', 'button primary')}</div>`;
  const cart = await api(`/cart?shippingMethod=${state.checkout.shippingMethod}`);
  updateCart(cart);
  if (!cart.canCheckout) return `${heading('CHECKOUT', 'Your cart needs attention.')}<div class="empty-state">${link('/cart', 'Return to cart', 'button primary')}</div>`;
  const customer = state.checkout.customer;
  const shipping = state.config.shippingMethods.find(method => method.id === state.checkout.shippingMethod);
  return `${heading('ONE LAST CHECK', 'Review & mock payment')}${steps(2)}${errors()}<div class="commerce-layout"><section class="payment-review"><div class="review-heading"><h2>Deliver to</h2>${link('/checkout', 'Edit', 'text-link')}</div><address>${escape(customer.firstName)} ${escape(customer.lastName)}<br>${escape(customer.address)}<br>${escape(customer.city)}, ${escape(customer.postalCode)}<br>${escape(customer.country)}<br>${escape(customer.email)}</address><p class="small">${escape(shipping.name)} / ${escape(shipping.estimate)}</p><div class="review-items">${cart.items.map(item => `<div><span>${escape(item.product.name)} <small>&times; ${item.quantity}</small></span><strong>${money(item.lineTotal)}</strong></div>`).join('')}</div><form id="payment-form"><div class="mock-notice"><strong>Mock payment</strong><p>No card details. No real charge. This creates a local demo order.</p></div><fieldset class="payment-options"><legend>Payment outcome</legend>${state.config.paymentModes.map((mode, index) => `<label class="radio-option"><input type="radio" name="mockPayment" value="${mode.id}" ${index === 0 ? 'checked' : ''}><span>${escape(mode.name)}</span></label>`).join('')}</fieldset><button class="button primary full" type="submit">Place demo order &middot; ${money(cart.total)}</button></form></section>${summary(cart)}</div>`;
}

function orderItems(order) {
  return `<div class="order-items">${order.items.map(item => `<div class="order-item">${image(item)}<div>${link(`/product/${item.productId}`, escape(item.name), 'product-title')}<p>${escape(item.subtitle)}</p><span class="muted">Qty ${item.quantity} &middot; ${money(item.unitPrice)} each</span></div><strong>${money(item.lineTotal)}</strong></div>`).join('')}</div>`;
}

async function orderPage(id, confirmation = false) {
  const order = await api(`/orders/${encodeURIComponent(id)}`);
  const customer = order.customer;
  return `${heading(confirmation ? 'ALL SET FOR THE NEXT JOB' : 'YOUR WORKSHOP RECORD', confirmation ? 'Order confirmed.' : 'Order details', `${date(order.createdAt)} &middot; Mock payment approved`)}${confirmation ? steps(3) : ''}<div class="confirmation-bar"><span class="status-badge">Confirmed</span><span class="order-id">${escape(order.id)}</span></div><div class="commerce-layout"><section>${orderItems(order)}<div class="order-delivery"><h2>Delivery address</h2><address>${escape(customer.firstName)} ${escape(customer.lastName)}<br>${escape(customer.address)}<br>${escape(customer.city)}, ${escape(customer.postalCode)}, ${escape(customer.country)}</address><p>${escape(state.config.shippingMethods.find(method => method.id === order.shippingMethod)?.name || order.shippingMethod)}</p><p class="fine-print">Demo order only. No shipment or confirmation email will be sent.</p></div><div class="button-row">${link('/orders', 'View order history', 'button primary')}${link('/catalog', 'Continue browsing', 'button secondary')}</div></section>${summary(order)}</div>`;
}

async function orderHistory() {
  const { items } = await api('/orders');
  return `${heading('YOUR WORKSHOP RECORD', 'Order history', `${items.length} ${items.length === 1 ? 'order' : 'orders'} in this local session.`)}${items.length ? `<div class="history-list">${items.map(order => `<article class="history-row"><div><p class="eyebrow">${date(order.createdAt)}</p><h2>${itemLabel(order.items)} <span class="status-badge">Confirmed</span></h2><p class="order-id">${escape(order.id)}</p><p class="muted">${order.items.map(item => `${escape(item.name)} &times; ${item.quantity}`).join(' / ')}</p></div><div class="history-action"><strong>${money(order.total)}</strong>${link(`/orders/${order.id}`, 'View order &#8594;', 'button secondary')}</div></article>`).join('')}</div>` : `<div class="empty-state"><h2>No orders yet.</h2><p>Your next project is a good place to start.</p>${link('/catalog', 'Browse tools', 'button primary')}</div>`}`;
}

async function render() {
  const version = ++state.routeVersion;
  const route = (location.hash.slice(1) || '/').split('?');
  const path = route[0];
  const params = new URLSearchParams(route[1]);
  main.setAttribute('aria-busy', 'true');
  main.innerHTML = '<div class="loading" role="status">Loading...</div>';
  document.querySelectorAll('[data-nav]').forEach(element => {
    const active = path.includes(element.dataset.nav);
    element.classList.toggle('active', active);
    if (active) element.setAttribute('aria-current', 'page'); else element.removeAttribute('aria-current');
  });
  try {
    state.config = await api('/config');
    let content;
    if (path === '/') content = await home();
    else if (path === '/catalog') content = await catalog(params);
    else if (path.startsWith('/product/')) content = await productDetail(decodeURIComponent(path.slice(9)));
    else if (path === '/cart') content = await cartPage();
    else if (path === '/checkout') content = await checkout();
    else if (path === '/payment') content = await payment();
    else if (path.startsWith('/confirmation/')) content = await orderPage(decodeURIComponent(path.slice(14)), true);
    else if (path === '/orders') content = await orderHistory();
    else if (path.startsWith('/orders/')) content = await orderPage(decodeURIComponent(path.slice(8)));
    else content = `${heading('NOT FOUND', 'That page is not in the workshop.')}${link('/catalog', 'Browse tools', 'button primary')}`;
    if (version !== state.routeVersion) return;
    main.innerHTML = content;
    document.title = `${main.querySelector('h1')?.textContent || 'Tools'} | Workbench`;
    if (!['/cart', '/checkout', '/payment'].includes(path)) updateCart(await api('/cart'));
    window.scrollTo(0, 0);
    main.focus({ preventScroll: true });
  } catch (error) {
    if (version !== state.routeVersion) return;
    main.innerHTML = `${heading('SOMETHING NEEDS ATTENTION', 'Unable to load this page.')}<div class="error" role="alert">${escape(error.message)}</div><button class="button primary" data-retry>Retry</button> ${link('/catalog', 'Browse tools', 'button secondary')}`;
  } finally {
    if (version === state.routeVersion) main.removeAttribute('aria-busy');
  }
}

async function addToCart(productId, quantity, button) {
  button.disabled = true;
  try {
    const cart = await api('/cart');
    const existing = cart.items.find(item => item.product.id === productId)?.quantity || 0;
    updateCart(await api(`/cart/items/${encodeURIComponent(productId)}`, { method: 'PUT', body: JSON.stringify({ quantity: existing + quantity }) }));
    state.pendingPayment = null;
    toast('Added to your cart.');
  } catch (error) { showError(error); }
  finally { button.disabled = false; }
}

document.addEventListener('error', event => {
  if (event.target.tagName === 'IMG') {
    event.target.hidden = true;
    const fallback = event.target.nextElementSibling;
    if (fallback) fallback.hidden = false;
  }
}, true);

document.addEventListener('click', async event => {
  if (event.target.closest('.skip-link')) {
    event.preventDefault();
    main.focus();
    return;
  }
  const button = event.target.closest('button');
  if (!button) return;
  if (button.hasAttribute('data-add')) await addToCart(button.dataset.add, 1, button);
  if (button.hasAttribute('data-quantity') || button.hasAttribute('data-remove')) {
    button.disabled = true;
    try {
      updateCart(await api(`/cart/items/${button.dataset.product || button.dataset.remove}`, { method: 'PUT', body: JSON.stringify({ quantity: button.hasAttribute('data-remove') ? 0 : Number(button.dataset.quantity) }) }));
      state.pendingPayment = null;
      await render();
    } catch (error) { showError(error); button.disabled = false; }
  }
  if (button.hasAttribute('data-retry')) await render();
  if (button.hasAttribute('data-zoom')) {
    document.querySelector('#dialog-image').innerHTML = image({ name: button.dataset.name, image: button.dataset.zoom });
    document.querySelector('#image-dialog').showModal();
  }
  if (button.classList.contains('close-dialog')) document.querySelector('#image-dialog').close();
});

document.addEventListener('submit', async event => {
  const form = event.target;
  event.preventDefault();
  const values = Object.fromEntries(new FormData(form));
  if (form.id === 'catalog-form') {
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(values)) if (value) params.set(key, key === 'inStock' ? 'true' : value);
    location.hash = `/catalog?${params}`;
  }
  if (form.id === 'add-form') await addToCart(form.dataset.product, Number(values.quantity), form.querySelector('button'));
  if (form.id === 'checkout-form') {
    const { shippingMethod, ...customer } = values;
    state.checkout = { customer, shippingMethod };
    state.pendingPayment = null;
    location.hash = '/payment';
  }
  if (form.id === 'payment-form') {
    const button = form.querySelector('button');
    button.disabled = true;
    button.textContent = 'Processing mock payment...';
    try {
      const body = { ...state.checkout, mockPayment: values.mockPayment, expectedTotal: state.cart.total };
      const serialized = JSON.stringify(body);
      if (!state.pendingPayment || state.pendingPayment.body !== serialized) state.pendingPayment = { key: crypto.randomUUID(), body: serialized };
      const result = await api('/orders', { method: 'POST', headers: { 'Idempotency-Key': state.pendingPayment.key }, body: serialized });
      state.checkout = null;
      state.pendingPayment = null;
      location.hash = `/confirmation/${result.order.id}`;
    } catch (error) {
      if (error.code) state.pendingPayment = null;
      showError(error);
      button.disabled = false;
      button.textContent = `Place demo order · ${money(state.cart.total)}`;
    }
  }
});

document.addEventListener('change', async event => {
  if (event.target.closest('#catalog-form') && event.target.name !== 'search') event.target.form.requestSubmit();
  if (event.target.name === 'shippingMethod') {
    const form = event.target.form;
    const values = Object.fromEntries(new FormData(form));
    const { shippingMethod, ...customer } = values;
    state.checkout = { customer, shippingMethod };
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    try {
      const cart = await api(`/cart?shippingMethod=${shippingMethod}`);
      if (form.isConnected && form.elements.shippingMethod.value === shippingMethod) {
        updateCart(cart);
        document.querySelector('#checkout-summary').innerHTML = summary(cart);
      }
    } catch (error) { showError(error); }
    finally { button.disabled = false; }
  }
});

document.querySelector('#api-link').href = `${API}/openapi.json`;
window.addEventListener('hashchange', render);
render();