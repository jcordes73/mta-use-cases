package com.acme.order.soap;

import jakarta.xml.ws.WebFault;

/**
 * wsconsume-generated fault wrapper, simplified: the provider's wsdl:fault becomes this
 * checked exception on the consumer side.
 */
@WebFault(name = "CustomerNotFoundFault", targetNamespace = "http://acme.com/customer")
public class CustomerNotFoundException_Exception extends Exception {

    private static final long serialVersionUID = 1L;

    public CustomerNotFoundException_Exception(String message) {
        super(message);
    }
}
