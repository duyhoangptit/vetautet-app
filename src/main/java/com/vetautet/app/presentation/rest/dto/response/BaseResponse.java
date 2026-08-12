
package com.vetautet.app.presentation.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
* Base response structure for all API responses
* Provides consistent response format across the application
*
* @param <T> the type of the payload
*/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    /**
     * Timestamp when the response was generated
     */
    private Instant timestamp;

    /**
     * HTTP status code
     */
    private int status;

    /**
     * HTTP status reason phrase (e.g., "OK", "Not Found")
     */
    private String error;

    /**
     * Application-specific error code (e.g., "ERR-1002")
     * Only present in error responses
     */
    private String errorCode;

    /**
     * Human-readable message describing the response
     */
    private String message;

    /**
     * Request path that generated this response
     */
    private String path;

    /**
     * Response payload data (generic type)
     * Can be null for error responses or responses without data
     */
    private T payload;

    /**
     * Create a success response with payload
     *
     * @param status  HTTP status code
     * @param message success message
     * @param payload response data
     * @param path    request path
     * @param <T>     type of payload
     * @return BaseResponse with success data
     */
    public static <T> BaseResponse<T> success(int status, String message, T payload, String path) {
        return BaseResponse.<T>builder()
                .timestamp(Instant.now())
                .status(status)
                .message(message)
                .payload(payload)
                .path(path)
                .build();
    }

    /**
     * Create a success response with payload (200 OK)
     *
     * @param payload response data
     * @param <T>     type of payload
     * @return BaseResponse with success data
     */
    public static <T> BaseResponse<T> success(T payload) {
        return BaseResponse.<T>builder()
                .timestamp(Instant.now())
                .status(200)
                .message("Success")
                .payload(payload)
                .build();
    }

    /**
     * Create a success response without payload
     *
     * @param status  HTTP status code
     * @param message success message
     * @param path    request path
     * @param <T>     type of payload
     * @return BaseResponse without data
     */
    public static <T> BaseResponse<T> success(int status, String message, String path) {
        return BaseResponse.<T>builder()
                .timestamp(Instant.now())
                .status(status)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Create an error response
     *
     * @param status    HTTP status code
     * @param error     HTTP status reason phrase
     * @param errorCode application error code
     * @param message   error message
     * @param path      request path
     * @param <T>       type of payload
     * @return BaseResponse with error information
     */
    public static <T> BaseResponse<T> error(int status, String error, String errorCode,
                                            String message, String path) {
        return BaseResponse.<T>builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .build();
    }
}