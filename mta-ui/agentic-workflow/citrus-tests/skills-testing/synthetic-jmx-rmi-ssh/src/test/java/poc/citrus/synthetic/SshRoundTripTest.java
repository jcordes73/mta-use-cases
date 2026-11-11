package poc.citrus.synthetic;

import java.io.IOException;
import java.net.ServerSocket;

import org.citrusframework.GherkinTestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.ssh.client.SshClient;
import org.citrusframework.ssh.client.SshClientBuilder;
import org.citrusframework.ssh.server.SshServer;
import org.citrusframework.ssh.server.SshServerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.ssh.message.SshMessage.request;
import static org.citrusframework.ssh.message.SshMessage.response;

/**
 * Citrus in-JVM tests for the {@code citrus-ssh} client-server pair:
 * the embedded {@code SshServer} (Apache MINA SSHD) accepts exec
 * requests and hands them to the test for validation; the test scripts
 * the {@code ssh-response} and the {@code SshClient} receives it.
 *
 * <p>
 * Both authentication mechanisms are covered against one server that
 * allows password <b>and</b> publickey auth (they are tried in turn):
 * password-based login in the first test, key-based login in the second
 * using the host/user keypair that ships inside the {@code citrus-ssh}
 * jar ({@code org/citrusframework/ssh/citrus.pem} + {@code citrus_pub.pem}
 * — the 5.1.0 jar does NOT ship the {@code test_user.*} keys the docs
 * reference, and {@code citrus.pem} is unencrypted, so no
 * {@code privateKeyPassword} is needed).
 * </p>
 */
@CitrusSupport
public class SshRoundTripTest {

    private static SshServer sshServer;
    private static int port;

    @BeforeAll
    static void startSshServer() {
        // probe-close-rebind is racy on busy machines (an unrelated local
        // connection can grab the probed ephemeral port in between), so
        // retry with a fresh port on bind failure
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 5 && sshServer == null; attempt++) {
            SshServer server = new SshServerBuilder()
                    .port(probeFreePort())
                    .user("admin")
                    .password("s3cr3t-pw")
                    .allowedKeyPath("classpath:org/citrusframework/ssh/citrus_pub.pem")
                    .build();
            try {
                // NOTE: no autoStart(true) here — initialize() already calls
                // start() when autoStart is set, and a second explicit start()
                // re-binds the SSH socket (BindException)
                server.initialize();
                server.start();
                sshServer = server;
                port = sshServer.getPort();
            } catch (RuntimeException e) {
                try {
                    server.stop();
                } catch (RuntimeException ignore) {
                    // best effort - the bind failed, nothing may be running
                }
                lastFailure = e;
            }
        }
        if (sshServer == null) {
            throw new IllegalStateException("could not start SSH server after 5 attempts", lastFailure);
        }
    }

    private static int probeFreePort() {
        try (ServerSocket probe = new ServerSocket(0)) {
            return probe.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("cannot probe a free SSH port", e);
        }
    }

    @AfterAll
    static void stopSshServer() {
        if (sshServer != null) {
            sshServer.stop();
        }
    }

    private static SshClient passwordClient() {
        return new SshClientBuilder()
                .host("localhost")
                .port(port)
                .user("admin")
                .password("s3cr3t-pw")
                .strictHostChecking(false)
                .timeout(5000L)
                .build();
    }

    private static SshClient keyClient() {
        return new SshClientBuilder()
                .host("localhost")
                .port(port)
                .user("admin")
                .privateKeyPath("classpath:org/citrusframework/ssh/citrus.pem")
                .strictHostChecking(false)
                .timeout(5000L)
                .build();
    }

    @Test
    @CitrusTest
    void passwordAuthExecRoundTrip(@CitrusResource GherkinTestActionRunner runner) {
        SshClient client = passwordClient();

        runner.then(send(client)
                .message(request("echo Hello", "stdin-data")));

        runner.then(receive(sshServer)
                .message(request("echo Hello", "stdin-data")));

        runner.then(send(sshServer)
                .message(response("Hello from citrus!", "", 0)));

        runner.then(receive(client)
                .message(response("Hello from citrus!", "", 0)));
    }

    @Test
    @CitrusTest
    void keyAuthExecRoundTrip(@CitrusResource GherkinTestActionRunner runner) {
        SshClient client = keyClient();

        runner.then(send(client)
                .message(request("uptime", "")));

        runner.then(receive(sshServer)
                .message(request("uptime", "")));

        runner.then(send(sshServer)
                .message(response("10:10:10 up 1 day", "", 0)));

        runner.then(receive(client)
                .message(response("10:10:10 up 1 day", "", 0)));
    }
}
