# Dependency Map

| old_artifact | new_artifact | version_constraint | action | notes | source_section |
|---|---|---|---|---|---|
| `org.jboss:jboss-ejb-client` (direct or via BOM) | removed | | remove | Consumer EJB client artifact; delete only after the last call site for the connection has cut over. External-JVM test usage needs the full client stack (naming, elytron, xnio, remoting, discovery, marshalling, transaction client) — BOM-managed test-scope deps | §4.2, §6, §10.3 |
| `org.jboss.eap:wildfly-ejb-client-bom` | removed | | remove | Imported in `<dependencyManagement>`; remove after last cutover (§4.4 audit gates any earlier removal of remoting bits) | §4.2, §6, §10.1 |
| shared remote-interface jar (`<app>-api`, compile dependency in consumers) | client artifact generated from the OpenAPI document (`openapi-generator-maven-plugin`, `--library microprofile`, `-DuseJakartaEe=true`) | | replace | Removes the compile-time coupling and shared-jar version ping-pong; keep the shared jar published for consumers that never migrate. `useJakartaEe=true` is mandatory — without it generated code uses `javax.*` and does not compile | §3.7, §3.2 |
| `wsconsume`/`wsimport` generated SOAP stub sources | `openapi-generator-cli` / Maven-plugin generated client (`-g java --library microprofile`) | | replace | Only for call sites migrated to REST; existing SOAP clients keep working unchanged. Treat generated code as a build artifact — never hand-edit; regenerate only when the provider changes the contract | §3.2, §3.7 |
| EJB client artifacts: `jboss-ejb-client.xml`, `jndi.properties` (classpath) | removed | | remove | Outbound connection definitions and legacy naming config; delete per application once all its call sites are on REST (gated cleanup) | §1.1, §4.2, §6 |
