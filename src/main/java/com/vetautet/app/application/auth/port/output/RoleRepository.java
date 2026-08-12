package com.vetautet.app.application.auth.port.output;

import java.util.List;
import java.util.UUID;

public interface RoleRepository {
    /**
     * Lấy danh sách tên các Role của người dùng dựa trên UserId và PortalCode.
     */
    List<String> findRoleNamesByUserIdAndPortalCode(UUID userId, String portalCode);
}
