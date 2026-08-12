--liquibase formatted sql

--changeset vetautet:012-inital-schema-for-rbac
--comment: Create initial schema for RBAC (Role-Based Access Control)

-- 0. Kích hoạt extension hỗ trợ sinh chuỗi UUID tự động nếu chưa có
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Tạo bảng Cổng thông tin (Portals)
CREATE TABLE portals (
                         id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         code VARCHAR(50) NOT NULL,
                         name VARCHAR(100) NOT NULL,
                         enabled BOOLEAN DEFAULT TRUE,

    -- Các trường Common Base Entity
                         created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         last_modified_date TIMESTAMP WITH TIME ZONE,
                         created_by VARCHAR(50),
                         last_modified_by VARCHAR(50),
                         version BIGINT DEFAULT 0,

                         CONSTRAINT uq_portal_code UNIQUE (code)
);

-- 2. Tạo bảng Vai trò (Roles) - Phụ thuộc vào Portal
CREATE TABLE roles (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       portal_id UUID NOT NULL,
                       name VARCHAR(50) NOT NULL,
                       description VARCHAR(255),

    -- Các trường Common Base Entity
                       created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       last_modified_date TIMESTAMP WITH TIME ZONE,
                       created_by VARCHAR(50),
                       last_modified_by VARCHAR(50),
                       version BIGINT DEFAULT 0,

                       CONSTRAINT fk_roles_portal FOREIGN KEY (portal_id) REFERENCES portals(id) ON DELETE CASCADE,
                       CONSTRAINT uq_portal_role_name UNIQUE (portal_id, name)
);

-- 3. Tạo bảng trung gian Người dùng - Vai trò (User_Roles)
-- Note: user_id liên kết trực tiếp sang bảng users sẵn có của bạn
CREATE TABLE user_roles (
                            user_id UUID NOT NULL,
                            role_id UUID NOT NULL,

                            PRIMARY KEY (user_id, role_id),
                            CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
    -- Nếu cần khóa cứng ngoại sang bảng users sẵn có, bổ sung dòng dưới:
    -- CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. Tạo bảng Quyền hạn nghiệp vụ (Permissions) - Dạng module:object:action
CREATE TABLE permissions (
                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             code VARCHAR(100) NOT NULL,
                             name VARCHAR(100) NOT NULL,
                             description VARCHAR(255),

    -- Các trường Common Base Entity
                             created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             last_modified_date TIMESTAMP WITH TIME ZONE,
                             created_by VARCHAR(50),
                             last_modified_by VARCHAR(50),
                             version BIGINT DEFAULT 0,

                             CONSTRAINT uq_permission_code UNIQUE (code)
);

-- 5. Tạo bảng trung gian Vai trò - Quyền hạn (Role_Permissions)
CREATE TABLE role_permissions (
                                  role_id UUID NOT NULL,
                                  permission_id UUID NOT NULL,

                                  PRIMARY KEY (role_id, permission_id),
                                  CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
                                  CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- 6. Tạo bảng Quản lý API kỹ thuật (Endpoints)
CREATE TABLE endpoints (
                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           http_method VARCHAR(10) NOT NULL,
                           url_pattern VARCHAR(255) NOT NULL,
                           description VARCHAR(255),

    -- Các trường Common Base Entity
                           created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           last_modified_date TIMESTAMP WITH TIME ZONE,
                           created_by VARCHAR(50),
                           last_modified_by VARCHAR(50),
                           version BIGINT DEFAULT 0,

                           CONSTRAINT uq_method_url UNIQUE (http_method, url_pattern)
);

-- 7. Tạo bảng trung gian Quyền hạn - API kỹ thuật (Permission_Endpoints)
CREATE TABLE permission_endpoints (
                                      permission_id UUID NOT NULL,
                                      endpoint_id UUID NOT NULL,

                                      PRIMARY KEY (permission_id, endpoint_id),
                                      CONSTRAINT fk_perm_end_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
                                      CONSTRAINT fk_perm_end_endpoint FOREIGN KEY (endpoint_id) REFERENCES endpoints(id) ON DELETE CASCADE
);

-- --- HỆ THỐNG INDEX TỐI ƯU TRUY VẤN ---
CREATE INDEX idx_portals_code ON portals(code);
CREATE INDEX idx_permissions_code ON permissions(code);
CREATE INDEX idx_endpoints_lookup ON endpoints(url_pattern, http_method);


--rollback DROP TABLE IF EXISTS permission_endpoints CASCADE;
--rollback DROP TABLE IF EXISTS endpoints CASCADE;
--rollback DROP TABLE IF EXISTS role_permissions CASCADE;
--rollback DROP TABLE IF EXISTS permissions CASCADE;
--rollback DROP TABLE IF EXISTS user_roles CASCADE;
--rollback DROP TABLE IF EXISTS roles CASCADE;
--rollback DROP TABLE IF EXISTS portals CASCADE;