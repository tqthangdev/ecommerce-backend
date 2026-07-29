-- Sprint 3: Catalog seed (5 categories, 5 brands, 10 products with variants)

INSERT INTO categories (name, slug, description, is_active) VALUES
    ('Electronics', 'electronics',    'Phones, laptops, accessories',  TRUE),
    ('Fashion',     'fashion',        'Clothing, shoes, accessories',  TRUE),
    ('Home',        'home',           'Furniture, decor, kitchen',     TRUE),
    ('Books',       'books',          'Fiction, non-fiction, textbooks', TRUE),
    ('Sports',      'sports',         'Equipment, gear, activewear',   TRUE);

INSERT INTO brands (name, slug, description, is_active) VALUES
    ('Apple',     'apple',     'Premium consumer electronics',     TRUE),
    ('Samsung',   'samsung',   'Global electronics and home',      TRUE),
    ('Nike',      'nike',      'Athletic footwear and apparel',    TRUE),
    ('IKEA',      'ikea',      'Affordable home furnishings',      TRUE),
    ('Penguin',   'penguin',   'Classic and modern publishing',    TRUE);

-- Products
INSERT INTO products (name, slug, description, base_price, discount_percent, stock_quantity, is_active, is_featured, category_id, brand_id) VALUES
    ('iPhone 15 Pro',         'iphone-15-pro',         'Latest Apple flagship phone',           999.00, 5.00,  50, TRUE,  TRUE,  1, 1),
    ('Samsung Galaxy S24',    'samsung-galaxy-s24',    'Premium Android smartphone',             899.00, 8.00,  40, TRUE,  TRUE,  1, 2),
    ('Nike Air Max 90',       'nike-air-max-90',       'Classic running sneaker',                129.00, 0.00, 100, TRUE,  FALSE, 2, 3),
    ('Nike Pegasus 40',       'nike-pegasus-40',       'Daily trainer running shoe',             139.00, 10.00, 80, TRUE,  TRUE,  2, 3),
    ('IKEA Markus Chair',     'ikea-markus-chair',     'Ergonomic office chair',                 219.00, 0.00,  25, TRUE,  FALSE, 3, 4),
    ('IKEA Billy Bookcase',   'ikea-billy-bookcase',   'Classic tall bookcase',                  89.00,  15.00, 60, TRUE,  FALSE, 3, 4),
    ('Atomic Habits',         'atomic-habits',         'James Clear - habit formation',           16.99, 0.00, 200, TRUE,  TRUE,  4, 5),
    ('The Pragmatic Programmer', 'pragmatic-programmer','Classic software craft book',           39.99, 5.00, 75,  TRUE,  FALSE, 4, 5),
    ('Yoga Mat Premium',      'yoga-mat-premium',      'Non-slip eco yoga mat',                  49.00,  0.00, 120, TRUE,  FALSE, 5, 3),
    ('Dumbbell Set 20kg',     'dumbbell-set-20kg',     'Adjustable dumbbell pair',               159.00, 12.00, 30, TRUE,  TRUE,  5, 3);

-- Variants (selected products)
INSERT INTO product_variants (product_id, sku, color, size, price, stock_quantity) VALUES
    (1, 'IPH15P-BLK-128', 'Black',   '128GB', 999.00, 20),
    (1, 'IPH15P-BLU-256', 'Blue',    '256GB', 1099.00, 15),
    (1, 'IPH15P-WHT-512', 'White',   '512GB', 1299.00, 15),
    (2, 'SGS24-BLK-128',  'Black',   '128GB', 899.00, 20),
    (2, 'SGS24-VIO-256',  'Violet',  '256GB', 999.00, 20),
    (3, 'NAM90-BLK-42',   'Black',   '42',    129.00, 30),
    (3, 'NAM90-WHT-43',   'White',   '43',    129.00, 30),
    (3, 'NAM90-RED-44',   'Red',     '44',    129.00, 40),
    (5, 'IMK-BLK',        'Black',   NULL,    219.00, 25),
    (10, 'DBS20KG',       NULL,      NULL,    159.00, 30);
