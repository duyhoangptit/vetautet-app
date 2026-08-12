package com.vetautet.app.application.auth.port.output;


import com.vetautet.app.application.auth.dto.EndpointDto;

import java.util.Collection;
import java.util.List;

public interface AuthorizationQuery {
    List<EndpointDto> getAllowedEndpoints(String portalCode, Collection<String> roles);
}
