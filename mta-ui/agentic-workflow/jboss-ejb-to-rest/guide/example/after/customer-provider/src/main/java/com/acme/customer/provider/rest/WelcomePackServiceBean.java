package com.acme.customer.provider.rest;

import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NEW code — replaces the SOAP @Oneway operation (§2.6, translatable design work):
 * the REST endpoint answers 202 immediately; the work runs in this @Asynchronous
 * EJB method (§5.7), and progress is visible to the consumer via a status resource
 * referenced by the 202's Location header.
 */
@Stateless
public class WelcomePackServiceBean {

    private final Map<Long, String> statusByCustomer = new ConcurrentHashMap<>();

    @Asynchronous
    public void start(long customerId) {
        statusByCustomer.put(customerId, "RUNNING");
        try {
            Thread.sleep(50); // stub: the actual welcome-pack work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        statusByCustomer.put(customerId, "DONE");
    }

    public String status(long customerId) {
        return statusByCustomer.getOrDefault(customerId, "UNKNOWN");
    }
}
