/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.strimzi.kafka.bridge.http;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.strimzi.kafka.bridge.BridgeContentType;
import io.strimzi.kafka.bridge.converter.JsonUtils;
import io.strimzi.kafka.bridge.http.beans.Error;

import jakarta.ws.rs.core.Response;

/**
 * Provides some utility methods for HTTP request/response
 */
public class HttpUtils {

    /**
     * Build an HTTP response
     *
     * @param statusCode the HTTP status code
     * @param contentType the content-type to set in the HTTP response
     * @param body the body to set in the HTTP response
     * @return HTTP response
     */
    public static Response buildResponse(int statusCode, String contentType, byte[] body) {
        Response.ResponseBuilder builder = Response.status(statusCode);
        if (body != null) {
            builder.header(HttpHeaderNames.CONTENT_TYPE.toString(), contentType);
            builder.header(HttpHeaderNames.CONTENT_LENGTH.toString(), String.valueOf(body.length));
            builder.entity(body);
        }
        return builder.build();
    }

    /**
     * Build an HTTP response containing a bridge error
     *
     * @param error representation of the bridge error send as HTTP response body
     * @return HTTP response describing the bridge error
     */
    public static Response buildResponseFromError(Error error) {
        return HttpUtils.buildResponse(error.getErrorCode(),
                BridgeContentType.KAFKA_JSON, JsonUtils.objectToBytes(error));
    }

    /**
     * Build an Error instance from code and message
     *
     * @param code HTTP error code
     * @param message HTTP error message
     * @return an Error instance
     */
    public static Error toError(int code, String message) {
        Error error = new Error();
        error.setErrorCode(code);
        error.setMessage(message);
        return error;
    }
}
