package com.acme.customer.api;

import java.io.Serializable;

/**
 * Shared DTO — part of the compile-time EJB coupling: consumers calling the remote
 * view compile against this jar. (The SOAP-only consumer does NOT need it — its
 * contract is the WSDL; this example's consumer mixes both couplings.)
 */
public class CustomerDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private String name;
    private String email;

    public CustomerDTO() {
    }

    public CustomerDTO(long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
