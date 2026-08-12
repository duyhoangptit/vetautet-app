package com.vetautet.app.application.ticketing.port.input;

import com.vetautet.app.application.ticketing.dto.DepartureAvailabilityDto;
import com.vetautet.app.application.ticketing.dto.SearchDepartureQuery;

import java.util.List;

public interface SearchDepartureUseCase {

    List<DepartureAvailabilityDto> execute(SearchDepartureQuery query);
}