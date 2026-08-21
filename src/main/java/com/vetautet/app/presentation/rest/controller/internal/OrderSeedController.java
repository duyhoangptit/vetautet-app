package com.vetautet.app.presentation.rest.controller.internal;

import com.vetautet.app.application.ordersdemo.port.input.SeedOrdersUseCase;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only bulk seeder for the orders-demo table. Only exists as a Spring
 * bean under the {@code dev}/{@code local} profiles - absent from the bean
 * graph (and therefore unreachable) in any other profile, including
 * production, regardless of routing/security configuration. Reachability in
 * dev/local additionally requires {@code /internal/orders-demo/**} to be in
 * {@code SecurityConfig}'s permitAll matchers (see that class) - safety here
 * comes from profile exclusion, not from auth.
 */
@RestController
@RequestMapping("/internal/orders-demo")
@RequiredArgsConstructor
@Slf4j
//@Profile({"dev", "local", "pentest"})
@Tag(name = "Orders Demo Seed", description = "Dev-only bulk seeder for the orders-demo table")
public class OrderSeedController {

    private final SeedOrdersUseCase seedOrdersUseCase;

    @PostMapping("/seed")
    @Operation(summary = "Bulk-seed synthetic orders", description = "Dev/local only. Not exposed in production.")
    public ResponseEntity<BaseResponse<Integer>> seed(
            @RequestParam(defaultValue = "2000000") int count,
            HttpServletRequest httpRequest) {
        log.info("Seeding {} synthetic orders", count);
        int inserted = seedOrdersUseCase.execute(count);

        BaseResponse<Integer> response = BaseResponse.success(
                HttpStatus.OK.value(), "Seed completed", inserted, httpRequest.getRequestURI());
        return ResponseEntity.ok(response);
    }
}
