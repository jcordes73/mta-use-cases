package poc.citrus.synthetic;

/**
 * Custom management bean interface, registered into the platform
 * MBean server by {@code JmxPlatformTest} and invoked through the
 * Citrus {@code JmxClient}.
 */
public interface SyntheticMBean {

    String greet(String name);

    int getCounter();
}
