package com.example.legacy;

import javax.xml.bind.JAXBContext;     // java.xml.bind module, removed in JDK 11
import javax.xml.ws.Service;           // java.xml.ws module, removed in JDK 11
import javax.activation.DataHandler;   // java.activation module, removed in JDK 11
import javax.annotation.PostConstruct; // java.xml.ws.annotation module, removed in JDK 11

/**
 * Test source for the jep320-removed-javaee-modules rules. Each import
 * triggers exactly one incident of the corresponding rule.
 */
public class JavaeeModuleUsage {

    private JAXBContext context;
    private Service service;
    private DataHandler handler;

    @PostConstruct
    public void init() {
    }
}
