# FTP / SFTP recipe — `citrus-ftp`

> **Status: DRAFT — grounded in the Citrus 5.1.0 MCP catalog + docs
> (module, endpoint properties, FTP server/client DSL verified against
> the 5.1.0 endpoint-ftp docs); SFTP half is catalog-grounded only and
> NOT validated live.** Validate through the `skills-testing/` harness
> before trusting; fold observed failure signatures back in.

## When the plan routes here

Applications that up/download files over FTP/SFTP. The test runs the
Citrus FTP/SFTP *server* as the simulator and points the app at it; for
outbound-server testing the `ftp-client` drives a real server.

## Dependencies (test-scoped)

```xml
<dependency>
    <groupId>org.citrusframework</groupId>
    <artifactId>citrus-ftp</artifactId>
</dependency>
```

## FTP server simulation (docs-verified DSL)

```java
@BindToRegistry
public FtpServer ftpServer() {
    return CitrusEndpoints.ftp()
            .server()
            .port(2222)
            .autoLogin(true)
            .autoStart(true)
            // commands handled automatically (positive result) so the
            // test only verifies the interesting ones:
            .autoHandleCommands("PORT,TYPE")
            .userManagerProperties(new ClassPathResource("citrus.ftp.user.properties"))
            .build();
}
```

`citrus.ftp.user.properties` (SHA-hashed password for `admin`/`admin`,
write permission, home dir under `target/`):

```properties
ftpserver.user.admin.userpassword=c7ad44cbad762a5da0a452f9e854fdc1e0e7a52a38015f23f3eab1d80b931dd472634dfac71cd34ebc35d16ab7fb8a90c81f975113d6c7538dc69dd8de9077ec
ftpserver.user.admin.homedirectory=target/ftp/user/admin
ftpserver.user.admin.enableflag=true
ftpserver.user.admin.writepermission=true
```

Test skeleton — verify the app's commands on the server side:

```java
$(receive(ftpServer).message(FtpMessage.command(FTPCmd.STOR).arguments("/test/hello.txt")));
$(send(ftpServer).message(FtpMessage.success()));   // then assert file landed in the user home
```

Client-side driving (for server-behavior tests):

```java
$(send(ftpClient).message(FtpMessage.put("test/hello.txt", DataType.ASCII).arguments("")));
$(receive(ftpClient).message(FtpMessage.result(result)));  // reply code 226, @contains('Transfer complete')@
```

## SFTP (catalog-grounded, builder names per docs)

`SftpClient`/`SftpServer` (same module): server needs `hostKeyPath`
(classpath RSA key) + `userHomePath`; client needs `strictHostChecking(false)`
in the sandbox, `privateKeyPath` or user/password. Same
command/result message model as FTP.

## Expected pitfalls (UNVALIDATED — confirm each in the harness run)

- User-manager properties file is mandatory for `autoLogin` — a missing
  file or wrong password hash fails the login *before* any command.
- Passive-mode data connections inside a containerized sandbox: fix
  `localPassiveMode` and watch for port-range firewall effects.
- The user home directory is on the real filesystem — clean it in
  `@BeforeEach`/`@AfterEach` for test isolation.
- Files land relative to the user home — assert content there, not via
  the client, to prove what the *app* transferred.

## Harness validation checklist

- [ ] app → simulator STOR + RETR round trip green in-JVM
- [ ] SFTP variant if the plan needs it
- [ ] failure signatures recorded below (none yet)
