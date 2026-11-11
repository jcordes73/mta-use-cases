package com.acme.order;

import org.citrusframework.TestCaseRunner;
import org.citrusframework.TestActionSupport;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.ws.client.WebServiceClient;
import org.citrusframework.ws.client.WebServiceClientBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * INTEGRATION TEST (CITRUS) — SOAP path of the BEFORE state (§4.5 "before" baseline).
 *
 * Exercises the provider's @WebService endpoint at the MESSAGE level the way Citrus
 * does it: the SOAP client builds the envelope around the payload, sends it over HTTP
 * and the receive action validates the response (guide §10.3). These are the tests
 * the after/ contract test (dual-run, §4.5) compares the REST path against.
 *
 * Citrus provides no stub-based client here on purpose: the wire contract IS the WSDL
 * (§2.6), so the tests pin the actual SOAP request/response shapes of the endpoint.
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
class SoapIntegrationTest implements TestActionSupport {

    private static WebServiceClient soapClient;

    @BeforeAll
    static void createClient() {
        assumeTrue(System.getProperty("customer.server") != null,
                "no EAP configured; run with -Dcustomer.server=<host:port> (guide §10.3)");
        String server = System.getProperty("customer.server");
        String deployment = System.getProperty("customer.deployment",
                "customer-provider-soap-2.0.0");
        String user = System.getProperty("customer.test.user", "testuser");
        String password = System.getProperty("customer.test.password", "testpassword1!");
        // the @RolesAllowed-secured bean requires an authenticated caller — send HTTP
        // BASIC credentials on every request (§2.4: provider-side security). NOTE: the
        // HttpComponents5MessageSender route has two pitfalls in this stack — the
        // no-arg constructor + setCredentials leaves its internal HttpClient null in
        // spring-ws 4.x (getHttpClient() never builds from the client factory), and
        // the httpclient5 connection path hits httpcore5 strictness ("Content-Length
        // header already present"). The dependency-free HttpUrlConnectionMessageSender
        // with a PREEMPTIVE Authorization header avoids both.
        String authorization = "Basic " + Base64.getEncoder().encodeToString(
                (user + ":" + password).getBytes(StandardCharsets.UTF_8));
        HttpUrlConnectionMessageSender sender = new HttpUrlConnectionMessageSender() {
            @Override
            protected void prepareConnection(HttpURLConnection connection) throws IOException {
                connection.setRequestProperty("Authorization", authorization);
                super.prepareConnection(connection);
            }
        };
        soapClient = new WebServiceClientBuilder()
                .defaultUri("http://" + server + "/" + deployment
                        + "/CustomerServiceService")
                .messageSender(sender)
                .build();
    }

    /** Happy path: a known id returns the DTO via the SOAP contract (§4.5). */
    @Test
    @CitrusTest(name = "soap-find-customer")
    void findCustomerReturnsDtoForKnownId(@CitrusResource TestCaseRunner runner) {
        runner.run(soap().client(soapClient)
                .send()
                .message()
                .body("<ns:findCustomer xmlns:ns=\"http://acme.com/customer\">"
                        + "<arg0>42</arg0></ns:findCustomer>"));
        runner.run(soap().client(soapClient)
                .receive()
                .message()
                .validate((message, context) -> {
                    String body = message.getPayload(String.class);
                    assertTrue(body.contains("findCustomerResponse"),
                            "expected findCustomerResponse wrapper in: " + body);
                    assertTrue(body.contains("<id>42</id>"), "id: " + body);
                    assertTrue(body.contains("<name>Customer 42</name>"), "name: " + body);
                    assertTrue(body.contains("<email>customer42@example.com</email>"),
                            "email: " + body);
                }));
    }

    /**
     * Error contract: an unknown id surfaces as a SOAP fault — the checked-exception
     * stub type of the @WebFault (§5.3). The error shape the REST migration must
     * preserve (§4.5).
     */
    @Test
    @CitrusTest(name = "soap-not-found-fault")
    void findCustomerRaisesSoapFaultForUnknownId(@CitrusResource TestCaseRunner runner) {
        runner.run(assertException()
                .exception(SoapFaultClientException.class)
                .when(soap().client(soapClient)
                        .send()
                        .message()
                        .body("<ns:findCustomer xmlns:ns=\"http://acme.com/customer\">"
                                + "<arg0>-1</arg0></ns:findCustomer>")));
    }
}
