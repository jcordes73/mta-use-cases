package com.acme.order.rest.dto;

/**
 * Consumer-owned DTO mirrored from the provider's OpenAPI document (§3.7) — NOT the
 * wsconsume-generated SOAP stub type; new call sites work against the REST contract.
 */
public class CustomerDTO {

    private long id;
    private String name;
    private String email;

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
