# Phase: Testing Migration

Update test code for Jakarta EE namespace and EAP 8 behavioral changes.

## Steps

1. Read `references/api-map.md` — apply all `javax.*` → `jakarta.*` changes to test source directories
2. Read `references/pattern-map.md` — filter for test-relevant rows

### Jakarta namespace in tests
- Apply the same `javax.*` → `jakarta.*` import migration to all test classes
- Update test resource files (`test-persistence.xml`, `test-web.xml`, `beans.xml` in test resources) with new namespaces
- Update Arquillian deployment descriptors if using Arquillian

### CDI bean discovery in tests
- Test `beans.xml` files (e.g., `src/test/resources/META-INF/beans.xml`) must also be updated
- If empty, add `bean-discovery-mode="all"` or add CDI annotations to test beans

### Security testing
- Remove any test setup using PicketBox or PicketLink APIs
- Update security test configuration to use Elytron security domains
- Update JAAS login module tests to use `jaas-realm` configuration

### Hibernate testing
- Update HQL queries in tests for Hibernate ORM 6 syntax changes
- Update Criteria API usage in tests
- Verify entity mapping changes

### Integration test deployment
- Ensure Arquillian or other integration test frameworks use EAP 8-compatible versions
- Update `arquillian.xml` container configuration for EAP 8
- Verify test datasource connectivity against EAP 8

### `@Stateful` EJB tests
- Add load tests for `@Stateful` EJBs if used — behavioral changes under concurrent access
- Test `SessionScoped` injection in `RequestScoped` stateful beans
- Test extended persistence contexts in `@Stateful` beans

3. Run the full test suite

## Build gate

Run `mvn test`. Test failures may indicate:
- Remaining `javax.*` imports in test code
- CDI bean discovery failures from empty `beans.xml`
- Hibernate 6 query syntax incompatibilities
- Security configuration mismatches after Elytron migration
