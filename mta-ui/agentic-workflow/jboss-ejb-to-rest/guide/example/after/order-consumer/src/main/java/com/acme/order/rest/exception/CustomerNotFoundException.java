package com.acme.order.rest.exception;

/**
 * Consumer-owned checked exception raised by the REST client when the provider answers
 * 404 (CustomerNotFoundResponseExceptionMapper). New call sites depend only on this
 * consumer-side type — NOT on any provider class (there is no shared jar in the SOAP
 * variant; the contracts are the WSDL and the OpenAPI document, §2.6).
 */
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
