--liquibase formatted sql

-- ============================================================
-- changeset vetautet:014-rbac-master-data
-- comment: Seed RBAC portal, roles, permissions, endpoints, and mappings for v1 REST controllers
-- ============================================================

INSERT INTO portals (id, code, name, enabled, created_date, last_modified_date, created_by, last_modified_by, version)
VALUES ('90000000-0000-0000-0000-000000000001', 'A', 'Default Portal', TRUE, NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    enabled = EXCLUDED.enabled,
    last_modified_date = NOW(),
    last_modified_by = 'SYSTEM';

INSERT INTO roles (id, portal_id, name, description, created_date, last_modified_date, created_by, last_modified_by, version)
SELECT role_seed.id,
       portal.id,
       role_seed.name,
       role_seed.description,
       NOW(),
       NOW(),
       'SYSTEM',
       'SYSTEM',
       0
FROM portals portal
JOIN (VALUES
    ('90000000-0000-0000-0000-000000000101'::uuid, 'ADMIN', 'Administrator role with all v1 permissions'),
    ('90000000-0000-0000-0000-000000000102'::uuid, 'USER', 'Authenticated customer role for booking and payment flows')
) AS role_seed(id, name, description) ON TRUE
WHERE portal.code = 'A'
ON CONFLICT (portal_id, name) DO UPDATE
SET description = EXCLUDED.description,
    last_modified_date = NOW(),
    last_modified_by = 'SYSTEM';

INSERT INTO permissions (id, code, name, description, created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('90000000-0000-0000-0000-000000001001', 'auth:register:create', 'Register user account', 'Register a new user account', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000001002', 'auth:login:create', 'Login user', 'Authenticate user credentials and create OTP login challenge', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000001003', 'auth:otp-login:verify', 'Verify login OTP', 'Verify OTP for login and issue JWT token', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000001004', 'auth:account:activate', 'Activate account', 'Send or verify OTP to activate a registered account', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000001005', 'auth:password:forgot', 'Forgot password', 'Create a password reset link for the user', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000001006', 'auth:password:reset', 'Reset password', 'Send OTP or verify OTP to complete password reset', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000001007', 'auth:logout:create', 'Logout user', 'Logout current JWT session', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000001101', 'user:create', 'Create user', 'Create a new user in the system', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000001102', 'user:update', 'Update user', 'Update an existing user by ID', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000001103', 'user:read', 'Read user', 'Retrieve a user by ID', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000001104', 'user:list', 'List users', 'Retrieve a paginated list of users', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000001105', 'user:search', 'Search users', 'Search users by keyword', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000001106', 'user:delete', 'Delete user', 'Delete a user by ID', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000001201', 'booking:hold:create', 'Hold booking', 'Hold seat inventory and create booking order', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000001301', 'payment:confirm:create', 'Confirm payment', 'Confirm payment transaction and finalize booking inventory', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000001401', 'departure:search:read', 'Search departures', 'Search open departures and seat availability', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    last_modified_date = NOW(),
    last_modified_by = 'SYSTEM';

INSERT INTO endpoints (id, http_method, url_pattern, description, created_date, last_modified_date, created_by, last_modified_by, version)
VALUES
    ('90000000-0000-0000-0000-000000002001', 'POST', '/api/v1/auth/register', 'AuthController.register', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000002002', 'POST', '/api/v1/auth/login', 'AuthController.login', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000002003', 'POST', '/api/v1/auth/verify-otp-login', 'AuthController.verifyOtpLogin', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000002004', 'POST', '/api/v1/auth/register/activate', 'AuthController.activateAccount', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000002005', 'POST', '/api/v1/auth/forgot-password', 'AuthController.forgotPassword', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000002006', 'POST', '/api/v1/auth/reset-password', 'AuthController.resetPassword', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000002007', 'POST', '/api/v1/auth/logout', 'AuthController.logout', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000002101', 'POST', '/api/v1/users', 'UserController.createUser', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000002102', 'PUT', '/api/v1/users/*', 'UserController.updateUser', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000002103', 'GET', '/api/v1/users/*', 'UserController.getUserById', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000002104', 'GET', '/api/v1/users', 'UserController.listUsers', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000002105', 'GET', '/api/v1/users/search', 'UserController.searchUsers', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000002106', 'DELETE', '/api/v1/users/*', 'UserController.deleteUser', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000002201', 'POST', '/api/v1/bookings/hold', 'BookingController.holdBooking', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000002301', 'POST', '/api/v1/payments/confirm', 'PaymentController.confirmPayment', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0),
    ('90000000-0000-0000-0000-000000002401', 'GET', '/api/v1/departures/search', 'DepartureController.search', NOW(), NOW(), 'SYSTEM', 'SYSTEM', 0)
ON CONFLICT (http_method, url_pattern) DO UPDATE
SET description = EXCLUDED.description,
    last_modified_date = NOW(),
    last_modified_by = 'SYSTEM';

INSERT INTO permission_endpoints (permission_id, endpoint_id)
SELECT permission.id, endpoint.id
FROM (VALUES
    ('auth:register:create', 'POST', '/api/v1/auth/register'),
    ('auth:login:create', 'POST', '/api/v1/auth/login'),
    ('auth:otp-login:verify', 'POST', '/api/v1/auth/verify-otp-login'),
    ('auth:account:activate', 'POST', '/api/v1/auth/register/activate'),
    ('auth:password:forgot', 'POST', '/api/v1/auth/forgot-password'),
    ('auth:password:reset', 'POST', '/api/v1/auth/reset-password'),
    ('auth:logout:create', 'POST', '/api/v1/auth/logout'),
    ('user:create', 'POST', '/api/v1/users'),
    ('user:update', 'PUT', '/api/v1/users/*'),
    ('user:read', 'GET', '/api/v1/users/*'),
    ('user:list', 'GET', '/api/v1/users'),
    ('user:search', 'GET', '/api/v1/users/search'),
    ('user:delete', 'DELETE', '/api/v1/users/*'),
    ('booking:hold:create', 'POST', '/api/v1/bookings/hold'),
    ('payment:confirm:create', 'POST', '/api/v1/payments/confirm'),
    ('departure:search:read', 'GET', '/api/v1/departures/search')
) AS mapping(permission_code, http_method, url_pattern)
JOIN permissions permission ON permission.code = mapping.permission_code
JOIN endpoints endpoint ON endpoint.http_method = mapping.http_method AND endpoint.url_pattern = mapping.url_pattern
ON CONFLICT (permission_id, endpoint_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN portals portal ON portal.id = role.portal_id
JOIN permissions permission ON TRUE
WHERE portal.code = 'A'
  AND role.name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN portals portal ON portal.id = role.portal_id
JOIN permissions permission ON permission.code IN (
    'auth:logout:create',
    'booking:hold:create',
    'payment:confirm:create',
    'departure:search:read'
)
WHERE portal.code = 'A'
  AND role.name = 'USER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code IN ('auth:register:create','auth:login:create','auth:otp-login:verify','auth:account:activate','auth:password:forgot','auth:password:reset','auth:logout:create','user:create','user:update','user:read','user:list','user:search','user:delete','booking:hold:create','payment:confirm:create','departure:search:read'));
-- rollback DELETE FROM permission_endpoints WHERE permission_id IN (SELECT id FROM permissions WHERE code IN ('auth:register:create','auth:login:create','auth:otp-login:verify','auth:account:activate','auth:password:forgot','auth:password:reset','auth:logout:create','user:create','user:update','user:read','user:list','user:search','user:delete','booking:hold:create','payment:confirm:create','departure:search:read'));
-- rollback DELETE FROM endpoints WHERE (http_method, url_pattern) IN (('POST','/api/v1/auth/register'),('POST','/api/v1/auth/login'),('POST','/api/v1/auth/verify-otp-login'),('POST','/api/v1/auth/register/activate'),('POST','/api/v1/auth/forgot-password'),('POST','/api/v1/auth/reset-password'),('POST','/api/v1/auth/logout'),('POST','/api/v1/users'),('PUT','/api/v1/users/*'),('GET','/api/v1/users/*'),('GET','/api/v1/users'),('GET','/api/v1/users/search'),('DELETE','/api/v1/users/*'),('POST','/api/v1/bookings/hold'),('POST','/api/v1/payments/confirm'),('GET','/api/v1/departures/search'));
-- rollback DELETE FROM permissions WHERE code IN ('auth:register:create','auth:login:create','auth:otp-login:verify','auth:account:activate','auth:password:forgot','auth:password:reset','auth:logout:create','user:create','user:update','user:read','user:list','user:search','user:delete','booking:hold:create','payment:confirm:create','departure:search:read');
-- rollback DELETE FROM roles WHERE id IN ('90000000-0000-0000-0000-000000000101','90000000-0000-0000-0000-000000000102');
-- rollback DELETE FROM portals WHERE id = '90000000-0000-0000-0000-000000000001';
