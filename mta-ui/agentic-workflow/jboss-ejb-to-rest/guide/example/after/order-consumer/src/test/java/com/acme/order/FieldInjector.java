package com.acme.order;

import java.lang.reflect.Field;

/**
 * UNIT TEST helper: injects test doubles into the @EJB/@WebServiceRef/@Inject fields
 * the container would populate. The unit tests run outside JBoss EAP, so the CDI/EJB
 * injection is simulated directly on the field — the wiring itself (which annotations
 * the call sites carry) is verified by the integration tests (§4.5).
 */
final class FieldInjector {

    private FieldInjector() {
    }

    static void inject(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot inject " + fieldName, e);
        }
    }
}
