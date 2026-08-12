package com.vetautet.app.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID permissionId;

    @Column(name = "code", length = 100, unique = true, nullable = false)
    private String code; // Cấu trúc module:object:action

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private Set<RoleJpaEntity> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "permission_endpoints",
        joinColumns = @JoinColumn(name = "permission_id"),
        inverseJoinColumns = @JoinColumn(name = "endpoint_id")
    )
    private Set<EndpointJpaEntity> endpoints = new HashSet<>();
}
