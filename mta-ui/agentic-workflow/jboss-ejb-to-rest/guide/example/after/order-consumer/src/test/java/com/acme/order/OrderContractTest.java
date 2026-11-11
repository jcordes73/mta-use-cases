package com.acme.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CONTRACT TEST SKELETON (§4.5) — EXAMPLE ONLY, not build-tested.
 *
 * The consumer's old paths (remote EJB AND SOAP) and the new REST path live in
 * parallel (§4.1). Dual-run comparison (§4.5): identical inputs must produce identical
 * outputs and error behavior on ALL THREE paths. Run in CI until the §4.4 audit shows
 * every consumer on REST.
 *
 * Filling in the TODOs requires a running provider (Testcontainers with a JBoss EAP
 * image, or a test environment URL) — wire that in per project.
 */
class OrderContractTest {

    // TODO: wire all three clients (§3.5 auth config):
    //
    //   CustomerServiceClient rest = RestClientBuilder.newBuilder()
    //           .baseUri(URI.create(System.getProperty("customer.baseUri")))
    //           .register(CustomerNotFoundResponseExceptionMapper.class)
    //           .build(CustomerServiceClient.class);
    //   CustomerService soap = new CustomerService_Service(
    //           URI.create(System.getProperty("customer.soapWsdl")).toURL())
    //           .getCustomerServicePort();
    //   CustomerServiceRemote remote = InitialContext.doLookup(
    //           "ejb:customer-app/customer-provider/CustomerServiceBean!"
    //                   + "com.acme.customer.api.CustomerServiceRemote");

    /**
     * Same input → same output on the remote-EJB path, the SOAP path, and the new
     * REST path (§4.5).
     */
    @Test
    void contract_restMatchesRemoteAndSoapForFindCustomer() {
        // TODO: for a representative set of ids, assert field-by-field equality of
        // remote.findCustomer(id), soap.findCustomer(id), and rest.findCustomer(id).
    }

    /**
     * Error contract: unknown id → SOAP fault on the SOAP path, checked exception over
     * remoting, 404 → exception on the REST path (§5.3).
     */
    @Test
    void contract_restRaisesNotFoundLikeTheOldPaths() {
        // TODO:
        //   assertThrows(CustomerNotFoundException_Exception.class, () -> soap.findCustomer(-1));
        //   assertThrows(CustomerNotFoundException.class, () -> remote.findCustomer(-1));
        //   assertThrows(com.acme.order.rest.exception.CustomerNotFoundException.class,
        //           () -> rest.findCustomer(-1));
    }

    /**
     * Dual-run comparison while all paths live in parallel (§4.1/§4.5); disable (do
     * not delete) once the §4.4 audit shows every consumer on REST.
     */
    @Test
    void dualRun_allPathsAgree() {
        // TODO: for a representative set of ids, assert remote, soap, and rest agree.
    }

    /** Per-call-site cutover gate (§4.3): a cut-over call site no longer resolves EJB lookups. */
    @Test
    void cutover_noEjbLookupRemainsInCutOverCode() {
        // TODO: static assertion — e.g. an ArchUnit or reflection check that the
        // refactored classes have no @EJB(lookup=...) pointing at the migrated bean.
    }
}
