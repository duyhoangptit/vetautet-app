package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.repository.OrderRepository;
import com.vetautet.app.infrastructure.persistence.jpa.repository.OrderJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.OrderEntityMapper;
import com.vetautet.app.shared.common.util.PageableSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Authoritative size-cap boundary for orders-demo reads (CLAUDE.md rule 1):
 * every {@code limit} is clamped here regardless of what any upstream layer
 * passed in. Callers (see {@code GetOrdersPageUseCaseImpl}) request
 * {@code size + 1} rows as a "has more" probe, so the ceiling is
 * {@code MAX_PAGE_SIZE + 1}, not {@code MAX_PAGE_SIZE} - reusing the same
 * shared constant per CLAUDE.md rule 2 rather than a second hardcoded max.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderRepositoryAdapter implements OrderRepository {

    private static final int MAX_QUERY_LIMIT = PageableSanitizer.MAX_PAGE_SIZE + 1;

    private final OrderJpaRepository orderJpaRepository;
    private final OrderEntityMapper orderEntityMapper;

    @Override
    public List<Order> findFirstPage(int limit) {
        return orderJpaRepository.findFirstPage(cap(limit)).stream()
                .map(orderEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findNextPage(UUID afterId, int limit) {
        return orderJpaRepository.findNextPage(afterId, cap(limit)).stream()
                .map(orderEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findPrevPage(UUID beforeId, int limit) {
        return orderJpaRepository.findPrevPage(beforeId, cap(limit)).stream()
                .map(orderEntityMapper::toDomain)
                .toList();
    }

    private int cap(int limit) {
        return Math.min(limit, MAX_QUERY_LIMIT);
    }
}
