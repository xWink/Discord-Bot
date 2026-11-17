INSERT IGNORE INTO shop_items (code, name, category, price, base_price, duration_ms, featured, rotating) VALUES
    ('fx-fireworks', 'Fireworks victory effect', 'effect', 8, 8, 259200000, 0, 1),
    ('fx-trophy', 'Golden trophy victory effect', 'effect', 8, 8, 259200000, 0, 1),
    ('fx-stars', 'Starfall victory effect', 'effect', 12, 12, 604800000, 0, 1);

UPDATE shop_items SET name = 'Confetti victory effect', duration_ms = 259200000 WHERE code = 'victory-fx';
