package com.vetautet.app.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "portals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortalJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID portalId;

    @Column(name = "code", length = 50, unique = true, nullable = false)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "enabled")
    private boolean enabled = true;

    @OneToMany(mappedBy = "portal", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RoleJpaEntity> roles = new HashSet<>();
}
