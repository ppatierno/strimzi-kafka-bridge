/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.strimzi.kafka.bridge.quarkus;

import io.strimzi.kafka.bridge.http.HttpOpenApiOperations;
import io.strimzi.kafka.bridge.http.converter.JsonUtils;
import io.vertx.core.http.HttpServerRequest;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

/**
 * Hosts request and response filters used for logging
 */
public class RestLoggingFilter {

    @Inject
    RestLoggers loggers;

    @ServerRequestFilter
    public void requestFilter(ContainerRequestContext requestContext, SimpleResourceInfo resourceInfo, HttpServerRequest httpServerRequest) {
        // resourceInfo is null if there is no match with one of the available declared REST endpoints
        if (resourceInfo != null) {
            Logger logger = loggers.get(resourceInfo.getMethodName());

            if (logger != null) {
                String requestLogHeader = this.requestLogHeader(requestContext, resourceInfo);
                logger.infof("%s Request: from %s, method = %s, path = %s",
                        requestLogHeader, httpServerRequest.remoteAddress(), // TODO: do we want HttpServerRequest (from Vert.x) just for this??
                        requestContext.getMethod(),
                        requestContext.getUriInfo().getPath());
                logger.debugf("%s Request: headers = %s", requestLogHeader, requestContext.getHeaders());
            }
        }
    }

    @ServerResponseFilter
    public void responseFilter(ContainerRequestContext requestContext, ContainerResponseContext responseContext, SimpleResourceInfo resourceInfo) {
        // resourceInfo is null if there is no match with one of the available declared REST endpoints
        if (resourceInfo != null) {
            Logger logger = loggers.get(resourceInfo.getMethodName());

            if (logger != null) {
                String requestLogHeader = this.requestLogHeader(requestContext, resourceInfo);
                logger.infof("%s Response: statusCode = %s, message = %s",
                        requestLogHeader, responseContext.getStatusInfo().getStatusCode(),
                        responseContext.getStatusInfo().getReasonPhrase());
                logger.debugf("%s Response: headers = %s", requestLogHeader, responseContext.getHeaders());
                // excluding the "metrics" endpoint because it doesn't return JSON but text/plain Prometheus format
                if (responseContext.getEntity() != null && !HttpOpenApiOperations.METRICS.toString().equals(resourceInfo.getMethodName())) {
                    byte[] body = (byte[]) responseContext.getEntity();
                    logger.debugf("%s Response: body = %s", requestLogHeader, JsonUtils.bytesToJson(body));
                }
            }
        }
    }

    private String requestLogHeader(ContainerRequestContext requestContext, SimpleResourceInfo resourceInfo) {
        int requestId;
        if (requestContext.getProperty("request-id") != null) {
            requestId = (Integer) requestContext.getProperty("request-id");
        } else {
            requestId = System.identityHashCode(requestContext.getRequest());
            requestContext.setProperty("request-id", requestId);
        }
        return String.format("[%s] %s", requestId, resourceInfo.getMethodName());
    }
}
