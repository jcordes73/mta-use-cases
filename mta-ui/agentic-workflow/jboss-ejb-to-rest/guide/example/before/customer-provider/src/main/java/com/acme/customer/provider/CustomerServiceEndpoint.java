package com.acme.customer.provider;

import com.acme.customer.api.CustomerDTO;
import com.acme.customer.api.CustomerNotFoundException;
import jakarta.ejb.EJB;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

/**
 * BEFORE: the EJB exposed as a SOAP web service (JAX-WS) — one of the two
 * consumer-facing views (the other is the remote EJB view, CustomerServiceRemote).
 * Existing consumers call this endpoint over SOAP; REST is ADDED in addition (§2.6),
 * the SOAP endpoint is NOT retired.
 */
@WebService(name = "CustomerService", serviceName = "CustomerServiceService",
        targetNamespace = "http://acme.com/customer",
        wsdlLocation = "WEB-INF/wsdl/CustomerServiceService.wsdl")
public class CustomerServiceEndpoint {

    @EJB
    private CustomerServiceBean customerService;

    @WebMethod(operationName = "findCustomer")
    public CustomerDTO findCustomer(long id) throws CustomerNotFoundException {
        return customerService.findCustomer(id);
    }
}
