package com.vetautet.app.application.ordersdemo.usecase;

import com.vetautet.app.application.ordersdemo.dto.OrderCursor;
import com.vetautet.app.application.ordersdemo.dto.OrderKeysetPage;
import com.vetautet.app.application.ordersdemo.port.input.GetOrdersPageUseCase;
import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.repository.OrderRepository;
import com.vetautet.app.shared.common.exception.AppLogicException;
import com.vetautet.app.shared.common.exception.ErrorCode;
import com.vetautet.app.shared.common.util.PageableSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetOrdersPageUseCaseImpl implements GetOrdersPageUseCase {

    private final OrderRepository orderRepository;

    @Override
    public OrderKeysetPage execute(String afterCursor, String beforeCursor, int size) {
        boolean hasAfter = StringUtils.hasText(afterCursor);
        boolean hasBefore = StringUtils.hasText(beforeCursor);
        if (hasAfter && hasBefore) {
            throw new AppLogicException(ErrorCode.INVALID_ORDER_CURSOR,
                    "after and before cannot both be supplied");
        }

        int cappedSize = Math.min(size, PageableSanitizer.MAX_PAGE_SIZE);
        int probeLimit = cappedSize + 1;

        if (hasAfter) {
            UUID cursorId = OrderCursor.decode(afterCursor).id();
            List<Order> rows = orderRepository.findNextPage(cursorId, probeLimit);
            boolean hasNext = rows.size() > cappedSize;
            List<Order> content = trim(rows, cappedSize, hasNext);
            return buildPage(content, hasNext, true);
        }

        if (hasBefore) {
            UUID cursorId = OrderCursor.decode(beforeCursor).id();
            List<Order> ascendingRows = orderRepository.findPrevPage(cursorId, probeLimit);
            boolean hasPrevious = ascendingRows.size() > cappedSize;
            List<Order> trimmedAscending = trim(ascendingRows, cappedSize, hasPrevious);
            List<Order> content = new ArrayList<>(trimmedAscending);
            Collections.reverse(content);
            return buildPage(content, true, hasPrevious);
        }

        List<Order> rows = orderRepository.findFirstPage(probeLimit);
        boolean hasNext = rows.size() > cappedSize;
        List<Order> content = trim(rows, cappedSize, hasNext);
        return buildPage(content, hasNext, false);
    }

    private List<Order> trim(List<Order> rows, int cappedSize, boolean hasExtra) {
        return hasExtra ? rows.subList(0, cappedSize) : rows;
    }

    private OrderKeysetPage buildPage(List<Order> content, boolean hasNext, boolean hasPrevious) {
        String nextCursor = (hasNext && !content.isEmpty())
                ? new OrderCursor(content.get(content.size() - 1).getId()).encode()
                : null;
        String prevCursor = (hasPrevious && !content.isEmpty())
                ? new OrderCursor(content.get(0).getId()).encode()
                : null;

        return OrderKeysetPage.builder()
                .content(content)
                .nextCursor(nextCursor)
                .prevCursor(prevCursor)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }
}
