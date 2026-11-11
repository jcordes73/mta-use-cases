package com.acme.order.soap;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

/**
 * wsconsume-generated port-type interface, simplified (§2.6 note: the SOAP client is
 * WSDL-driven — wsconsume generates these stubs; they are shown here as the shape of
 * the existing client code, kept untouched by the refactoring).
 */
@WebService(name = "CustomerService", targetNamespace = "http://acme.com/customer")
public interface CustomerService {

    @WebMethod(operationName = "findCustomer")
    CustomerDTO findCustomer(long id) throws CustomerNotFoundException_Exception;
}
