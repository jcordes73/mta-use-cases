package com.acme.customer.provider.rest;

import jakarta.json.Json;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

/**
 * NEW code — replaces strict XSD enforcement (§2.6, translatable design work): Bean
 * Validation violations become a 400 with the same JSON error body shape as §5.3.
 * Unlike XSD (reject-before-processing), Bean Validation runs in-application, so the
 * decision to fail fast is made per endpoint via @Valid on the resource method.
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException e) {
        List<String> violations = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .toList();
        var body = Json.createObjectBuilder()
                .add("error", "VALIDATION_FAILED")
                .add("violations", Json.createArrayBuilder(violations))
                .build();
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
