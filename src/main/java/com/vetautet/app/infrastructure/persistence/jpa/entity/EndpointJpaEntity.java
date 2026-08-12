package com.vetautet.app.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "endpoints", uniqueConstraints = {
    @UniqueConstraint(name = "uq_method_url", columnNames = {"http_method", "url_pattern"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EndpointJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID endpointId;

    @Column(name = "http_method", length = 10, nullable = false)
    private String httpMethod; // GET, POST, PUT, DELETE, *

    @Column(name = "url_pattern", length = 255, nullable = false)
    private String urlPattern; // Ví dụ: /api/v1/orders/**

    @Column(name = "description")
    private String description;

    @ManyToMany(mappedBy = "endpoints", fetch = FetchType.LAZY)
    private Set<PermissionJpaEntity> permissions = new HashSet<>();
}
