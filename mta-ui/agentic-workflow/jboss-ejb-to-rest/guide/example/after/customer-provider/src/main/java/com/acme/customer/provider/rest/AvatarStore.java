package com.acme.customer.provider.rest;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NEW code (§2.6: add only new classes) — in-memory stub for the object store that
 * replaces the EJB's MTOM/XOP attachment handling (§2.6, translatable design work).
 * A real implementation streams to an object store or a DB BLOB instead of buffering
 * the binary in memory.
 */
@ApplicationScoped
public class AvatarStore {

    private final Map<Long, byte[]> avatars = new ConcurrentHashMap<>();

    public void put(long customerId, byte[] data) {
        avatars.put(customerId, data);
    }

    public byte[] get(long customerId) {
        return avatars.get(customerId);
    }
}
