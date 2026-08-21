--liquibase formatted sql

--changeset vetautet-dev:018-orders-demo-rbac
--comment: Register GET /api/v1/orders-demo as ADMIN-only via RBAC (see 014-rbac-master-data.sql for the base seed pattern, and docs/superpowers/specs/2026-08-21-orders-keyset-pagination-demo-design.md for why this endpoint stays admin-only rather than open to the USER role)
INSERT INTO permissions (id, code, name, description, created_date, last_modified_date, created_by, last_modified_by, version)
VALUES ('90000000-0000-0000-0000-000000001501', 'ordersdemo:orders:list', 'List demo orders', 'List orders from the orders-demo keyset pagination table', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    last_modified_date = NOW(),
    last_modified_by = 'SYSTEM';

INSERT INTO endpoints (id, http_method, url_pattern, description, created_date, last_modified_date, created_by, last_modified_by, version)
VALUES ('90000000-0000-0000-0000-000000002501', 'GET', '/api/v1/orders-demo', 'OrderDemoController.listOrders', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0)
ON CONFLICT (http_method, url_pattern) DO UPDATE
SET description = EXCLUDED.description,
    last_modified_date = NOW(),
    last_modified_by = 'SYSTEM';

INSERT INTO permission_endpoints (permission_id, endpoint_id)
SELECT permission.id, endpoint.id
FROM permissions permission
JOIN endpoints endpoint ON endpoint.http_method = 'GET' AND endpoint.url_pattern = '/api/v1/orders-demo'
WHERE permission.code = 'ordersdemo:orders:list'
ON CONFLICT (permission_id, endpoint_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN portals portal ON portal.id = role.portal_id
JOIN permissions permission ON permission.code = 'ordersdemo:orders:list'
WHERE portal.code = 'A'
  AND role.name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

--rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'ordersdemo:orders:list');
--rollback DELETE FROM permission_endpoints WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'ordersdemo:orders:list');
--rollback DELETE FROM endpoints WHERE (http_method, url_pattern) = ('GET', '/api/v1/orders-demo');
--rollback DELETE FROM permissions WHERE code = 'ordersdemo:orders:list';
