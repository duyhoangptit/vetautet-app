package com.vetautet.app.application.ordersdemo.dto;

import com.vetautet.app.domain.ordersdemo.model.Order;
import lombok.Builder;

import java.util.List;

@Builder
public record OrderKeysetPage(
        List<Order> content,
        String nextCursor,
        String prevCursor,
        boolean hasNext,
        boolean hasPrevious) {
}
