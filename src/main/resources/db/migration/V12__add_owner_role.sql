-- ==========================================
-- V12: Add OWNER role and user management permissions
-- ==========================================


-- ==========================================
-- Add OWNER role
-- ==========================================

INSERT INTO roles (name)
SELECT 'OWNER'
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE name = 'OWNER'
);


-- ==========================================
-- Add new permissions
-- ==========================================

INSERT INTO permissions (code, description)
SELECT 'USER_CREATE', 'Create users'
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions
    WHERE code = 'USER_CREATE'
);


INSERT INTO permissions (code, description)
SELECT 'ROLE_MANAGE', 'Manage user roles'
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions
    WHERE code = 'ROLE_MANAGE'
);


-- ==========================================
-- OWNER permissions
-- OWNER has full access
-- ==========================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'OWNER'
AND NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = r.id
    AND rp.permission_id = p.id
);


-- ==========================================
-- ADMIN permissions
-- ADMIN can manage users
-- ADMIN cannot manage roles
-- ==========================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
ON p.code IN (
    'USER_READ',
    'USER_WRITE',
    'USER_CREATE',
    'PRODUCT_READ',
    'PRODUCT_WRITE',
    'ORDER_READ',
    'ORDER_WRITE',
    'ADMIN_ACCESS'
)
WHERE r.name = 'ADMIN'
AND NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = r.id
    AND rp.permission_id = p.id
);


-- ==========================================
-- USER permissions
-- ==========================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
ON p.code IN (
    'USER_READ',
    'PRODUCT_READ',
    'ORDER_READ'
)
WHERE r.name = 'USER'
AND NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = r.id
    AND rp.permission_id = p.id
);