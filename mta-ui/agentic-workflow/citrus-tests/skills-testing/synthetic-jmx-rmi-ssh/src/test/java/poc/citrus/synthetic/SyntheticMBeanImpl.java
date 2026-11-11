package poc.citrus.synthetic;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of the {@link SyntheticMBean} management interface.
 * {@code greet} answers and ticks the counter so the test can observe
 * an operation side effect through an attribute read.
 */
public class SyntheticMBeanImpl implements SyntheticMBean {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public String greet(String name) {
        counter.incrementAndGet();
        return "Hello " + name;
    }

    @Override
    public int getCounter() {
        return counter.get();
    }
}
