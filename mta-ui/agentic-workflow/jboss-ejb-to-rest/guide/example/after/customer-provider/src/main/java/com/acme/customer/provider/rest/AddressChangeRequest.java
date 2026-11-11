package com.acme.customer.provider.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * NEW code — DTO for a new REST-only capability, with Bean Validation constraints
 * replacing the strict XSD the SOAP schema enforced (§2.6, translatable design work):
 * the constraints are opt-in per endpoint (@Valid on the resource method), violations
 * become a 400 via ValidationExceptionMapper.
 */
public class AddressChangeRequest {

    @NotBlank
    private String street;

    @NotBlank
    @Size(max = 20)
    private String postalCode;

    @NotBlank
    @Size(max = 60)
    private String city;

    @Pattern(regexp = "^\\+?[0-9 -]{6,20}$", message = "not a valid phone number")
    private String phone;

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
