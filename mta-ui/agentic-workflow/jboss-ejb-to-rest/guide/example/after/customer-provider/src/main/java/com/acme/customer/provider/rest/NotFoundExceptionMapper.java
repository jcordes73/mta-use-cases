package com.acme.customer.provider.rest;

import com.acme.customer.api.CustomerNotFoundException;
import jakarta.json.Json;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Server-side error translation (§2.2, §5.3): the JAX-WS fault becomes a 404 with a
 * consistent JSON error body. The SOAP endpoint keeps throwing the WebFault for its
 * existing SOAP clients — only the NEW REST path gets the JSON error contract.
 */
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<CustomerNotFoundException> {

    @Override
    public Response toResponse(CustomerNotFoundException e) {
        var body = Json.createObjectBuilder()
                .add("error", "CUSTOMER_NOT_FOUND")
                .add("message", "No customer with id " + e.getCustomerId())
                .add("customerId", e.getCustomerId())
                .build();
        return Response.status(Response.Status.NOT_FOUND)
                .header("X-Customer-Id", e.getCustomerId()) // machine-readable id for clients (§5.3)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
