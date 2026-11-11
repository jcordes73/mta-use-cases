package com.acme.order.soap;

import jakarta.xml.ws.WebFault;

/**
 * wsconsume-generated fault wrapper, simplified: the provider's wsdl:fault becomes this
 * checked exception on the consumer side, carrying the fault bean (getFaultInfo) that
 * wsconsume derives from the fault's schema type (§2.6: WSDL-driven stubs).
 */
@WebFault(name = "CustomerNotFoundFault", targetNamespace = "http://acme.com/customer")
public class CustomerNotFoundException_Exception extends Exception {

    private static final long serialVersionUID = 1L;

    private final CustomerNotFoundFault faultInfo;

    public CustomerNotFoundException_Exception(String message, CustomerNotFoundFault faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public CustomerNotFoundFault getFaultInfo() {
        return faultInfo;
    }
}
