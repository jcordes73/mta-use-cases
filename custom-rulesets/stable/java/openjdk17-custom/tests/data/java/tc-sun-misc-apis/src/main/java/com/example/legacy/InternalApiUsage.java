package com.example.legacy;

import sun.misc.BASE64Encoder; // removed in JDK 9 (JEP 260)

/**
 * Test source for the sun-misc-internal-apis rule.
 */
public class InternalApiUsage {

    public String encode(byte[] data) {
        return new BASE64Encoder().encode(data);
    }
}
