package com.acme.order.rest.exception;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UNIT TEST (pure JVM — no server): the client-side error translation (§3.2, §5.3).
 * The integration tests (RestClientIntegrationTest) exercise this mapper through the
 * live provider; these tests pin the branch behavior itself. The generated SOAP stubs
 * (com.acme.order.soap.*) are excluded from the coverage measurement — generated code
 * is verified by the integration tests against the live WSDL, not by unit tests.
 */
class CustomerNotFoundResponseExceptionMapperTest {

    private final CustomerNotFoundResponseExceptionMapper mapper =
            new CustomerNotFoundResponseExceptionMapper();

    /** The mapper selects ONLY the 404 (§3.2) — every other status is untouched. */
    @Test
    void handlesOnlyNotFound() {
        assertTrue(mapper.handles(404, new MultivaluedHashMap<>()));
        assertFalse(mapper.handles(200, new MultivaluedHashMap<>()));
        assertFalse(mapper.handles(400, new MultivaluedHashMap<>()));
        assertFalse(mapper.handles(413, new MultivaluedHashMap<>()));
        assertFalse(mapper.handles(500, new MultivaluedHashMap<>()));
    }

    /** The id comes from the machine-readable X-Customer-Id header (§5.3). */
    @Test
    void toThrowableExtractsCustomerIdFromHeader() {
        Response response = Response.status(404).header("X-Customer-Id", 42L).build();
        CustomerNotFoundException e = mapper.toThrowable(response);
        assertEquals(42, e.getCustomerId());
        assertEquals("No customer with id 42", e.getMessage());
    }

    /** Without the header the mapper still raises the exception (defensive id -1). */
    @Test
    void toThrowableFallsBackToMinusOneWithoutHeader() {
        Response response = Response.status(404).build();
        CustomerNotFoundException e = mapper.toThrowable(response);
        assertEquals(-1, e.getCustomerId());
        assertEquals("No customer with id -1", e.getMessage());
    }
}
