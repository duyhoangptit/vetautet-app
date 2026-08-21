package com.vetautet.app.infrastructure.persistence.jpa.repository;

import com.vetautet.app.infrastructure.persistence.jpa.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Native (not Spring Data derived / not {@code Pageable}-based) keyset
 * queries over {@code orders}, keyed on {@code id} (UUIDv7) alone - see
 * docs/superpowers/specs/2026-08-21-orders-keyset-pagination-demo-design.md
 * for why no {@code created_at} tie-breaker or extra composite index is
 * needed. All three queries are pure primary-key range scans.
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    @Query(value = "SELECT * FROM orders ORDER BY id DESC LIMIT :limit", nativeQuery = true)
    List<OrderJpaEntity> findFirstPage(@Param("limit") int limit);

    @Query(value = "SELECT * FROM orders WHERE id < :cursorId ORDER BY id DESC LIMIT :limit", nativeQuery = true)
    List<OrderJpaEntity> findNextPage(@Param("cursorId") UUID cursorId, @Param("limit") int limit);

    @Query(value = "SELECT * FROM orders WHERE id > :cursorId ORDER BY id ASC LIMIT :limit", nativeQuery = true)
    List<OrderJpaEntity> findPrevPage(@Param("cursorId") UUID cursorId, @Param("limit") int limit);
}
