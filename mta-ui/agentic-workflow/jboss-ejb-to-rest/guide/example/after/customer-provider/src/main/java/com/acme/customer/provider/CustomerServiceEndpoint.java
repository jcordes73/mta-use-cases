package com.acme.customer.provider;

import com.acme.customer.api.CustomerDTO;
import com.acme.customer.api.CustomerNotFoundException;
import jakarta.ejb.EJB;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

/**
 * UNCHANGED from before/ — the @WebService endpoint keeps serving its existing
 * SOAP clients; REST is offered IN ADDITION (§2.6). Retirement is a separate,
 * audited decision (§4.4).
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
