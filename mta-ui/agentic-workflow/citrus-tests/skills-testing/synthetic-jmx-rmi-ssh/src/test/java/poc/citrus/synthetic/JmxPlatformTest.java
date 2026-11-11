package poc.citrus.synthetic;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.citrusframework.GherkinTestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.jmx.client.JmxClient;
import org.citrusframework.jmx.client.JmxClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.jmx.message.JmxMessage.invocation;
import static org.citrusframework.jmx.message.JmxMessage.result;

/**
 * Citrus in-JVM tests for the {@code citrus-jmx} client against the
 * test JVM's <b>platform MBean server</b> ({@code serverUrl("platform")}).
 *
 * <p>
 * Scope note: no quickstart in the repo registers MBeans, so this is a
 * synthetic surface — a JVM platform bean ({@code java.lang:type=Memory})
 * plus a custom MBean registered into the same platform server by the
 * test itself. This validates the {@code citrus-jmx} DSL and message
 * model, not any application code.
 * </p>
 */
@CitrusSupport
public class JmxPlatformTest {

    private static final String CUSTOM_BEAN = "poc.citrus.synthetic:type=Synthetic,name=One";

    private static ObjectName registeredName;

    @BeforeAll
    static void registerCustomMBean() throws Exception {
        MBeanServer platform = ManagementFactory.getPlatformMBeanServer();
        registeredName = platform
                .registerMBean(new StandardMBean(new SyntheticMBeanImpl(), SyntheticMBean.class),
                        new ObjectName(CUSTOM_BEAN))
                .getObjectName();
    }

    @AfterAll
    static void unregisterCustomMBean() throws Exception {
        if (registeredName != null) {
            ManagementFactory.getPlatformMBeanServer().unregisterMBean(registeredName);
        }
    }

    private static JmxClient platformClient() {
        return new JmxClientBuilder()
                .serverUrl("platform")
                .timeout(5000L)
                .build();
    }

    @Test
    @CitrusTest
    void readsPlatformBeanAttributeAndInvokesOperation(@CitrusResource GherkinTestActionRunner runner) {
        JmxClient client = platformClient();

        // attribute read on a JVM-provided bean
        runner.then(send(client)
                .message(invocation("java.lang:type=Memory").attribute("Verbose")));
        runner.then(receive(client)
                .message(result(false)));

        // void operation on the same bean (MemoryMXBean#gc)
        runner.then(send(client)
                .message(invocation("java.lang:type=Memory").operation("gc")));
        runner.then(receive(client)
                .message(result()));
    }

    @Test
    @CitrusTest
    void invokesCustomMBeanOperationAndAttribute(@CitrusResource GherkinTestActionRunner runner) {
        JmxClient client = platformClient();

        // operation with parameter, then observe the side effect as attribute
        runner.then(send(client)
                .message(invocation(CUSTOM_BEAN).operation("greet").parameter("Citrus")));
        runner.then(receive(client)
                .message(result("Hello Citrus")));

        runner.then(send(client)
                .message(invocation(CUSTOM_BEAN).attribute("Counter")));
        runner.then(receive(client)
                .message(result(1)));
    }
}
