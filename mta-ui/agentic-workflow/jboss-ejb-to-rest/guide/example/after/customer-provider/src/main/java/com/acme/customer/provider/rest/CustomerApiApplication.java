package com.acme.customer.provider.rest;

import jakarta.annotation.security.DeclareRoles;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS activation. NOTE (§2.4): @LoginConfig does not exist in Jakarta EE 10 — it
 * arrived in EE 11 — so the MP-JWT auth method is configured in web.xml
 * (<login-config><auth-method>MP-JWT</auth-method></login-config>). The
 * microprofile-jwt-smallrye subsystem validates the tokens (§2.4) — requires the EAP
 * XP; on base EAP use an Elytron token-realm instead (no XP needed, §2.4). The token's
 * `groups` claim maps to the roles checked by @RolesAllowed.
 *
 * The web.xml here contains ONLY the login-config; JAX-RS auto-discovery still applies
 * (the @ApplicationPath on this class activates the resources — see "Deployment
 * Descriptors to Check" in the guide).
 */
@ApplicationPath("/")
@DeclareRoles("customer-reader")
public class CustomerApiApplication extends Application {
}
