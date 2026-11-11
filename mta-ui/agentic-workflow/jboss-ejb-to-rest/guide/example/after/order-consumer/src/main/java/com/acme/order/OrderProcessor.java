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
 * AFTER: the two EXISTING call paths live side by side, untouched (§2.6/§4.4):
 *
 * - The EXISTING SOAP call site (placeOrder) stays on SOAP — existing SOAP clients
 *   may not transition easily; cutover is a per-call-site decision (§4.2/§4.4).
 * - The EXISTING remote-EJB call site (lookupCustomer) stays on the remote view —
 *   same §4.4 stance; the api jar and EJB client stack remain until the audit shows
 *   no consumers left.
 *
 * NEW call sites (ReportingProcessor) use the REST client — no shared jar, no WSDL:
 * the OpenAPI document is the contract (§3.2). Over time, call sites migrate one at
 * a time (§4.2), each gated by contract tests (§4.5); all old paths stay supported
 * until the §4.4 audit decides retirement.
 */
@Stateless
public class OrderProcessor {

    // EXISTING coupling 1 — SOAP: WSDL-driven stubs (kept, §2.6/§4.4)
    @WebServiceRef(CustomerService_Service.class)
    private CustomerService soapCustomerService;

    // EXISTING coupling 2 — remote EJB: shared api jar (kept, §4.4).
    // RUNTIME-VERIFIED (§3.1 gotcha): this ejb: URL only resolves if the app/module
    // names match the deployed artifacts, and only from a separate process (in-server
    // calls need the provider's java:global binding instead). When the deployments are
    // renamed during the migration, the call fails at invocation time with
    // EJBCLIENT000409: No more destinations are available — verified on EAP 8.0.11.
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

