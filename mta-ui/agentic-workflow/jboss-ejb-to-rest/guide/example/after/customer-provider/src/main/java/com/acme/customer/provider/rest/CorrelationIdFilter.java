package com.acme.customer.provider.rest;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.UUID;

/**
 * NEW code — replaces a SOAP handler-chain entry (§2.6, translatable design work): a
 * SOAP handler processing a message-level correlation header becomes an HTTP-level
 * filter. Header semantics differ — there is no `mustUnderstand` equivalent, so a
 * filter that must reject unknown mandatory headers enforces that itself (417/400).
 * Correlation propagation across the boundary is §5.8.
 */
@Provider
@Priority(Priorities.USER)
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String HEADER = "X-Correlation-Id";

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        String correlationId = request.getHeaderString(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        request.getHeaders().putSingle(HEADER, correlationId);
        request.setProperty(HEADER, correlationId);
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response)
            throws IOException {
        Object correlationId = request.getProperty(HEADER);
        if (correlationId != null) {
            response.getHeaders().putSingle(HEADER, correlationId);
        }
    }
}
