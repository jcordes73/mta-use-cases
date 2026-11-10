# JMX / RMI / SSH recipe — `citrus-jmx`, `citrus-rmi`, `citrus-ssh`

> **Status: DRAFT — grounded in the Citrus 5.1.0 MCP catalog + docs
> (module, builders and message DSL verified against the 5.1.0
> endpoint-jmx / endpoint-rmi / endpoint-ssh docs); NOT validated
> live.** The pitfalls below are *expected*, not observed. Validate
> through the `skills-testing/` harness on a real quickstart before
> trusting this recipe; fold observed failure signatures back in and
> flip this banner to verified.

Three simulator/management families share this file — each app
typically needs only one section.

## JMX — `citrus-jmx`

Management beans (`@MBean`, `StandardMBean`, remote JMX connectors).

### Dependencies (test-scoped)

```xml
<dependency>
    <groupId>org.citrusframework</groupId>
    <artifactId>citrus-jmx</artifactId>
</dependency>
```

### Endpoint construction (5.1.0 builders)

```java
// client against the JVM's own platform MBean server (in-JVM lane):
@BindToRegistry
public JmxClient jmxClient() {
    return new JmxClientBuilder()
            .serverUrl("platform")
            .build();
}

// or a simulated remote MBean server:
@BindToRegistry
public JmxServer jmxServer() {
    return new JmxServerBuilder()
            .serverUrl("service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi")
            .mbeans(jmxBeans())     // scripted mbean definitions
            .build();
}
```

### Test skeleton

```java
// read an attribute
$(send(jmxClient)
    .message(JmxMessage.invocation("java.lang:type=Memory")
        .attribute("Verbose")));
$(receive(jmxClient)
    .message(JmxMessage.result(false)));

// invoke an operation on an app-registered mbean
$(send(jmxClient)
    .message(JmxMessage.invocation("org.citrusframework.news:name=News")
        .operation("clearNews")));  // exact operation name/signature per plan
```

### Expected pitfalls (UNVALIDATED)

- `serverUrl("platform")` reaches the *test JVM's* platform MBean
  server — the app's beans are only there if the app runs in-process
  (in-JVM lane). A remote `JmxServer` simulation is the alternative.
- MBean object names must match exactly (quoting, domain) — copy from
  the app's registration code.
- Operations with parameters need `.parameter(...)` typed per the
  MBeanInfo — signature mismatch fails at resolve time.

## RMI — `citrus-rmi`

Remote interfaces (`java.rmi.Remote` EJB-style services).

### Dependencies (test-scoped)

```xml
<dependency>
    <groupId>org.citrusframework</groupId>
    <artifactId>citrus-rmi</artifactId>
</dependency>
```

### Endpoint construction (5.1.0 builders)

```java
@BindToRegistry
public RmiClient rmiClient() {
    return new RmiClientBuilder()
            .host("localhost")
            .port(1099)
            .binding("newsService")
            .build();            // or .serverUrl("rmi://localhost:1099/newsService")
}

@BindToRegistry
public RmiServer rmiServer() {   // simulate the remote service
    return new RmiServerBuilder()
            .host("localhost")
            .port(1099)
            .remoteInterfaces(NewsService.class)
            .binding("newsService")
            .createRegistry(true)
            .autoStart(true)
            .build();
}
```

### Test skeleton

```java
$(send(rmiClient).message(RmiMessage.invocation(NewsService.class, "getNews")));
$(receive(rmiClient).message(RmiMessage.result("This is news from RMI!")));

// scripted error path:
$(receive(rmiServer).message(RmiMessage.invocation(NewsService.class, "setNews")));
$(send(rmiServer).message(RmiMessage.exception("Something went wrong")));
```

### Expected pitfalls (UNVALIDATED)

- Both client and server need the *same* interface class on the
  classpath (test-scoped dependency on the app's API, or a stub).
- `createRegistry(true)` — a registry already bound on the port fails
  the server start; isolate port per test class.
- RMI system properties (`java.rmi.server.hostname`) can break
  registry lookups in containers.

## SSH — `citrus-ssh`

Applications shelling out over SSH or exposing SSH command channels.
Citrus models SSH as **XML documents** (`ssh-request` / `ssh-response`,
namespace `http://www.citrusframework.org/schema/ssh/message`), not raw
protocol commands.

### Dependencies (test-scoped)

```xml
<dependency>
    <groupId>org.citrusframework</groupId>
    <artifactId>citrus-ssh</artifactId>
</dependency>
```

### Endpoint construction (5.1.0 builders)

```java
@BindToRegistry
public SshClient sshClient() {
    return new SshClientBuilder()
            .host("localhost")
            .port(9072)
            .user("admin")
            .privateKeyPath("classpath:ssh/test_user.priv")
            .strictHostChecking(false)   // sandbox: no known_hosts
            .build();
}

@BindToRegistry
public SshServer sshServer() {   // simulate the SSH server
    return new SshServerBuilder()
            .port(9072)
            .user("admin")
            .allowedKeyPath("classpath:ssh/test_user_pub.pem")
            .endpointAdapter(...)        // scripts the response
            .autoStart(true)
            .build();
}
```

### Test skeleton

```java
$(send(sshClient).message()
    .body("""
        <ssh-command xmlns="http://www.citrusframework.org/schema/ssh/message">
          <command>cat</command>
          <stdin>Hello</stdin>
        </ssh-command>
        """));
$(receive(sshClient).message()
    .body("""/* validated ssh-response with stdout/stderr/exit code */"""));
```

### Expected pitfalls (UNVALIDATED)

- Key-based auth requires the keypair on the classpath; password auth
  needs the server's user/password config — the sandbox typically has
  neither wired for the app, so plan around the app's actual client
  configuration.
- Host-key checking on by default: `strictHostChecking(false)` is
  mandatory in the sandbox or every connect fails.
- The server responds via `endpointAdapter` — without one, no response
  is scripted and the client hangs.

## Harness validation checklist (all three families)

- [ ] one family exercised against a real quickstart before promotion
- [ ] failure signatures recorded below (none yet)
