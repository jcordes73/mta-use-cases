# API Map — Apache Ant to Apache Maven

This migration is a build-tool migration, so most "APIs" are Ant build-file constructs rather than Java classes — those live in `pattern-map.md`. This table covers the Java-level API elements the guide names.

| old_api | new_api | kind | action | before | after | notes | source_section |
|---|---|---|---|---|---|---|---|
| `javax.servlet.http.HttpServlet` | `jakarta.servlet.http.HttpServlet` | class | move_package | `import javax.servlet.http.HttpServlet;` | `import jakarta.servlet.http.HttpServlet;` | Only when migrating to a Jakarta EE container; javax→jakarta is a code change (import rewrites), not a dependency swap. Match the namespace your container implements | Packaging Web Applications (WAR) |
| `javax.servlet.jsp.*` | `jakarta.servlet.jsp.*` | package | move_package | `import javax.servlet.jsp.*;` | `import jakarta.servlet.jsp.*;` | Same Jakarta namespace rule; JSP API jars also become `provided`-scoped dependencies | Packaging Web Applications (WAR) |
| `org.apache.tools.ant.Task` | keep (custom tasks stay as-is) | class | replace | `class VersionStampTask extends org.apache.tools.ant.Task { ... }` | same class, invoked via `maven-antrun-plugin` with the task jar in the plugin's `<dependencies>` | Custom task classes survive the migration via the Antrun escape hatch; rewrite as a Maven mojo only if the step is build-proper | Phase 5: Custom Ant Tasks and the Antrun Escape Hatch |
