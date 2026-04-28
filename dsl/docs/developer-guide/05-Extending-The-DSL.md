# Extending the DSL

This chapter provides step-by-step guidance for common extension tasks: adding new grammar constructs, creating generators, and testing changes.

## Adding a New Property Type

**Example:** adding a `BigDecimal` type.

1. **Grammar** (`nebula.langium`): add to `PrimitiveType`:
   ```langium
   PrimitiveType:
       typeName=('Integer' | 'Long' | 'Float' | 'Double' | 'String' | 'Boolean' | 'LocalDateTime' | 'Object' | 'BigDecimal');
   ```

2. **Type resolver** (`generators/common/unified-type-resolver.ts`): add the Java mapping:
   ```typescript
   case 'BigDecimal': return 'java.math.BigDecimal';
   ```

3. **Rebuild and test:**
   ```bash
   npm run langium:generate && npm run build
   ```

## Adding a New Action Statement

**Example:** adding an `emit` statement for inline event firing.

1. **Grammar**: add to `ActionStatement` union and define the rule:
   ```langium
   ActionStatement:
       CreateActionStatement | LoadActionStatement | FindActionStatement | AssignActionStatement | EmitActionStatement;

   EmitActionStatement:
       'emit' eventType=ID '(' (args+=ActionExpression (',' args+=ActionExpression)*)? ')';
   ```

2. **Regenerate parser**: `npm run langium:generate`

3. **Generator** (`service/default/action-method-generator.ts`): add a case in `renderActionStatements`:
   ```typescript
   case 'EmitActionStatement': {
       const eventType = stmt.eventType;
       const args = (stmt.args || []).map(a => this.renderExpression(a));
       lines.push(`        unitOfWorkService.registerEvent(new ${eventType}(${args.join(', ')}), unitOfWork);`);
       break;
   }
   ```

4. **Build and test**: `npm run build`, then regenerate and run Spock tests.

## Adding a New Top-Level Block

**Example:** adding a `Migration` block for schema evolution.

1. **Grammar**: add to `Model` and define the rule:
   ```langium
   entry Model:
       (aggregates+=Aggregate | sharedEnums+=SharedEnums | sagaStatesBlocks+=SagaStatesBlock
        | topLevelWorkflows+=TopLevelWorkflow | migrations+=MigrationBlock)*
       (exceptions=ExceptionMessages)?;

   MigrationBlock:
       'Migration' name=ID '{' steps+=MigrationStep* '}';
   ```

2. **Regenerate**: `npm run langium:generate`

3. **Generator**: create `generators/coordination/migrations/migration-generator.ts`

4. **Wire into pipeline** (`engine/code-generator.ts`): add after aggregate loop:
   ```typescript
   const migrations = models.flatMap(m => (m as any).migrations || []);
   for (const migration of migrations) {
       // generate migration files
   }
   ```

5. **Build and test.**

## Adding a Validator

1. Create a validator class in `src/language/validation/`:
   ```typescript
   export class MigrationValidator {
       checkMigration(migration: MigrationBlock, accept: ValidationAcceptor): void {
           if (!migration.steps || migration.steps.length === 0) {
               accept('warning', 'Migration has no steps', { node: migration, property: 'name' });
           }
       }
   }
   ```

2. Register in `validation/index.ts`:
   ```typescript
   const checks: ValidationChecks<NebulaAstType> = {
       // ... existing checks
       MigrationBlock: validator.checkMigration,
   };
   ```

3. Instantiate in `validation/validators.ts`.

## Modifying an Existing Generator

### Common patterns

**Adding a field to generated entities**: modify `EntityGenerator` or the entity builders in `entity/builders/`.

**Changing CRUD service logic**: modify the specific operation generator in `service/crud/` (e.g., `crud-delete-generator.ts` for delete).

**Changing saga enforcement**: modify the operation generators in `sagas/operations/` (e.g., `saga-update-generator.ts` for update saga state).

**Adding imports to generated services**: modify `service/default/structure-generator.ts` which builds the import section.

### Finding the right file

To find which generator produces a specific Java file:
1. Check the file's package path — it maps to a generator category
2. Search for a unique string from the generated code: `grep -rn "that string" src/cli/generators/`

## Testing Changes

### Full test cycle

```bash
cd dsl/nebula

# 1. Rebuild DSL
npm run langium:generate && npm run build

# 2. Regenerate all projects and run tests
cd ../..
bash dsl/tests/generate.sh
bash dsl/tests/test-spock.sh
bash dsl/tests/test-crud.sh
```

### Fast iteration

For quick feedback while developing a generator:
```bash
npm run build && ./bin/cli.js generate ../abstractions/showcase/
```

Then check the generated output in `applications/showcase/src/main/java/`.

### Writing Spock tests

Test specs live in `dsl/tests/spock/<project>/`. They're copied into the generated application's `src/test/groovy/` during `generate.sh`. Example:

```groovy
@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class MyFeatureSpec extends Specification {
    @Autowired MyService myService
    @Autowired UnitOfWorkService unitOfWorkService

    def "feature works"() {
        when:
            def uow = unitOfWorkService.createUnitOfWork("test")
            def result = myService.myMethod(params, uow)
            unitOfWorkService.commit(uow)
        then:
            result != null
    }
}
```

### CRUD tests

Shell scripts in `dsl/tests/crud/<project>.sh` test REST endpoints via `curl`. They run against a booted application instance.

## Naming Conventions

Follow these conventions when creating generators:

| Variable | Meaning | Example |
|----------|---------|---------|
| `aggregateName` | Original case from AST | `"User"` |
| `lowerAggregate` | Lowercase (for packages/paths) | `"user"` |
| `capitalizedAggregate` | Capitalized (for class names) | `"User"` |
| `upperAggregate` | Uppercase (for enum values) | `"USER"` |
| `projectName` | From config/directory | `"showcase"` |

## Key Files to Know

| When you want to... | Look at... |
|---------------------|-----------|
| Add a grammar rule | `src/language/nebula.langium` |
| Change what CRUD generates | `generators/microservices/service/crud/` |
| Change entity structure | `generators/microservices/entity/builders/` |
| Change REST endpoints | `generators/coordination/webapi/controller-generator.ts` |
| Change saga behaviour | `generators/sagas/operations/` |
| Change workflow generation | `generators/coordination/workflows/top-level-workflow-generator.ts` |
| Add a validator | `src/language/validation/` |
| Change cross-file type inference | `utils/aggregate-helpers.ts` |
| Change action body translation | `generators/microservices/service/default/action-method-generator.ts` |
| Add a template | `templates/` + register in the generator |

---

**Previous:** [04-Generators-Templates](04-Generators-Templates.md)
