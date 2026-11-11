package com.acme.order;

import com.acme.customer.api.CustomerDTO;
import com.acme.customer.api.CustomerNotFoundException;
import com.acme.customer.api.CustomerServiceRemote;
import com.acme.order.soap.CustomerService;
import com.acme.order.soap.CustomerService_Service;
import com.acme.order.soap.CustomerNotFoundException_Exception;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.xml.ws.WebServiceRef;

/**
 * BEFORE: one consumer mixing BOTH couplings toward the same provider:
 *
 * - SOAP (WSDL-driven): placeOrder uses the wsconsume-style stubs via @WebServiceRef —
 *   no compile dependency on provider classes, but the WSDL is the contract.
 * - Remote EJB: lookupCustomer uses the shared customer-api jar via @EJB(lookup=...) —
 *   compile-time coupling plus the EJB client stack (jboss-ejb-client.xml, jndi.properties).
 *
 * This mixed shape is common in real systems: the same bean exposed both ways, with
 * different consumers on each. The refactoring (§2.6) adds REST for both; the SOAP
 * path and the remote view stay supported until the §4.4 audit decides retirement.
 */
@Stateless
public class OrderProcessor {

    // COUPLING 1 — SOAP: WSDL-driven stubs
    @WebServiceRef(CustomerService_Service.class)
    private CustomerService soapCustomerService;

    // COUPLING 2 — remote EJB: shared api jar + ejb: lookup
    @EJB(lookup = "ejb:customer-app/customer-provider/CustomerServiceBean!com.acme.customer.api.CustomerServiceRemote")
    private CustomerServiceRemote remoteCustomerService;

    public void placeOrder(long customerId, String article) throws CustomerNotFoundException_Exception {
        // two DTO types coexist in a mixed consumer: the wsconsume stub type (SOAP)
        // and the shared api type (remote EJB) — see lookupCustomer below
        com.acme.order.soap.CustomerDTO customer = soapCustomerService.findCustomer(customerId);
        // ... create and persist the order for `customer`
    }

    public CustomerDTO lookupCustomer(long customerId) throws CustomerNotFoundException {
        return remoteCustomerService.findCustomer(customerId);
    }
}
