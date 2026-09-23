const seededProducts = [
  { id: 'gsr-18v-55', sku: 'BOS-GSR1855', name: 'GSR 18V-55', subtitle: 'Brushless drill driver', brand: 'Bosch', category: 'Drills & drivers', price: 14900, stock: 12, featured: true, image: '/images/gsr-18v-55.webp', description: 'A compact, brushless drill driver for drilling and screwdriving in wood and metal. A metal chuck and two-speed gearbox suit everyday workshop and installation work.', specs: { 'Battery platform': '18 V', 'Max. torque': '55 Nm', 'No-load speed': '0-460 / 0-1,800 rpm', 'Chuck capacity': '1.5-13 mm', 'Max. drilling in wood': '35 mm', 'Included': 'Bare tool; battery and charger sold separately' } },
  { id: 'gks-190', sku: 'BOS-GKS190', name: 'GKS 190', subtitle: '190 mm circular saw', brand: 'Bosch', category: 'Saws', price: 17900, stock: 8, featured: true, image: '/images/gks-190.webp', description: 'A corded circular saw for straight cuts in timber and sheet materials. Its 70 mm cutting depth and adjustable bevel make it a useful workshop all-rounder.', specs: { 'Rated power': '1,400 W', 'Blade diameter': '190 mm', 'No-load speed': '5,500 rpm', 'Cutting depth at 90 degrees': '70 mm', 'Bevel capacity': '56 degrees', 'Included': 'Saw blade, parallel guide and hex key' } },
  { id: 'gst-90-be', sku: 'BOS-GST90BE', name: 'GST 90 BE', subtitle: '650 W jigsaw', brand: 'Bosch', category: 'Saws', price: 11900, stock: 6, featured: false, image: '/images/gst-90-be.webp', description: 'A top-handle jigsaw for controlled curved and straight cuts. Tool-free blade changes, variable speed and four-stage orbital action help adapt the cut to the material.', specs: { 'Rated power': '650 W', 'Stroke rate': '500-3,100 spm', 'Stroke length': '26 mm', 'Cutting depth in wood': '90 mm', 'Cutting depth in steel': '10 mm', 'Included': 'Jigsaw blade and hex key' } },
  { id: 'gws-7-115', sku: 'BOS-GWS7115', name: 'GWS 7-115', subtitle: '115 mm angle grinder', brand: 'Bosch', category: 'Grinding & sanding', price: 6900, stock: 15, featured: true, image: '/images/gws-7-115.webp', description: 'A slim-grip corded angle grinder for metal finishing and light grinding work. Its compact gear head makes it easier to work in confined spaces.', specs: { 'Rated power': '720 W', 'Disc diameter': '115 mm', 'No-load speed': '11,000 rpm', 'Spindle thread': 'M14', 'Weight': '1.9 kg', 'Included': 'Protective guard, auxiliary handle and wrench; disc not included' } },
  { id: 'gex-125-1-ae', sku: 'BOS-GEX1251', name: 'GEX 125-1 AE', subtitle: 'Random orbit sander', brand: 'Bosch', category: 'Grinding & sanding', price: 9900, stock: 9, featured: true, image: '/images/gex-125-1-ae.webp', description: 'A compact random orbit sander for preparing and finishing wood surfaces. Variable speed and an integrated dust box support controlled, comfortable sanding.', specs: { 'Rated power': '250 W', 'Sanding pad': '125 mm', 'No-load speed': '7,500-12,000 rpm', 'Orbit diameter': '2.5 mm', 'Weight': '1.3 kg', 'Included': 'Microfilter dust box and sanding sheet' } },
  { id: 'glm-40', sku: 'BOS-GLM40', name: 'GLM 40', subtitle: '40 m laser measure', brand: 'Bosch', category: 'Measuring', price: 7900, stock: 11, featured: false, image: '/images/glm-40.webp', description: 'A pocket-sized laser measure for distance, area and volume calculations. An illuminated display and measurement memory make quick on-site checks straightforward.', specs: { 'Measuring range': '0.15-40 m', 'Typical accuracy': '+/- 1.5 mm', 'Laser class': '2', 'Protection rating': 'IP54', 'Power supply': '2 x AAA batteries', 'Included': 'Batteries and protective pouch' } },
  { id: 'gbh-2-26', sku: 'BOS-GBH226', name: 'GBH 2-26', subtitle: 'SDS plus rotary hammer', brand: 'Bosch', category: 'Drills & drivers', price: 19900, stock: 4, featured: false, image: '/images/gbh-2-26.webp', description: 'A corded rotary hammer for drilling concrete, masonry, wood and metal. Three operating modes cover drilling, hammer drilling and light chiselling.', specs: { 'Rated power': '830 W', 'Impact energy': '2.7 J', 'Tool holder': 'SDS plus', 'Max. drilling in concrete': '26 mm', 'Weight': '2.7 kg', 'Included': 'Auxiliary handle, depth stop and carrying case' } },
  { id: 'gop-30-28', sku: 'BOS-GOP3028', name: 'GOP 30-28', subtitle: 'Oscillating multi-tool', brand: 'Bosch', category: 'Multi-tools', price: 13900, stock: 0, featured: false, image: '/images/gop-30-28.webp', description: 'A versatile corded multi-tool for plunge cuts, trimming and detail sanding. The Starlock accessory interface transfers power efficiently for renovation and fitting tasks.', specs: { 'Rated power': '300 W', 'No-load oscillation': '8,000-20,000 opm', 'Oscillation angle': '1.4 degrees each side', 'Accessory interface': 'Starlock', 'Weight': '1.5 kg', 'Included': 'Plunge-cut blade and hex key' } }
];

export const products = seededProducts.map(product => ({ ...product, image: product.image.replace('.webp', '.png') }));

export function listProducts(db, query = {}) {
  let items = db.prepare('SELECT * FROM products').all().map(decodeProduct);
  const search = String(query.search || '').trim().toLowerCase();
  if (search) items = items.filter(product => `${product.name} ${product.subtitle} ${product.brand} ${product.description} ${product.sku}`.toLowerCase().includes(search));
  if (query.category) items = items.filter(product => product.category === query.category);
  if (query.inStock === 'true') items = items.filter(product => product.stock > 0);
  if (query.featured === 'true') items = items.filter(product => product.featured);
  const sorters = {
    featured: (first, second) => Number(second.featured) - Number(first.featured) || first.name.localeCompare(second.name),
    'price-asc': (first, second) => first.price - second.price,
    'price-desc': (first, second) => second.price - first.price,
    name: (first, second) => first.name.localeCompare(second.name)
  };
  items.sort(sorters[query.sort] || sorters.featured);
  return { items, total: items.length };
}

export function decodeProduct(row) {
  return { ...row, featured: Boolean(row.featured), specs: JSON.parse(row.specs) };
}