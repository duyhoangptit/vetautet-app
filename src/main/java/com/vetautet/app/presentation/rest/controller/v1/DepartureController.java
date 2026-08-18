package com.vetautet.app.presentation.rest.controller.v1;

import com.vetautet.app.application.ticketing.dto.DepartureAvailabilityDto;
import com.vetautet.app.application.ticketing.dto.SearchDepartureQuery;
import com.vetautet.app.application.ticketing.port.input.SearchDepartureUseCase;
import com.vetautet.app.presentation.config.ratelimit.RateLimit;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import com.vetautet.app.presentation.rest.dto.response.DepartureAvailabilityResponse;
import com.vetautet.app.presentation.rest.mapper.BookingFlowPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departures")
@RequiredArgsConstructor
@Tag(name = "Departure Search", description = "APIs for searching train departures and seat availability")
public class DepartureController {

    private final SearchDepartureUseCase searchDepartureUseCase;
    private final BookingFlowPresentationMapper mapper;

    @GetMapping("/search")
    @RateLimit(scope = "departureSearch")
    @Operation(summary = "Search departures", description = "Search open departures by business date and origin/destination station")
    public ResponseEntity<BaseResponse<List<DepartureAvailabilityResponse>>> search(
            @Parameter(description = "Origin station ID") @RequestParam UUID originStationId,
            @Parameter(description = "Destination station ID") @RequestParam UUID destinationStationId,
            @Parameter(description = "Business date") @RequestParam LocalDate businessDate,
            HttpServletRequest httpRequest) {
        List<DepartureAvailabilityDto> results = searchDepartureUseCase.execute(SearchDepartureQuery.builder()
                .originStationId(originStationId)
                .destinationStationId(destinationStationId)
                .businessDate(businessDate)
                .build());

        BaseResponse<List<DepartureAvailabilityResponse>> baseResponse = BaseResponse.success(
                HttpStatus.OK.value(),
                "Departures retrieved successfully",
                mapper.toDepartureResponses(results),
                httpRequest.getRequestURI());
        return ResponseEntity.ok(baseResponse);
    }
}