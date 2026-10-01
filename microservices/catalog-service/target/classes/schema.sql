CREATE TABLE IF NOT EXISTS products (
    id TEXT PRIMARY KEY,
    sku TEXT NOT NULL,
    name TEXT NOT NULL,
    subtitle TEXT,
    brand TEXT DEFAULT 'Bosch',
    featured INTEGER DEFAULT 0,
    category TEXT NOT NULL,
    description TEXT NOT NULL,
    price INTEGER NOT NULL,
    stock_quantity INTEGER NOT NULL,
    image_url TEXT NOT NULL,
    seller_id INTEGER NOT NULL,
    specs TEXT,
    active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
