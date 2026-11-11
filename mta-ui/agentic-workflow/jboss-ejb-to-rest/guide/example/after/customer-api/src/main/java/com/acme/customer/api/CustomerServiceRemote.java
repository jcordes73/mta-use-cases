package com.acme.customer.api;

import jakarta.ejb.Remote;

/**
 * The remote EJB view — a SECOND consumer-facing view alongside the SOAP endpoint
 * (common real-world shape: the same bean exposed both ways). This is the
 * compile-time coupling: consumers calling this view lock to Java and matching
 * JBoss EJB client library versions. KEPT in the after/ state (§4.4).
 */
@Remote
public interface CustomerServiceRemote {

    CustomerDTO findCustomer(long id) throws CustomerNotFoundException;
}
