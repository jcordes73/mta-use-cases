# Phase: Build System Migration

Replace Spring Boot build infrastructure with Quarkus equivalents.

## Steps

1. Read `references/dependency-map.md`
2. Read `references/pattern-map.md` — filter for rows with `source_section` relating to build system (BOM, plugin, Java version)

### POM restructuring

3. **Remove the Spring Boot parent POM:**
   - Delete the entire `<parent>` block containing `spring-boot-starter-parent`
   - Add a `<dependencyManagement>` section with the Quarkus BOM:
     ```xml
     <dependencyManagement>
         <dependencies>
             <dependency>
                 <groupId>io.quarkus.platform</groupId>
                 <artifactId>quarkus-bom</artifactId>
                 <version>${quarkus.platform.version}</version>
                 <type>pom</type>
                 <scope>import</scope>
             </dependency>
         </dependencies>
     </dependencyManagement>
     ```
   - Add the Quarkus version property: `<quarkus.platform.version>3.x.x</quarkus.platform.version>`

4. **Replace the Spring Boot Maven plugin:**
   - Remove `spring-boot-maven-plugin`
   - Add `quarkus-maven-plugin` with `build`, `generate-code`, and `generate-code-tests` goals

5. **Update Java compiler version:**
   - Set `maven.compiler.source` and `maven.compiler.target` to 17 or later

### Dependency replacement

6. For each row in the dependency map:
   - Search the build file for `old_artifact`
   - Apply the `action`:
     - `replace` → change the artifact coordinate to `new_artifact`; remove explicit version tags (Quarkus BOM manages versions)
     - `remove` → delete the dependency entry
   - Check `notes` for gotchas

7. **Choose migration path for web and DI:**
   - **Quarkus Spring (compatibility):** Add `quarkus-spring-web`, `quarkus-spring-di`, and optionally `quarkus-spring-data-jpa` extensions to keep Spring annotations working
   - **Pure Quarkus:** Use JAX-RS (`quarkus-rest-jackson`) and CDI directly — requires code changes in the Code phase

8. Run the build gate

## Build gate

Run `mvn compile`. If it fails, fix dependency issues before proceeding. Common issues:
- Missing Quarkus extensions (add via `mvn quarkus:add-extension -Dextensions="..."`)
- Version conflicts from transitive Spring dependencies still on classpath
- Removed starters that had no Quarkus equivalent
