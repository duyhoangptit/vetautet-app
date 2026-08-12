
package com.vetautet.app.presentation.rest.dto.response;

import java.time.Instant;

/**
* Success response wrapper extending BaseResponse
* Used for successful API responses with data payload
*
* @param <T> the type of the payload
*/
public class SuccessResponse<T> extends BaseResponse<T> {

    /**
     * Create a success response with payload
     *
     * @param status  HTTP status code
     * @param message success message
     * @param payload response data
     * @param path    request path
     */
    public SuccessResponse(int status, String message, T payload, String path) {
        this.setTimestamp(Instant.now());
        this.setStatus(status);
        this.setMessage(message);
        this.setPayload(payload);
        this.setPath(path);
    }

    /**
     * Create a success response with payload (200 OK)
     *
     * @param payload response data
     */
    public SuccessResponse(T payload) {
        this.setTimestamp(Instant.now());
        this.setStatus(200);
        this.setMessage("Success");
        this.setPayload(payload);
    }

    /**
     * Create a success response with custom message
     *
     * @param message success message
     * @param payload response data
     */
    public SuccessResponse(String message, T payload) {
        this.setTimestamp(Instant.now());
        this.setStatus(200);
        this.setMessage(message);
        this.setPayload(payload);
    }
}
