import { DatabaseSync } from 'node:sqlite';
import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import { products } from './catalog.mjs';

export function openDatabase(path = ':memory:') {
  if (path !== ':memory:') mkdirSync(dirname(path), { recursive: true });
  const db = new DatabaseSync(path);
  db.exec(`
    PRAGMA foreign_keys = ON;
    PRAGMA journal_mode = WAL;
    PRAGMA busy_timeout = 5000;
    CREATE TABLE IF NOT EXISTS products (
      id TEXT PRIMARY KEY, sku TEXT NOT NULL, name TEXT NOT NULL, subtitle TEXT NOT NULL,
      brand TEXT NOT NULL, category TEXT NOT NULL, price INTEGER NOT NULL CHECK(price >= 0),
      stock INTEGER NOT NULL CHECK(stock >= 0), featured INTEGER NOT NULL,
      image TEXT NOT NULL, description TEXT NOT NULL, specs TEXT NOT NULL
    );
    CREATE TABLE IF NOT EXISTS sessions (id TEXT PRIMARY KEY, created_at TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS cart_items (
      session_id TEXT REFERENCES sessions(id), product_id TEXT REFERENCES products(id),
      quantity INTEGER NOT NULL CHECK(quantity BETWEEN 1 AND 99), PRIMARY KEY(session_id, product_id)
    );
    CREATE TABLE IF NOT EXISTS orders (
      id TEXT PRIMARY KEY, session_id TEXT NOT NULL REFERENCES sessions(id),
      idempotency_key TEXT NOT NULL, fingerprint TEXT NOT NULL, created_at TEXT NOT NULL,
      data TEXT NOT NULL, UNIQUE(session_id, idempotency_key)
    );
  `);
  const insert = db.prepare(`INSERT OR IGNORE INTO products
    (id, sku, name, subtitle, brand, category, price, stock, featured, image, description, specs)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`);
  for (const product of products) {
    insert.run(product.id, product.sku, product.name, product.subtitle, product.brand, product.category, product.price, product.stock, Number(product.featured), product.image, product.description, JSON.stringify(product.specs));
  }
  return db;
}