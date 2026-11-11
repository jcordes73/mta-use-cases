package com.acme.order;

import com.acme.order.rest.CustomerServiceClient;
import com.acme.order.rest.dto.CustomerDTO;
import com.acme.order.rest.exception.CustomerNotFoundResponseExceptionMapper;
import com.acme.order.rest.exception.CustomerNotFoundException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.TestActionSupport;
import org.citrusframework.actions.AbstractTestAction;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.context.TestContext;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * INTEGRATION TEST (CITRUS) — the consumer-side MP REST Client path of the AFTER
 * state (§3.2), REST leg of the §4.5 dual-run.
 *
 * Exercises the NEW call path the way the after consumer uses it for new call sites:
 * the @RegisterRestClient interface (contract = the published OpenAPI document, §2.7),
 * consumer-owned DTO and exception, and the client-side ResponseExceptionMapper that
 * translates a 404 back into the checked CustomerNotFoundException (§3.2, §5.3).
 *
 * Uses the PROGRAMMATIC RestClientBuilder — the §3.2 fallback that deploys and works
 * on base EAP, unlike the @Inject variant (XP-only, WELD-001408 — §3.1). Runs against
 * a provisioned EAP with the after/ provider deployed — see the guide's §10.2/§10.3.
 * Skipped (not failed) unless the server is configured:
 *
 *   mvn test -Dcustomer.server=localhost:8080
 *
 * Optional overrides: -Dcustomer.deployment=<runtime-name without .war>,
 * -Dcustomer.test.user=<user>, -Dcustomer.test.password=<password> — the defaults
 * match the §10.3 smoke-test deployment (testuser/testpassword1!, role
 * customer-reader). The smoke-test setup secures the REST path with the web.xml
 * BASIC overlay (§10.3); in production this path uses MP-JWT bearer tokens (§2.4,
 * §3.5) — on base EAP (no XP) MP-JWT cannot deploy (§3.1).
 */
@CitrusSupport
class RestClientIntegrationTest implements TestActionSupport {

    private static CustomerServiceClient client;

    @BeforeAll
    static void createClient() {
        assumeTrue(System.getProperty("customer.server") != null,
                "no EAP configured; run with -Dcustomer.server=<host:port> (guide §10.3)");
        String host = System.getProperty("customer.server");
        String deployment = System.getProperty("customer.deployment",
                "customer-provider-soap-2.0.0");
        String user = System.getProperty("customer.test.user", "testuser");
        String password = System.getProperty("customer.test.password", "testpassword1!");
        // programmatic fallback (§3.2): baseUri overrides the @RegisterRestClient
        // annotation per environment; the @RegisterProvider mapper on the interface
        // is honored per the MP REST Client spec
        client = RestClientBuilder.newBuilder()
                .baseUri(URI.create("http://" + host + "/" + deployment))
                .register(new BasicAuthFilter(user, password))
                .register(CustomerNotFoundResponseExceptionMapper.class)
                .build(CustomerServiceClient.class);
    }

    /** Happy path: a known id returns the CONSUMER-OWNED DTO over the REST path. */
    @Test
    @CitrusTest(name = "rest-client-find-customer")
    void findCustomerReturnsConsumerDtoForKnownId(@CitrusResource TestCaseRunner runner) {
        runner.run(action(new AbstractTestAction() {
            @Override
            public void doExecute(TestContext context) {
                try {
                    CustomerDTO customer = client.findCustomer(42);
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
     * Error translation (§3.2, §5.3): a 404 from the REST path is re-raised by the
     * client-side mapper as the CONSUMER-OWNED checked exception, with the id
     * extracted from the machine-readable X-Customer-Id header — the REST counterpart
     * of the checked-exception/SOAP-fault baseline encoded by the before/ tests. The
     * provider's stub fabricates a customer for ANY positive id — its error contract
     * is "id <= 0", so the unknown id here is negative.
     */
    @Test
    @CitrusTest(name = "rest-client-not-found-mapped")
    void findCustomerRaisesConsumerNotFoundForUnknownId(@CitrusResource TestCaseRunner runner) {
        runner.run(action(new AbstractTestAction() {
            @Override
            public void doExecute(TestContext context) {
                CustomerNotFoundException e = assertThrows(CustomerNotFoundException.class,
                        () -> client.findCustomer(-1));
                assertEquals(-1, e.getCustomerId());
            }
        }));
    }

    /**
     * The smoke-test setup secures the REST path with the web.xml BASIC overlay
     * (§10.3) — send the credentials on every request. In production the REST path
     * uses MP-JWT bearer tokens (§2.4, §3.5); the filter shows where the auth
     * plumbing attaches (a ClientRequestFilter — the same seam a bearer token
     * would use, §3.5).
     */
    private static final class BasicAuthFilter implements ClientRequestFilter {

        private final String authorization;

        private BasicAuthFilter(String user, String password) {
            this.authorization = "Basic " + Base64.getEncoder().encodeToString(
                    (user + ":" + password).getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void filter(ClientRequestContext requestContext) {
            requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, authorization);
        }
    }
}
