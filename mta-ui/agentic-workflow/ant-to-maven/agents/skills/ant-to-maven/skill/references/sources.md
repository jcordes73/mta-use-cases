# Sources — Apache Ant to Apache Maven

Provenance for every `source_section` value in the mapping tables.

## Primary guide

Source: `README.md` (local file, repo: ant-to-maven). Single-page guide; fragment anchors follow the heading slugs below.

| source_section | Anchor |
|---|---|
| Phase 1: Scaffold the POM | README.md#3-phase-1-scaffold-the-pom |
| Phase 2: Dependencies | README.md#4-phase-2-dependencies |
| Phase 3: Map Build Steps to Plugins | README.md#5-phase-3-map-build-steps-to-plugins |
| Phase 4: Migrate Tests | README.md#6-phase-4-migrate-tests |
| Packaging Web Applications (WAR) | README.md#7-packaging-web-applications-war |
| Packaging Enterprise Applications (EAR) | README.md#8-packaging-enterprise-applications-ear |
| Phase 5: Custom Ant Tasks and the Antrun Escape Hatch | README.md#9-phase-5-custom-ant-tasks-and-the-antrun-escape-hatch |
| Phase 6: Multi-Module Builds | README.md#10-phase-6-multi-module-builds |
| Phase 7: Profiles, Properties, Environments | README.md#11-phase-7-profiles-properties-environments |
| Phase 8: CI/CD and Release | README.md#12-phase-8-cicd-and-release |
| Phase 9: Cutover and Decommissioning | README.md#13-phase-9-cutover-and-decommissioning |
| Troubleshooting and Common Pitfalls | README.md#14-troubleshooting-and-common-pitfalls |
| Scaffolding Phase / Dependencies Phase / Plugin Mapping Phase / Testing Phase / WAR Packaging Phase / Multi-Module Phase / CI/CD Phase (verify-errors phase groups) | derived groupings over the sections above, not distinct guide headings |

## Supplementary

- Maven Central artifact search: https://central.sonatype.com/ (referenced in Phase 2 for jar identification)
- Maven POM model reference: https://maven.apache.org/pom.html (standard layout, scopes, properties)
