package com.vetautet.app.presentation.rest.exception;

import com.vetautet.app.presentation.rest.dto.response.BaseResponse;

import java.time.Instant;

/**
* Error response extending BaseResponse
* Used for error API responses
*/
public class ErrorResponse extends BaseResponse<Void> {

    /**
     * Create an error response
     *
     * @param status    HTTP status code
     * @param error     HTTP status reason phrase
     * @param errorCode application error code
     * @param message   error message
     * @param path      request path
     */
    public ErrorResponse(int status, String error, String errorCode, String message, String path) {
        this.setTimestamp(Instant.now());
        this.setStatus(status);
        this.setError(error);
        this.setErrorCode(errorCode);
        this.setMessage(message);
        this.setPath(path);
    }

    /**
     * Create an error response using builder pattern
     */
    public static ErrorResponseBuilder errorBuilder() {
        return new ErrorResponseBuilder();
    }

    /**
     * Builder class for ErrorResponse
     */
    public static class ErrorResponseBuilder {
        private int status;
        private String error;
        private String errorCode;
        private String message;
        private String path;
        private Instant timestamp;

        public ErrorResponseBuilder status(int status) {
            this.status = status;
            return this;
        }

        public ErrorResponseBuilder error(String error) {
            this.error = error;
            return this;
        }

        public ErrorResponseBuilder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorResponseBuilder path(String path) {
            this.path = path;
            return this;
        }

        public ErrorResponseBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ErrorResponse build() {
            ErrorResponse response = new ErrorResponse(status, error, errorCode, message, path);
            if (timestamp != null) {
                response.setTimestamp(timestamp);
            }
            return response;
        }
    }
}
 