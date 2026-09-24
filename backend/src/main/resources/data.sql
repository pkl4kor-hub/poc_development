INSERT OR IGNORE INTO users (id, username, password_hash, role, active, created_at, updated_at) VALUES
(1, 'buyer', '$2a$10$8V2sJTTf2dM7SE7t4f5uFuSB0vP6QJ1CCv3H6H6qgRh3sk1t6P3vO', 'BUYER', 1, datetime('now'), datetime('now')),
(2, 'seller', '$2a$10$u9t8k98bN.ZYWIL1N5PqTeWvDAz4cP0uYUV9vZiYfMgwjJpI3ZBJG', 'SELLER', 1, datetime('now'), datetime('now')),
(3, 'admin', '$2a$10$CgP4I0k4imX2vLlb4uCfIOhSmQm.8Y0Uom9I4xjPk1wDUDUbgfR9m', 'ADMIN', 1, datetime('now'), datetime('now'));

INSERT OR IGNORE INTO products (id, sku, name, category, description, price, stock_quantity, image_url, seller_id, active, created_at, updated_at)
VALUES
('gsr-18v-55', 'BOS-GSR1855', 'GSR 18V-55', 'Drills & drivers', 'A compact, brushless drill driver for drilling and screwdriving in wood and metal.', 14900, 12, '/images/gsr-18v-55.png', 2, 1, datetime('now'), datetime('now')),
('gks-190', 'BOS-GKS190', 'GKS 190', 'Saws', 'A corded circular saw for straight cuts in timber and sheet materials.', 17900, 8, '/images/gks-190.png', 2, 1, datetime('now'), datetime('now')),
('gst-90-be', 'BOS-GST90BE', 'GST 90 BE', 'Saws', 'A top-handle jigsaw for controlled curved and straight cuts.', 11900, 6, '/images/gst-90-be.png', 2, 1, datetime('now'), datetime('now')),
('gws-7-115', 'BOS-GWS7115', 'GWS 7-115', 'Grinding & sanding', 'A slim-grip corded angle grinder for metal finishing and light grinding work.', 6900, 15, '/images/gws-7-115.png', 2, 1, datetime('now'), datetime('now')),
('gex-125-1-ae', 'BOS-GEX1251', 'GEX 125-1 AE', 'Grinding & sanding', 'A compact random orbit sander for preparing and finishing wood surfaces.', 9900, 9, '/images/gex-125-1-ae.png', 2, 1, datetime('now'), datetime('now')),
('glm-40', 'BOS-GLM40', 'GLM 40', 'Measuring', 'A pocket-sized laser measure for distance, area and volume calculations.', 7900, 11, '/images/glm-40.png', 2, 1, datetime('now'), datetime('now')),
('gbh-2-26', 'BOS-GBH226', 'GBH 2-26', 'Drills & drivers', 'A corded rotary hammer for drilling concrete, masonry, wood and metal.', 19900, 4, '/images/gbh-2-26.png', 2, 1, datetime('now'), datetime('now')),
('gop-30-28', 'BOS-GOP3028', 'GOP 30-28', 'Multi-tools', 'A versatile corded multi-tool for plunge cuts, trimming and detail sanding.', 13900, 0, '/images/gop-30-28.png', 2, 1, datetime('now'), datetime('now'));
