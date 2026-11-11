package com.acme.order.soap;

/**
 * wsconsume-generated fault bean, simplified: the wsdl:fault's payload type — the
 * data the client can inspect when the SOAP fault arrives (§2.6: WSDL-driven stubs).
 */
public class CustomerNotFoundFault {

    private long customerId;

    public CustomerNotFoundFault() {
    }

    public CustomerNotFoundFault(long customerId) {
        this.customerId = customerId;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }
}
