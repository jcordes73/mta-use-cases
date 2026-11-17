# Custom MTA Ruleset Example

This directory is a working example of how to create new (custom) analysis rules for
MTA, following the conventions of the [konveyor/rulesets](https://github.com/konveyor/rulesets)
repository — in particular its `.skills/create-new-rule.md` and `.skills/run-rule-tests.md`
skills and the `.cursor/rules/rule-format-and-conventions.mdc` / `test-format-and-conventions.mdc`
references it points at. The example targets the same migration path as this PoC:
**JBoss EAP 7 → JBoss EAP 8**.

## Layout

The ruleset mirrors the upstream `stable/<language>/<technology>/` layout:

```
custom-rulesets/
└── stable/
    └── java/
        └── eap8-custom/                              one directory = one target technology ruleset
            ├── ruleset.yaml                          ruleset name + description (required per directory)
            ├── eap8-custom-example.yaml              the rule file (one concern per file in production)
            └── tests/
                ├── eap8-custom-example.test.yaml     test file: tests/<rule-file-basename>.test.yaml
                └── data/                             test data, one folder per provider and test case
                    ├── builtin/
                    │   └── tc-hardcoded-ip/application.properties
                    └── java/
                        └── tc-commons-lang/          a small Maven project (pom.xml + sources)
```

## The two example rules

Both rules carry the labels used by this PoC's analysis profile (`konveyor.io/source=eap7`,
`konveyor.io/target=eap8+`), so they are only picked up for EAP 7 → EAP 8 analyses
(see [Define an Analysis Profile](../README.md#define-an-analysis-profile) in the main README).

| ruleID | `when` condition | What it flags |
|--------|------------------|---------------|
| `eap8-custom-00010` | `builtin.filecontent` | Hard-coded IPv4 addresses in `.properties`/`.yml`/`.yaml` files (cloud-readiness concern) |
| `eap8-custom-00020` | `java.dependency` | Apache Commons Lang 2.x (`commons-lang:commons-lang`), which must move to `commons-lang3` |

Rule IDs follow the upstream convention: `<ruleset-prefix>-<5-digit-number>`, incrementing
by 10 (`00010`, `00020`, ...) so rules can be inserted between existing ones.

## Second ruleset: Oracle JDK 8 → OpenJDK 17

[`stable/java/openjdk17-custom/`](stable/java/openjdk17-custom/) applies the same
workflow to a **Oracle JDK 8 → OpenJDK 17** migration. It deliberately fills the gaps
left by the upstream
[`stable/java/openjdk17`](https://github.com/konveyor/rulesets/tree/main/stable/java/openjdk17)
ruleset (which already covers applets, Nashorn, RMI Activation, Pack200 and the
Security Manager) rather than duplicating it:

| Rule file | ruleIDs | What it flags |
|-----------|---------|---------------|
| `jep320-removed-javaee-modules.yaml` | `...-00010` – `...-00040` | Imports from the Java EE modules removed in JDK 11 (JEP 320): `javax.xml.bind` (JAXB), `javax.xml.ws` (JAX-WS), `javax.activation` (JAF), `javax.annotation` (Common Annotations) |
| `sun-misc-internal-apis.yaml` | `...-00010` | `sun.misc.*`/`sun.reflect.*` internals — `BASE64Encoder` removed in JDK 9, the rest strongly encapsulated by JEP 396/403 on JDK 17 |
| `jvm-flags.yaml` | `...-00010` – `...-00020` | `-XX:+UseConcMarkSweepGC` (startup failure on JDK 14+, JEP 363) and the PermGen flags `-XX:PermSize`/`-XX:MaxPermSize` (no-ops since JDK 8, JEP 122) |

The rules carry the upstream source/target labels (`konveyor.io/source=openjdk11-`,
`konveyor.io/target=openjdk17+`), so Oracle JDK 8 applications match via the
`openjdk11-` (11 or earlier) and generic `openjdk` source labels, and the rules stay
valid for any JDK 17+ target — including this PoC's `openjdk21` analysis profile.

## Step-by-step: adding a new rule

1. **Create/choose the ruleset directory** — under `stable/<language>/<technology>/`
   (or `preview/...` for work in progress). Each directory needs a `ruleset.yaml`
   with `name` and `description`.
2. **Create a rule file** — one file per concern, named `<tech>-<concern>.yaml`.
3. **Add the rule entry** with `ruleID` (next number in the prefix sequence),
   `category` (`mandatory`, `optional`, or `potential`), `effort` (Fibonacci number),
   `labels` (source/target), `description`, and — strongly recommended — a `message`
   with before/after code examples plus `links` to migration docs. AI agents consume
   these messages, so make them actionable.
4. **Define the `when` condition**, provider-dependent:
   - `java.referenced` (with `pattern` + `location`, e.g. `IMPORT`, `METHOD_CALL`, `INHERITANCE`)
     or `java.dependency` (with `name`/`nameregex` and `upperbound`) for Java code;
   - `builtin.filecontent` (`pattern` + quoted `filePattern` with escaped dot, e.g.
     `".*\\.properties"`) or `builtin.file` / `builtin.xml` for configuration files;
   - combine with `and`/`or`, and use `as`/`from`/`filepaths` chaining for
     cross-element conditions (see the format reference below).
5. **Create the test file** — `tests/<rule-file-basename>.test.yaml` in the same
   directory, with `rulesPath` pointing at the rule file, one `providers` entry per
   provider used by the rules (`java`, `builtin`, ...) each with its own `dataPath`,
   and a `tests` entry per `ruleID` with test cases (`name`, `analysisParams` such as
   `mode: "source-only"`, and `hasIncidents` with `exactly`/`atLeast`/`atMost`).
6. **Add the test data** — one folder per test case under
   `tests/data/<provider-dataPath>/<test-case-name>/`. Java provider data is a
   buildable Maven project; builtin data is the config/text files to scan.
7. **Run the tests** (below) and iterate.

References: [analyzer-lsp rule schema](https://github.com/konveyor/analyzer-lsp/blob/main/docs/rules.md),
[konveyor/rulesets rule format](https://github.com/konveyor/rulesets/blob/main/.cursor/rules/rule-format-and-conventions.mdc),
[test format](https://github.com/konveyor/rulesets/blob/main/.cursor/rules/test-format-and-conventions.mdc).

## Running the tests with Kantra

The upstream CI runs rule tests with [Kantra](https://github.com/konveyor/kantra)
([rules-quickstart](https://github.com/konveyor/kantra/blob/main/docs/rules-quickstart.md#how-to-install-kantra)
for installation). Locally:

1. **Install Kantra** and make sure the container runtime (podman/docker) is available.
2. **Run a single test file** directly:

    ```bash
    kantra test custom-rulesets/stable/java/eap8-custom/tests/eap8-custom-example.test.yaml
    ```

3. **Run everything** — CI mirrors the expected layout into
   `~/.config/.kantra/rulesets/stable` / `.../preview` and then runs all tests, so
   arrange the rulesets accordingly and run:

    ```bash
    JVM_MAX_MEM=3072m RUN_LOCAL=1 kantra test ~/.config/.kantra/rulesets
    ```

A passing run prints the analysis incidents per test case and exits 0 when the
`hasIncidents` expectations match. This is the same flow the upstream
[local-ci workflow](https://github.com/konveyor/rulesets/blob/main/.github/workflows/local-ci.yaml)
runs on every push/PR (and nightly for all tests).

## Using the ruleset in MTA

- **Upload it to the Hub**: in the MTA web console, *Migration → Rulesets* lets you
  create/upload a custom ruleset (select the rule file, e.g. `eap8-custom-example.yaml`;
  MTA labels the ruleset with the `konveyor.io/source`/`konveyor.io/target` labels found
  in the rules).
- **Apply it in an analysis profile**: add a matching label selector — e.g.
  `konveyor.io/source=eap7` and `konveyor.io/target=eap8+` — to the analysis profile
  used for the application (see
  [Define an Analysis Profile](../README.md#define-an-analysis-profile)). The example
  rules' labels are chosen so they slot into this PoC's EAP 7 → EAP 8 profile without
  further changes.
