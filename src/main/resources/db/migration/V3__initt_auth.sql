INSERT INTO roles (name) VALUES ('ADMIN'), ('USER');

INSERT INTO permissions (code, description) VALUES
    ('USER_READ', 'Read user profile'),
    ('USER_WRITE', 'Update user profile'),
    ('PRODUCT_READ', 'View products'),
    ('PRODUCT_WRITE', 'Manage products'),
    ('ORDER_READ', 'View orders'),
    ('ORDER_WRITE', 'Create and manage orders'),
    ('ADMIN_ACCESS', 'Full admin access');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('USER_READ', 'USER_WRITE', 'PRODUCT_READ', 'ORDER_READ', 'ORDER_WRITE')
WHERE r.name = 'USER';