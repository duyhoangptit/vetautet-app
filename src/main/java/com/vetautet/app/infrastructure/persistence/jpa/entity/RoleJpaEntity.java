package com.vetautet.app.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles", uniqueConstraints = {
    @UniqueConstraint(name = "uq_portal_role_name", columnNames = {"portal_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portal_id", nullable = false)
    private PortalJpaEntity portal;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    // Liên kết với bảng users sẵn có của bạn bằng cấu trúc Many-to-Many
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id") // user_id kiểu UUID trỏ tới bảng users của bạn
    )
    private Set<UserJpaEntity> users = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<PermissionJpaEntity> permissions = new HashSet<>();
}
