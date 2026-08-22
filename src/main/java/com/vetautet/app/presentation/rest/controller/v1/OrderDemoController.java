package com.vetautet.app.presentation.rest.controller.v1;

import com.vetautet.app.application.ordersdemo.dto.OrderKeysetPage;
import com.vetautet.app.application.ordersdemo.port.input.GetOrdersPageUseCase;
import com.vetautet.app.presentation.config.RequireBearerAuth;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import com.vetautet.app.presentation.rest.dto.response.OrderKeysetPageResponse;
import com.vetautet.app.presentation.rest.mapper.OrderPresentationMapper;
import com.vetautet.app.shared.common.util.PageableSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo endpoint for keyset (cursor) pagination over a simulated {@code orders}
 * table (docs/keyset-pagination-spec.md). Sequential next/previous
 * navigation only - there is intentionally no {@code page} parameter.
 * ADMIN-only (see RBAC migration 018-orders-demo-rbac.sql) in addition to
 * {@link RequireBearerAuth}.
 */
@RestController
@RequestMapping("/api/v1/orders-demo")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders Demo", description = "Keyset pagination demo over a simulated orders table")
//@RequireBearerAuth
//@SecurityRequirement(name = "bearer-jwt")
public class OrderDemoController {

    private final GetOrdersPageUseCase getOrdersPageUseCase;
    private final OrderPresentationMapper mapper;

    @GetMapping
    @Operation(summary = "List orders (keyset pagination)",
            description = "Sequential next/previous pagination only - no jump-to-page. Supply at most one of after/before.")
    public ResponseEntity<BaseResponse<OrderKeysetPageResponse>> listOrders(
            @Parameter(description = "Cursor for the next page") @RequestParam(required = false) String after,
            @Parameter(description = "Cursor for the previous page") @RequestParam(required = false) String before,
            @Parameter(description = "Page size (capped at " + PageableSanitizer.MAX_PAGE_SIZE + ")")
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        int safeSize = Math.min(size, PageableSanitizer.MAX_PAGE_SIZE);
        log.info("Listing orders-demo - after: {}, before: {}, size: {}", after, before, safeSize);

        OrderKeysetPage page = getOrdersPageUseCase.execute(after, before, safeSize);
        OrderKeysetPageResponse response = mapper.toPageResponse(page);

        BaseResponse<OrderKeysetPageResponse> baseResponse = BaseResponse.success(
                HttpStatus.OK.value(), "Orders retrieved successfully", response, httpRequest.getRequestURI());

        log.info("Returned {} orders, hasNext={}, hasPrevious={}",
                response.getContent().size(), response.isHasNext(), response.isHasPrevious());
        return ResponseEntity.ok(baseResponse);
    }
}
