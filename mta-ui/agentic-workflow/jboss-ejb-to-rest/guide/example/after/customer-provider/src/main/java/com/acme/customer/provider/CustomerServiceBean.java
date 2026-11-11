package com.acme.customer.provider;

import com.acme.customer.api.CustomerDTO;
import com.acme.customer.api.CustomerNotFoundException;
import com.acme.customer.api.CustomerServiceRemote;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

/**
 * UNCHANGED from before/ — the refactoring is additive. The bean is exposed
 * through TWO consumer-facing views: the SOAP endpoint (CustomerServiceEndpoint)
 * and the remote EJB view (CustomerServiceRemote). The REST facade in rest/ calls
 * this bean over its LOCAL interface; both existing views stay supported until the
 * §4.4 audit decides retirement separately.
 *
 * @LocalBean is REQUIRED here: the bean implements an interface (the remote view),
 * so without it the no-interface (local) view would not exist and the in-deployment
 * injections of the bean class (@EJB in the SOAP endpoint and the REST resource)
 * would fail at runtime (§5: classic EJB view gotcha).
 */
@Stateless
@LocalBean
@RolesAllowed("customer-reader")
public class CustomerServiceBean implements CustomerServiceRemote {

    @Override
    public CustomerDTO findCustomer(long id) throws CustomerNotFoundException {
        // stub: a real implementation would load from the database
        if (id <= 0) {
            throw new CustomerNotFoundException(id);
        }
        return new CustomerDTO(id, "Customer " + id, "customer" + id + "@example.com");
    }
}
