package com.acme.order.rest.exception;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

/**
 * Client-side error translation (§3.2, §5.3): a 404 from the REST path is turned back
 * into the consumer-owned CustomerNotFoundException. This is the MICROPROFILE
 * interface — the server-side jakarta.ws.rs.ext.ExceptionMapper on the provider has
 * no effect here.
 */
public class CustomerNotFoundResponseExceptionMapper
        implements ResponseExceptionMapper<CustomerNotFoundException> {

    @Override
    public CustomerNotFoundException toThrowable(Response response) {
        return new CustomerNotFoundException(extractCustomerId(response));
    }

    @Override
    public boolean handles(int status, MultivaluedMap<String, Object> headers) {
        return status == Response.Status.NOT_FOUND.getStatusCode();
    }

    private long extractCustomerId(Response response) {
        Object id = response.getHeaders().getFirst("X-Customer-Id");
        return id instanceof Number n ? n.longValue() : -1L;
    }
}
