package com.acme.order.rest.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * UNIT TEST: the consumer-owned DTO (§3.7) — the JSON-B binding target of the §3.2
 * REST client. Mirrored from the provider's OpenAPI document, NOT the wsconsume stub.
 */
class CustomerDTOTest {

    @Test
    void carriesAllFields() {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(42);
        dto.setName("Customer 42");
        dto.setEmail("customer42@example.com");
        assertEquals(42, dto.getId());
        assertEquals("Customer 42", dto.getName());
        assertEquals("customer42@example.com", dto.getEmail());
    }

    @Test
    void defaultsAreUnset() {
        CustomerDTO dto = new CustomerDTO();
        assertEquals(0, dto.getId());
        assertNull(dto.getName());
        assertNull(dto.getEmail());
    }
}
