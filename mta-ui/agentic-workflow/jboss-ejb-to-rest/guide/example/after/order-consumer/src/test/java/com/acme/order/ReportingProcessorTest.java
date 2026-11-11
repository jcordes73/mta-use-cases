package com.acme.order;

import com.acme.order.rest.CustomerServiceClient;
import com.acme.order.rest.dto.CustomerDTO;
import com.acme.order.rest.exception.CustomerNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UNIT TEST (pure JVM — no server): the NEW call site that goes on REST from the start
 * (§2.6/§3.2). A test double stands in for the MP REST Client proxy the container would
 * inject via @Inject (§3.1: the @Inject variant is XP-only; the programmatic fallback
 * the integration tests use, §3.2, reaches the same interface).
 */
class ReportingProcessorTest {

    private ReportingProcessor processor;
    private StubRestClient rest;

    /** Fake of the §3.2 client interface — the contract is the OpenAPI document. */
    private static class StubRestClient implements CustomerServiceClient {
        long lastId = -1;
        boolean fail = false;

        @Override
        public CustomerDTO findCustomer(long id) throws CustomerNotFoundException {
            lastId = id;
            if (fail) {
                throw new CustomerNotFoundException(id);
            }
            CustomerDTO dto = new CustomerDTO();
            dto.setId(id);
            return dto;
        }

        @Override
        public jakarta.ws.rs.core.Response uploadAvatar(long id, InputStream data) {
            return null; // not exercised by reportCustomerCounts
        }

        @Override
        public jakarta.ws.rs.core.Response requestWelcomePack(long id)
                throws CustomerNotFoundException {
            return null; // not exercised by reportCustomerCounts
        }
    }

    @BeforeEach
    void injectFakes() {
        processor = new ReportingProcessor();
        rest = new StubRestClient();
        FieldInjector.inject(processor, "restCustomerService", rest);
    }

    /** reportCustomerCounts goes through the REST client (§3.2). */
    @Test
    void reportConsumesTheRestCallSite() {
        assertDoesNotThrow(() -> processor.reportCustomerCounts(42));
        assertEquals(42, rest.lastId);
    }

    /** A 404 (mapped to CustomerNotFoundException, §3.2) is swallowed — customer null. */
    @Test
    void notFoundIsHandledWithoutPropagating() {
        rest.fail = true;
        assertDoesNotThrow(() -> processor.reportCustomerCounts(-1));
        assertEquals(-1, rest.lastId);
    }
}
