package com.acme.order.soap;

import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebEndpoint;
import jakarta.xml.ws.WebServiceClient;

import javax.xml.namespace.QName;
import java.net.URL;

/**
 * wsconsume-generated service class, simplified (§2.6: WSDL-driven client stubs).
 */
@WebServiceClient(name = "CustomerServiceService",
        targetNamespace = "http://acme.com/customer",
        wsdlLocation = "http://customer-app:8080/customer/CustomerServiceService?wsdl")
public class CustomerService_Service extends Service {

    private static final QName SERVICE_NAME =
            new QName("http://acme.com/customer", "CustomerServiceService");

    public CustomerService_Service(URL wsdlLocation) {
        super(wsdlLocation, SERVICE_NAME);
    }

    @WebEndpoint(name = "CustomerServicePort")
    public CustomerService getCustomerServicePort() {
        return super.getPort(new QName("http://acme.com/customer", "CustomerServicePort"),
                CustomerService.class);
    }
}
