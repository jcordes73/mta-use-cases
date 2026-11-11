package poc.citrus.synthetic;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.citrusframework.GherkinTestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.rmi.client.RmiClient;
import org.citrusframework.rmi.client.RmiClientBuilder;
import org.citrusframework.rmi.server.RmiServer;
import org.citrusframework.rmi.server.RmiServerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.rmi.message.RmiMessage.invocation;
import static org.citrusframework.rmi.message.RmiMessage.result;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Citrus in-JVM tests for the {@code citrus-rmi} client-server pair:
 * the {@code RmiServer} creates a JRMP registry, binds a proxy for the
 * {@link NewsService} remote interface, and hands the incoming method
 * calls to the test for validation; the test scripts the return value
 * (or exception) and the {@code RmiClient} receives it.
 *
 * <p>
 * Scope note: the only RMI-flavored quickstart (jts) speaks IIOP/CORBA,
 * which citrus-rmi cannot reach (it is JRMP-registry based). This is a
 * synthetic surface validating the {@code citrus-rmi} DSL and the
 * request/script/reply pattern.
 * </p>
 */
@CitrusSupport
public class RmiRoundTripTest {

    private static RmiServer rmiServer;
    private static RmiClient rmiClient;

    @BeforeAll
    static void startRmiServer() {
        // probe-close-rebind is racy on busy machines (an unrelated local
        // connection can grab the probed ephemeral port in between), so
        // retry with a fresh port on bind failure
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 5 && rmiServer == null; attempt++) {
            int port = probeFreePort();
            RmiServer server = new RmiServerBuilder()
                    .host("localhost")
                    .port(port)
                    .remoteInterfaces(NewsService.class)
                    .binding("newsService")
                    .createRegistry(true)
                    .build();
            try {
                // NOTE: no autoStart(true) here — initialize() already calls
                // start() when autoStart is set, and a second explicit start()
                // re-binds the registry (BindException / "ObjID already in use")
                server.initialize();
                server.start();
                rmiServer = server;
                rmiClient = new RmiClientBuilder()
                        .host("localhost")
                        .port(port)
                        .binding("newsService")
                        .timeout(5000L)
                        .build();
            } catch (RuntimeException e) {
                try {
                    server.stop();
                } catch (RuntimeException ignore) {
                    // best effort - the bind failed, nothing may be running
                }
                lastFailure = e;
            }
        }
        if (rmiServer == null) {
            throw new IllegalStateException("could not start RMI server after 5 attempts", lastFailure);
        }
    }

    private static int probeFreePort() {
        try (ServerSocket probe = new ServerSocket(0)) {
            return probe.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("cannot probe a free registry port", e);
        }
    }

    @AfterAll
    static void stopRmiServer() {
        if (rmiServer != null) {
            rmiServer.stop();
        }
    }

    @Test
    @CitrusTest
    void clientInvocationRoundTripsThroughSimulatedService(@CitrusResource GherkinTestActionRunner runner) {
        // client invokes the remote proxy; RmiClient.send() BLOCKS on the
        // RMI reflection call until the server replies, so the send must be
        // forked (same pattern as upstream RmiEndpointJavaIT) or the test
        // deadlocks on its way to the server-side receive
        runner.then(send(rmiClient)
                .message(invocation(NewsService.class, "getNews"))
                .fork(true));

        runner.then(receive(rmiServer)
                .message(invocation(NewsService.class, "getNews")));

        // test scripts the return value, unblocking the RMI server thread
        runner.then(send(rmiServer)
                .message(result("News from citrus!")));

        runner.then(receive(rmiClient)
                .message(result("News from citrus!")));
    }

    @Test
    @CitrusTest
    void serverScriptedExceptionReachesClient(@CitrusResource GherkinTestActionRunner runner,
            @CitrusResource TestContext context) throws Exception {

        // the client-side RMI failure is NOT a receivable message: the
        // invocation exception surfaces as a thrown CitrusRuntimeException
        // inside RmiClient.send() ("Failed to invoke method on remote
        // target"). A forked send would swallow that failure on the
        // executor thread, so run the blocking send on a hand-rolled
        // thread and assert the outcome directly.
        CompletableFuture<Throwable> sendOutcome = new CompletableFuture<>();
        Thread caller = new Thread(() -> {
            try {
                rmiClient.send(invocation(NewsService.class, "setNews").argument("bad input"), context);
                sendOutcome.complete(null);
            } catch (Throwable t) {
                sendOutcome.complete(t);
            }
        });
        caller.start();

        runner.then(receive(rmiServer)
                .message(invocation(NewsService.class, "setNews").argument("bad input")));

        // test scripts the exception, unblocking the RMI server thread
        // (exception() is an instance method on a result message)
        runner.then(send(rmiServer)
                .message(result().exception("Something went wrong")));

        Throwable failure = sendOutcome.get(10, TimeUnit.SECONDS);
        caller.join(1000L);

        assertNotNull(failure, "client send should have failed with the scripted exception");
        assertInstanceOf(CitrusRuntimeException.class, failure);
        assertTrue(
                String.valueOf(failure.getMessage()).contains("Failed to invoke method on remote target"),
                "unexpected client failure message: " + failure.getMessage());
        assertNotNull(failure.getCause(), "failure should carry the remote cause");
        assertTrue(
                String.valueOf(failure.getCause()).contains("Something went wrong"),
                "cause should carry the scripted exception message, got: " + failure.getCause());
    }
}
