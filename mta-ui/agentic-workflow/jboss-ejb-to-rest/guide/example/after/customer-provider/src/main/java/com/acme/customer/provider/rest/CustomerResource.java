package com.acme.customer.provider.rest;

import com.acme.customer.api.CustomerDTO;
import com.acme.customer.api.CustomerNotFoundException;
import com.acme.customer.provider.CustomerServiceBean;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * NEW REST facade (§2.6) — the existing @WebService endpoint is left untouched; REST is
 * offered IN ADDITION. Calls the existing SOAP-exposed EJB over its LOCAL interface
 * (same deployment, no network hop).
 *
 * §2.6 fallback: if the bean were NOT reachable locally (different deployment), this
 * facade would call the existing SOAP endpoint instead — see the §2.6 fallback code;
 * either way the EJB code itself is not changed.
 *
 * The endpoints below the first one demonstrate the translatable design work from §2.6:
 * MTOM attachments → streaming bodies, @Oneway → 202 + @Asynchronous, XSD enforcement →
 * Bean Validation. (The correlation-ID filter translation is CorrelationIdFilter.)
 */
@Path("/api/v1/customers")
@RolesAllowed("customer-reader")
public class CustomerResource {

    @EJB // local view: same deployment — the §2.6 preferred path
    private CustomerServiceBean customerService;

    // --- NEW code for new REST-only capabilities (§2.6: add only new classes) ---

    @Inject
    private AvatarStore avatarStore;

    @Inject
    private WelcomePackServiceBean welcomePackService;

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public CustomerDTO getCustomer(@PathParam("id") long id) throws CustomerNotFoundException {
        return customerService.findCustomer(id);
    }

    /**
     * MTOM/XOP attachment → streaming body (§2.6, translatable design work): a single
     * binary as application/octet-stream, streamed with a size limit enforced before
     * buffering; unknown content types are rejected by the framework (415).
     */
    @PUT
    @Path("/{id}/avatar")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response uploadAvatar(@PathParam("id") long id, InputStream data)
            throws IOException, CustomerNotFoundException {
        customerService.findCustomer(id); // 404 on unknown customer
        byte[] bytes = data.readNBytes(1_000_000); // 1 MB limit
        if (data.read() != -1) {
            return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE).build();
        }
        if (bytes.length == 0) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        avatarStore.put(id, bytes);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/avatar")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getAvatar(@PathParam("id") long id) {
        byte[] avatar = avatarStore.get(id);
        return avatar == null
                ? Response.status(Response.Status.NOT_FOUND).build()
                : Response.ok(avatar).build();
    }

    /**
     * @Oneway fire-and-forget → 202 Accepted + @Asynchronous (§2.6, §5.7): the endpoint
     * answers immediately; the consumer learns of completion by polling the status
     * resource in the Location header.
     */
    @POST
    @Path("/{id}/welcome-pack")
    public Response requestWelcomePack(@PathParam("id") long id) throws CustomerNotFoundException {
        customerService.findCustomer(id); // 404 on unknown customer
        welcomePackService.start(id);
        return Response.accepted()
                .location(URI.create("/api/v1/customers/" + id + "/welcome-pack/status"))
                .build();
    }

    @GET
    @Path("/{id}/welcome-pack/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response welcomePackStatus(@PathParam("id") long id) {
        return Response.ok(Json.createObjectBuilder()
                .add("status", welcomePackService.status(id))
                .build()).build();
    }

    /**
     * XSD enforcement → Bean Validation (§2.6, translatable design work): the DTO's
     * constraints are checked because of @Valid; violations become a 400 with a JSON
     * error body via ValidationExceptionMapper.
     */
    @POST
    @Path("/{id}/address-change-requests")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response requestAddressChange(@PathParam("id") long id, @Valid AddressChangeRequest request)
            throws CustomerNotFoundException {
        customerService.findCustomer(id); // 404 on unknown customer
        // stub: persist the address-change request
        return Response.accepted().build();
    }
}
