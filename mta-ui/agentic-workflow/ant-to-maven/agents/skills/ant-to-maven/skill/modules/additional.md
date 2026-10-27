# Phase: Additional Changes

WAR/EAR packaging, container deployment, Arquillian, multi-module builds, and CI/CD + release.

## Steps

1. Read `references/pattern-map.md` — apply the rows for these topics
2. **WAR packaging** (guide Chapter 7): `<packaging>war</packaging>`; web resources in `src/main/webapp` (with `WEB-INF/web.xml`); the hand-assembled `WEB-INF/lib` staging disappears
   - All container-supplied APIs (servlet, CDI, JSP, JSF) declared `<scope>provided</scope>` — no JEE jar in `WEB-INF/lib` of the built WAR
   - Match the namespace to the container: `jakarta.*` for Jakarta EE, `javax.*` for Java EE 8
   - Ant deploy/start/stop targets → `org.codehaus.cargo:cargo-maven3-plugin` (or a native plugin: `wildfly-maven-plugin`, embedded-glassfish); keep `mvn deploy` (repository publish) distinct
   - Hand-wired container tests → Arquillian deployments (`arquillian-junit5-container` + container adapter, `@Deployment` method) running under Failsafe
3. **EAR packaging** (guide Chapter 8): `<packaging>ear</packaging>` on an aggregator module; `maven-ear-plugin` with `generateApplicationXml`, `<defaultLibBundleDir>lib/</defaultLibBundleDir>`, `<modules>` listing `webModule`/`jarModule` entries; EAR module declares sibling `<dependencies>` with `<type>war</type>`
   - Skinny wars: `<skinnyWars>true</skinnyWars>` (or `<excludeLibs>`) so `WEB-INF/lib` holds only WAR-specific jars
4. **Multi-module builds** (guide Chapter 10): driver script (`<subant>`/`<ant antfile=...>`) → aggregator POM (packaging `pom`) with `<modules>` + reactor
   - Shared config → parent POM `<properties>` + `<dependencyManagement>`; children use `<parent>` and declare dependencies without versions
   - Sibling jars by GAV coordinates, never `<path>` to a sibling's `dist/` output; partial builds with `mvn -pl <module> -am package`
5. **CI/CD and release** (guide Chapter 12): `ant clean test jar` → `mvn -B -ntp verify`; cache `~/.m2/repository` (not all of `~/.m2`); builds must succeed cold
   - Versioning: `versions-maven-plugin` (`versions:set`, `versions:revert`, `versions:commit`); no hand-edited version bumps
   - Releases: `<scm>` element + `maven-release-plugin` with `tagNameFormat` and `releaseProfiles`; `release:prepare`/`release:perform`
   - Deploy: `<distributionManagement>` with release/snapshot repositories; credentials in `settings.xml` `<servers>` keyed by matching `<id>`; `mvn -B -ntp deploy` gated in CI
6. Run the build gate

## Build gate

Run `mvn clean package` (from the aggregator root for multi-module builds). Verify the built artifact: for a WAR, `WEB-INF/lib` contains no container jars; for an EAR, no duplicated copies of the same jar; for CI, the build succeeds on a cold cache.
