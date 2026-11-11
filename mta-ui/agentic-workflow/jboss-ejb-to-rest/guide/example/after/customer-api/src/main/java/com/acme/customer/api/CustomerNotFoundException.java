package com.acme.customer.api;

import jakarta.xml.ws.WebFault;

/**
 * Checked exception serving two contracts: a SOAP fault on the @WebService endpoint
 * (@WebFault) and a checked exception over EJB remoting.
 */
@WebFault(name = "CustomerNotFoundFault")
public class CustomerNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    private final long customerId;

    public CustomerNotFoundException(long customerId) {
        super("No customer with id " + customerId);
        this.customerId = customerId;
    }

    public long getCustomerId() {
        return customerId;
    }
}
