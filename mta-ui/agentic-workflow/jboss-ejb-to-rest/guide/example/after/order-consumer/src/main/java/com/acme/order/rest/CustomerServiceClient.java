package com.acme.order.rest;

import com.acme.order.rest.dto.CustomerDTO;
import com.acme.order.rest.exception.CustomerNotFoundResponseExceptionMapper;
import com.acme.order.rest.exception.CustomerNotFoundException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.InputStream;

/**
 * NEW: the REST client interface mirroring the provider's REST API (§2.6/§3.2). The
 * contract is the published OpenAPI document (§2.7); this interface is generated from
 * it or kept in sync manually (§3.7).
 *
 * baseUri is overridden per environment via MicroProfile Config (§3.2/§3.5):
 *   com.acme.order.rest.CustomerServiceClient/mp-rest/url=https://customer-app:8443
 */
@Path("/api/v1/customers")
@RegisterRestClient(baseUri = "https://customer-app:8443")
@RegisterProvider(CustomerNotFoundResponseExceptionMapper.class)
public interface CustomerServiceClient {

    /** CustomerNotFoundException is re-raised by the client-side mapper from a 404. */
    @GET
    @Path("/{id}")
    CustomerDTO findCustomer(@PathParam("id") long id) throws CustomerNotFoundException;

    /**
     * Binary upload (MTOM → octet-stream, §2.6): the consumer sends raw bytes — no
     * MTOM encoding on either side. Error statuses (404, 413) raise via the mapper.
     */
    @PUT
    @Path("/{id}/avatar")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    Response uploadAvatar(@PathParam("id") long id, InputStream data);

    /**
     * @Oneway → 202 + async (§2.6): the raw Response lets the caller verify the
     * 202, read the Location header, and poll the status resource — the WS-Addressing
     * callback is replaced by explicit polling (§5.7).
     */
    @POST
    @Path("/{id}/welcome-pack")
    Response requestWelcomePack(@PathParam("id") long id) throws CustomerNotFoundException;
}
