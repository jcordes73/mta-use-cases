# Dependency Map — JDK 8 to 17

Minimum versions for JDK 17 compatibility. `floor` = do not ship older; `recommended` = known-good on 17.

| artifact | old (typical on JDK 8) | floor | recommended | reason | notes |
|---|---|---|---|---|---|
| org.ow2.asm:asm | 6.x / 5.x | 9.1 | 9.6 | Class file major 61 parsing | Usually transitive — pin via dependencyManagement |
| net.bytebuddy:byte-buddy | 1.9.x | 1.12.7 | 1.14.x | Instrumentation of JDK 17 internals; exact-version check refuses unknown JVMs | Mockito/Hibernate pull it; pin the managed version |
| org.mockito:mockito-core | 2.x / 3.x | 4.1 | 5.x | Inline mock maker on JDK 17; PowerMock replacement | `mockito-inline` needed for static/final mocks ≤4.x |
| org.jacoco:org.jacoco.agent (maven plugin) | 0.8.3 | 0.8.8 | 0.8.11 | JDK 17 class files in instrumentation | Older agent crashes forked test JVM |
| org.projectlombok:lombok | 1.18.10 | 1.18.24 | 1.18.30 | Annotation processor on javac 17 |javac internals changed 9→17 |
| org.codehaus.groovy:groovy | 2.4.x / 2.5.x | 3.0.9 | 4.x (if code allows) | Groovy 2.x does not run on JDK 17 | Groovy 3+ handles module system |
| org.mapstruct:mapstruct / processor | 1.3.x | 1.5.0 | 1.5.5 | javac 17 annotation processing | |
| com.google.errorprone:error_prone_core | 2.3.x | 2.10.0 | 2.20+ | javac 17 internals | |
| org.springframework:spring-core/context/... | 5.0–5.2 | 5.3.0 | 5.3.39 | JDK 17 supported from 5.3 | Spring 6 requires Jakarta — out of scope here |
| org.springframework.boot:spring-boot-dependencies | < 2.5.5 | 2.5.5 | 2.7.18 | First JDK 17 support 2.5.5 | Do not jump to 3.x inside this migration |
| org.hibernate:hibernate-core | 5.3 / 5.4 | 5.5.0 | 5.6.15 | JDK 17 bytecode support | Hibernate 6 = Jakarta migration |
| com.fasterxml.jackson.core:jackson-databind | 2.9 / 2.10 | 2.13.0 | 2.17.x | Reflects into JDK internals; older versions trip JEP 403 | Also CVE floor 2.13.4.2 |
| org.apache.logging.log4j:log4j-core | 2.x old | 2.17.2 | 2.23.x | Log4Shell + JDK 17 | |
| org.apache.commons:commons-lang3 | 3.8 | 3.12.0 | 3.14 | JDK 17 runtime support | |
| org.junit:junit-bucket / junit-jupiter | 5.3 | 5.8 | 5.10 | JDK 17 test JVM support | JUnit 4 unaffected |
| org.testcontainers:testcontainers | 1.15 | 1.17 | 1.20 | Uses JDK internals / docker client refresh | |
| org.openjdk.nashorn:nashorn-core | (n/a) | 15.4 | 15.4 | Standalone Nashorn after JEP 372 | Only if keeping Nashorn scripting |
| jakarta.xml.bind:jakarta.xml.bind-api | (n/a) | 2.3.3 | 2.3.3 | Replaces JDK-removed JAXB | `javax.xml.bind:jaxb-api:2.3.1` if keeping javax imports |
| org.glassfish.jaxb:jaxb-runtime | (n/a) | 2.3.9 | 2.3.9 | JAXB runtime (was bundled) | scope=runtime |
| jakarta.annotation:jakarta.annotation-api | (n/a) | 1.3.5 | 1.3.5 | Replaces JDK-removed EE annotations | `javax.annotation:javax.annotation-api:1.3.2` for javax imports |
| com.sun.xml.ws:jaxws-rt | (n/a) | 2.3.7 | 2.3.7 | Replaces JDK-removed JAX-WS | Only if JAX-WS used |
| org.apache.maven.plugins:maven-compiler-plugin | 3.8.0 | 3.11.0 | 3.13.0 | Reliable `<release>` handling | |
| org.apache.maven.plugins:maven-surefire-plugin | 2.12.4–2.21 | 2.22.0 | 3.2.5 | Fork JVM args on 17; class file parsing | Failsafe same |
| org.apache.maven.plugins:maven-shade-plugin | 3.1 | 3.4.1 | 3.5.x | Class rewriting on module-info/major 61 | |
