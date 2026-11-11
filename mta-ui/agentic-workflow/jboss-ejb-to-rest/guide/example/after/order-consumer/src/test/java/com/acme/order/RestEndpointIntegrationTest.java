package com.acme.order;

import org.citrusframework.TestCaseRunner;
import org.citrusframework.TestActionSupport;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.http.client.HttpClientBuilder;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * INTEGRATION TEST (CITRUS, HTTP LEVEL) — the NEW REST path of the AFTER state
 * (§4.5 "after" state, REST leg of the dual-run).
 *
 * Exercises the provider's REST facade (§2.2/§2.6) at the HTTP level the way an
 * external consumer does — the executable version of the REST rows of the §10.3
 * endpoint matrix:
 *
 *   GET /<context>/api/v1/customers/42   → 401 unauthenticated / 200 + JSON DTO
 *   GET /<context>/api/v1/customers/999  → 404 + JSON error body + X-Customer-Id
 *
 * The 404 JSON error body is the NEW error contract (§2.2, §5.3) — the counterpart of
 * the checked exception / SOAP fault baseline encoded by the before/ tests.
 *
 * Requires a provisioned EAP with the after/ provider deployed — see the guide's
 * §10.2/§10.3 for the exact setup. Skipped (not failed) unless the server is
 * configured:
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
class RestEndpointIntegrationTest implements TestActionSupport {

    private static HttpClient httpClient;
    private static String basic;

    @BeforeAll
    static void createClient() {
        assumeTrue(System.getProperty("customer.server") != null,
                "no EAP configured; run with -Dcustomer.server=<host:port> (guide §10.3)");
        String host = System.getProperty("customer.server");
        String deployment = System.getProperty("customer.deployment",
                "customer-provider-soap-2.0.0");
        String user = System.getProperty("customer.test.user", "testuser");
        String password = System.getProperty("customer.test.password", "testpassword1!");
        basic = "Basic " + Base64.getEncoder().encodeToString(
                (user + ":" + password).getBytes(StandardCharsets.UTF_8));
        httpClient = new HttpClientBuilder()
                .requestUrl("http://" + host + "/" + deployment)
                .build();
    }

    /** §10.3 matrix row: unauthenticated REST call is rejected. */
    @Test
    @CitrusTest(name = "rest-unauthorized")
    void getCustomerWithoutCredentialsIsUnauthorized(@CitrusResource TestCaseRunner runner) {
        runner.run(http().client(httpClient)
                .send()
                .get("/api/v1/customers/42")
                .message());
        runner.run(http().client(httpClient)
                .receive()
                .response(401));
    }

    /** §10.3 matrix row: the full HTTP → JAX-RS → EJB → security chain works. */
    @Test
    @CitrusTest(name = "rest-get-customer")
    void getCustomerReturnsJsonDtoForKnownId(@CitrusResource TestCaseRunner runner) {
        runner.run(http().client(httpClient)
                .send()
                .get("/api/v1/customers/42")
                .message()
                .header("Authorization", basic)
                .accept("application/json"));
        runner.run(http().client(httpClient)
                .receive()
                .response(200)
                .message()
                .validate((message, context) -> {
                    String body = message.getPayload(String.class);
                    assertTrue(body.contains("\"id\":42"), "id: " + body);
                    assertTrue(body.contains("\"name\":\"Customer 42\""), "name: " + body);
                    assertTrue(body.contains("\"email\":\"customer42@example.com\""),
                            "email: " + body);
                }));
    }

    /**
     * The NEW error contract (§2.2, §5.3): an unknown id is a 404 with the JSON error
     * body and the machine-readable X-Customer-Id header the client-side mapper
     * (§3.2) turns back into the consumer-owned exception. The provider's stub
     * fabricates a customer for ANY positive id — its error contract is "id <= 0",
     * so the unknown id here is negative (matching the before/ baseline tests).
     */
    @Test
    @CitrusTest(name = "rest-not-found")
    void getUnknownCustomerReturnsJsonErrorContract(@CitrusResource TestCaseRunner runner) {
        runner.run(http().client(httpClient)
                .send()
                .get("/api/v1/customers/-1")
                .message()
                .header("Authorization", basic)
                .accept("application/json"));
        runner.run(http().client(httpClient)
                .receive()
                .response(404)
                .message()
                .header("X-Customer-Id", "-1")
                .validate((message, context) -> {
                    String body = message.getPayload(String.class);
                    assertTrue(body.contains("CUSTOMER_NOT_FOUND"), "error code: " + body);
                    assertTrue(body.contains("\"customerId\":-1"), "customerId: " + body);
                }));
    }
}
