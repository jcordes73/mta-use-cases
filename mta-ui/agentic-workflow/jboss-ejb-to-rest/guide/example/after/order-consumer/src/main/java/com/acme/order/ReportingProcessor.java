package com.acme.order;

import com.acme.order.rest.CustomerServiceClient;
import com.acme.order.rest.dto.CustomerDTO;
import com.acme.order.rest.exception.CustomerNotFoundException;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * AFTER: a NEW processor whose call sites go on REST from the start (§2.6/§3.2) —
 * no shared jar, no WSDL: the OpenAPI document is the contract. The EXISTING
 * processors (OrderProcessor: remote EJB + SOAP call sites) stay untouched until
 * the §4.2 per-call-site cutover and the §4.4 audit decide their migration.
 */
@Stateless
public class ReportingProcessor {

    // NEW call site — goes on REST from the start
    @Inject
    private CustomerServiceClient restCustomerService;

    public void reportCustomerCounts(long customerId) {
        CustomerDTO customer;
        try {
            customer = restCustomerService.findCustomer(customerId);
        } catch (CustomerNotFoundException e) {
            customer = null;
        }
        // ... build the report for `customer`
    }
}
