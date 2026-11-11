package com.acme.order;

import com.acme.customer.api.CustomerDTO;
import com.acme.customer.api.CustomerNotFoundException;
import com.acme.customer.api.CustomerServiceRemote;
import com.acme.order.soap.CustomerNotFoundException_Exception;
import com.acme.order.soap.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UNIT TEST (pure JVM — no server): the two old call paths of the BEFORE consumer
 * (§2.6), exercised with test doubles standing in for the container-injected
 * @WebServiceRef/@EJB proxies (FieldInjector). Verifies the delegation and the checked-
 * exception propagation the integration tests cover end to end. The wsconsume-GENERATED
 * SOAP stubs (com.acme.order.soap.*) are excluded from the coverage measurement —
 * generated code is verified by the integration tests against the live WSDL, not by
 * unit tests.
 */
class OrderProcessorTest {

    private OrderProcessor processor;
    private RecordingSoapClient soap;
    private StubRemote remote;

    /** Fake of the wsconsume-generated SEI (com.acme.order.soap.CustomerService). */
    private static class RecordingSoapClient implements CustomerService {
        long lastId = -1;

        @Override
        public com.acme.order.soap.CustomerDTO findCustomer(long id)
                throws CustomerNotFoundException_Exception {
            lastId = id;
            return new com.acme.order.soap.CustomerDTO();
        }
    }

    /** Fake of the remote view (com.acme.customer.api.CustomerServiceRemote). */
    private static class StubRemote implements CustomerServiceRemote {
        long lastId = -1;
        boolean fail = false;

        @Override
        public CustomerDTO findCustomer(long id) throws CustomerNotFoundException {
            lastId = id;
            if (fail) {
                throw new CustomerNotFoundException(id);
            }
            CustomerDTO dto = new CustomerDTO();
            dto.setId(id);
            return dto;
        }
    }

    @BeforeEach
    void injectFakes() {
        processor = new OrderProcessor();
        soap = new RecordingSoapClient();
        remote = new StubRemote();
        FieldInjector.inject(processor, "soapCustomerService", soap);
        FieldInjector.inject(processor, "remoteCustomerService", remote);
    }

    /** placeOrder calls the SOAP stub path (WSDL-driven, §2.6). */
    @Test
    void placeOrderDelegatesToTheSoapCallSite() throws CustomerNotFoundException_Exception {
        processor.placeOrder(42, "widget");
        assertEquals(42, soap.lastId);
    }

    /** lookupCustomer calls the remote-EJB path (shared api jar, §2.6). */
    @Test
    void lookupCustomerDelegatesToTheRemoteCallSite() throws CustomerNotFoundException {
        CustomerDTO dto = processor.lookupCustomer(42);
        assertEquals(42, remote.lastId);
        assertEquals(42, dto.getId());
    }

    /** The checked api exception propagates unchanged from the remote call site. */
    @Test
    void lookupCustomerPropagatesTheCheckedNotFound() {
        remote.fail = true;
        CustomerNotFoundException e = assertThrows(CustomerNotFoundException.class,
                () -> processor.lookupCustomer(-1));
        assertEquals(-1, e.getCustomerId());
    }
}
