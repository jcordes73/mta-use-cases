package com.acme.order.soap;

/**
 * wsconsume-generated DTO, simplified: an independent consumer-side type derived from
 * the WSDL — NOT the provider's class (the SOAP contract is the WSDL, §2.6).
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
