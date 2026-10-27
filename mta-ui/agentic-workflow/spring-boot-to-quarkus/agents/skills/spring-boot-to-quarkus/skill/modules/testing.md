# Phase: Testing Migration

Replace Spring Boot test annotations, frameworks, and patterns with Quarkus equivalents.

## Steps

1. Read `references/api-map.md` — filter for rows where `source_section` is "Testing"
2. Read `references/dependency-map.md` — filter for test-scoped dependencies

### Test dependencies

3. Replace test dependencies:
   - `spring-boot-starter-test` → `quarkus-junit5` + `rest-assured`
   - `spring-security-test` → `quarkus-test-security`
   - Add `quarkus-junit5-mockito` for `@InjectMock` and `@InjectSpy` support

### Test annotations

4. Replace `@SpringBootTest` → `@QuarkusTest`:
   - Import `io.quarkus.test.junit.QuarkusTest`
   - Remove `@ExtendWith(SpringExtension.class)` if present (not needed with `@QuarkusTest`)

5. Replace `@MockBean` → `@InjectMock`:
   - Import `io.quarkus.test.InjectMock`
   - For `@Singleton` beans, add `@MockitoConfig(convertScopes = true)` on the test class

6. Replace `@SpyBean` → `@InjectSpy`:
   - Import `io.quarkus.test.InjectSpy`

7. Replace `@Autowired` → `@Inject` in test classes

### HTTP testing

8. Replace MockMvc with REST Assured:
   - Remove `@AutoConfigureMockMvc` annotation
   - Replace `mockMvc.perform(get("/path"))` with `given().when().get("/path")`
   - Replace `.andExpect(status().isOk())` with `.then().statusCode(200)`
   - Replace `.andExpect(jsonPath("$.field").value("val"))` with `.body("field", equalTo("val"))`

### Slice tests

9. Replace slice test annotations:
   - `@DataJpaTest` → `@QuarkusTest` + `@Transactional` on test methods
   - `@WebMvcTest` → `@QuarkusTest` + `@InjectMock` for dependencies

### Test profiles

10. Replace `@ActiveProfiles("test")`:
    - Create a class implementing `QuarkusTestProfile`
    - Override `getConfigOverrides()` to set test-specific config
    - Annotate test with `@TestProfile(MyTestProfile.class)`

### Run tests

11. Run the full test suite: `mvn test`

## Build gate

Run `mvn test`. Test failures may indicate:
- Missing `@InjectMock` where `@MockBean` was used
- REST Assured syntax errors from MockMvc conversion
- CDI scope issues with mocked beans
