package com.vetautet.app.presentation.rest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderKeysetPageResponse {
    private List<OrderResponse> content;
    private String nextCursor;
    private String prevCursor;
    private boolean hasNext;
    private boolean hasPrevious;
}
