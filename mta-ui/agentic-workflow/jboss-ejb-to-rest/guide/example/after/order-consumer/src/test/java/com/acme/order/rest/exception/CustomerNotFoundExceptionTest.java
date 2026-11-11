package com.acme.order.rest.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UNIT TEST: the consumer-owned checked exception (§3.2) — new call sites depend only
 * on this consumer-side type, NOT on any provider class (§2.6).
 */
class CustomerNotFoundExceptionTest {

    @Test
    void carriesTheCustomerId() {
        CustomerNotFoundException e = new CustomerNotFoundException(42);
        assertEquals(42, e.getCustomerId());
        assertEquals("No customer with id 42", e.getMessage());
    }

    /** Negative ids (the error-contract probe of the baseline tests, §4.5) are carried too. */
    @Test
    void negativeIdsAreCarriedToo() {
        CustomerNotFoundException e = new CustomerNotFoundException(-1);
        assertEquals(-1, e.getCustomerId());
        assertEquals("No customer with id -1", e.getMessage());
    }
}
