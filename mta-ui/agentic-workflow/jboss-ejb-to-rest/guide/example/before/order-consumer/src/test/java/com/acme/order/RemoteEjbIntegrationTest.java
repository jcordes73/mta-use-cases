package com.acme.order;

import com.acme.customer.api.CustomerDTO;
import com.acme.customer.api.CustomerNotFoundException;
import com.acme.customer.api.CustomerServiceRemote;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.TestActionSupport;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.context.TestContext;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * INTEGRATION TEST (CITRUS) — remote-EJB path of the BEFORE state (§4.5 "before"
 * baseline).
 *
 * Exercises the provider's @Remote view the way the BEFORE consumer does: a remote
 * lookup over remoting and checked-exception error handling. These are the tests the
 * after/ contract test (dual-run, §4.5) compares the REST path against.
 *
 * Citrus has no native EJB endpoint (it tests message transports: HTTP, SOAP, JMS,
 * Kafka, ...), so the JNDI lookup and the remote calls run as a Citrus CUSTOM ACTION
 * (guide §10.3: custom action) — that keeps the calls in the Citrus test report while
 * reusing JUnit assertions on the DTO and the checked exception.
 *
 * Requires a provisioned EAP with the provider deployed — see the guide's §10.2/§10.3
 * for the exact setup. Skipped (not failed) unless the server is configured:
 *
 *   mvn test -Dcustomer.server=localhost:8080
 *
 * Optional overrides: -Dcustomer.deployment=<runtime-name without .war>,
 * -Dcustomer.test.user=<user>, -Dcustomer.test.password=<password> — the defaults
 * match the §10.3 smoke-test deployment (testuser/testpassword1!, role
 * customer-reader), which the @RolesAllowed-secured bean requires.
 */
@CitrusSupport
class RemoteEjbIntegrationTest implements TestActionSupport {

    private static CustomerServiceRemote remote;

    @BeforeAll
    static void lookupRemote() {
        assumeTrue(System.getProperty("customer.server") != null,
                "no EAP configured; run with -Dcustomer.server=<host:port> (guide §10.3)");
        String host = System.getProperty("customer.server");
        String deployment = System.getProperty("customer.deployment",
                "customer-provider-soap-2.0.0");
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "org.wildfly.naming.client.WildFlyInitialContextFactory");
        env.put(Context.PROVIDER_URL, "remote+http://" + host);
        env.put(Context.SECURITY_PRINCIPAL,
                System.getProperty("customer.test.user", "testuser"));
        env.put(Context.SECURITY_CREDENTIALS,
                System.getProperty("customer.test.password", "testpassword1!"));
        try {
            InitialContext ic = new InitialContext(env);
            remote = (CustomerServiceRemote) ic.lookup(
                    "ejb:/" + deployment + "/CustomerServiceBean!"
                            + CustomerServiceRemote.class.getName());
        } catch (Exception e) {
            throw new IllegalStateException("remote EJB lookup failed: " + e, e);
        }
    }

    /** Happy path: a known id returns the DTO over the remote view (§4.5). */
    @Test
    @CitrusTest(name = "remote-ejb-find-customer")
    void findCustomerReturnsDtoForKnownId(@CitrusResource TestCaseRunner runner) {
        runner.run(action(new org.citrusframework.actions.AbstractTestAction() {
            @Override
            public void doExecute(TestContext context) {
                try {
                    CustomerDTO customer = remote.findCustomer(42);
                    assertEquals(42, customer.getId());
                    assertEquals("Customer 42", customer.getName());
                    assertEquals("customer42@example.com", customer.getEmail());
                } catch (CustomerNotFoundException e) {
                    throw new AssertionError("unexpected not-found for id 42", e);
                }
            }
        }));
    }

    /**
     * Error contract: an unknown id raises the CHECKED exception over remoting —
     * the error shape the REST migration must preserve (§4.5, §5.3).
     */
    @Test
    @CitrusTest(name = "remote-ejb-not-found")
    void findCustomerRaisesCheckedNotFoundForUnknownId(@CitrusResource TestCaseRunner runner) {
        runner.run(action(new org.citrusframework.actions.AbstractTestAction() {
            @Override
            public void doExecute(TestContext context) {
                CustomerNotFoundException e = assertThrows(CustomerNotFoundException.class,
                        () -> remote.findCustomer(-1));
                assertEquals(-1, e.getCustomerId());
            }
        }));
    }

    /**
     * Deterministic error contract (§4.5): the same unknown id fails identically on
     * every call — prerequisite for comparing with the REST path.
     */
    @Test
    @CitrusTest(name = "remote-ejb-error-contract-deterministic")
    void errorContractIsDeterministic(@CitrusResource TestCaseRunner runner) {
        runner.run(action(new org.citrusframework.actions.AbstractTestAction() {
            @Override
            public void doExecute(TestContext context) {
                for (int i = 0; i < 3; i++) {
                    CustomerNotFoundException e = assertThrows(CustomerNotFoundException.class,
                            () -> remote.findCustomer(-7));
                    assertEquals(-7, e.getCustomerId());
                }
            }
        }));
    }
}
