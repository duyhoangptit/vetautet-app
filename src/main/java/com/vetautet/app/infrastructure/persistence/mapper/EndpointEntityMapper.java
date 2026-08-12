package com.vetautet.app.infrastructure.persistence.mapper;

import com.vetautet.app.application.auth.dto.EndpointDto;
import com.vetautet.app.domain.user.model.Endpoint;
import org.springframework.stereotype.Component;

@Component
public class EndpointEntityMapper {

    public Endpoint toDomain(EndpointDto entity) {
        if (entity == null) {
            return null;
        }
        return Endpoint.builder()
                .urlPattern(entity.urlPattern())
                .httpMethod(entity.httpMethod())
                .build();
    }
}
