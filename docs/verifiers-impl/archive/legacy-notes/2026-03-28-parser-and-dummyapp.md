# 2026-03-28

## Removed
- `verifiers/src/main/java/.../GreeterService.java` — placeholder Spring `@Service` with a single `greet()` method
- `verifiers/src/test/groovy/.../GreeterServiceSpec.groovy` — Spock test for the above

## Created

### `ApplicationsFileTreeParser.java`
**Path:** `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/ApplicationsFileTreeParser.java`

Utility that recursively walks a directory tree and builds FQN→Path maps for Java source and Groovy test files.

- `parse(Path applicationsRoot)` — uses `Files.walkFileTree` over the given root. For each file, locates the `src/main/java/` or `src/test/groovy/` marker in the absolute path, strips the prefix, and converts the remaining path (minus extension) into a dot-separated FQN.
- `javaClassPaths` — `Map<String, Path>` populated for `.java` files found under `src/main/java/`
- `groovyClassPaths` — `Map<String, Path>` populated for `.groovy` files found under `src/test/groovy/`

### Dummy app (`applications/dummyapp/`)
A minimal test fixture to exercise the parser.

- `src/main/java/com/example/dummyapp/DummyApp.java` — `@SpringBootApplication` entry point
- `src/main/java/com/example/dummyapp/DummyAggregate.java` — `@Entity` extending `Aggregate`; implements `verifyInvariants()` (empty) and `getEventSubscriptions()` (empty set). Has a single `label` field.
- `src/test/groovy/com/example/dummyapp/DummyAppSpec.groovy` — one Spock spec creating a `DummyAggregate`

### `ApplicationsFileTreeParserSpec.groovy`
**Path:** `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/ApplicationsFileTreeParserSpec.groovy`

Three Spock specs validating the parser against the dummy app:

- `parser discovers dummy app java and groovy files` — asserts exactly `DummyAggregate` and `DummyApp` in the java map, `DummyAppSpec` in the groovy map
- `parser resolves correct file paths` — checks that each entry's path string ends with the expected relative path
- `parser rejects non-directory path` — expects `IllegalArgumentException` when given a nonexistent path
