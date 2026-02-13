# Nebula DSL - Refactoring Execution Plan

**Created:** 2026-02-13
**Status:** 🟡 In Progress

---

## Executive Summary

**Total Files to Refactor:** ~50 files
**Total Lines to Remove:** ~800 lines (duplicates + dead code)
**Total Execution Time:** ~20-30 minutes
**Risk Level:** Low-Medium (mostly cleanup and consolidation)

---

## Tier 1: Quick Wins ⚡ (5-10 minutes)

### ✅ Task 1.1: Delete Backup Files
**Time:** 1 minute
**Risk:** None (git history preserved)

**Action:** Delete 21 `.bak*` files
```bash
find dsl/nebula/src/cli -name "*.bak*" -delete
```

**Files:**
- `generators/microservices/repository/repository-interface-generator.ts.bak`
- `generators/microservices/types.ts.bak`
- `generators/microservices/service/service-definition-generator.ts.bak`
- `generators/microservices/events/event-types.ts.bak`
- `generators/coordination/config/config-types.ts.bak`
- `generators/coordination/config/integration-generator.ts.bak`
- `generators/coordination/webapi/webapi-types.ts.bak`
- `generators/sagas/causal-entity-generator.ts.bak`
- `generators/sagas/saga-generator.ts.bak`
- `generators/common/template-context-builder.ts.bak`
- `generators/common/types.ts.bak`
- `generators/common/extractors/template-data-types.ts.bak`
- `generators/common/extractors/template-data-base.ts.bak{2,5,6}`
- `generators/common/extractors/template-data-extractor.ts.bak{3,5}`
- `generators/common/exception-generator.ts.bak`
- `generators/validation/validation-types.ts.bak`

**Status:** ✅ Complete (2026-02-13)

**Result:** 21 backup files deleted successfully

---

### ✅ Task 1.2: Delete Dead Code
**Time:** 2 minutes
**Risk:** None (unused files, functionality replaced)

**Files to Delete:**

1. **`utils/package-name-refactor.ts`** (195 lines)
   - Never imported anywhere
   - Functionality replaced by `PackageNameBuilder`
   - Was a migration tool for hardcoded package refactoring

2. **`utils/batch-processor.ts`** (353 lines)
   - Never imported anywhere
   - Sophisticated but unused batch operations
   - All file operations use `FileWriter` directly

**Total Lines Removed:** 548 lines

**Status:** ⏳ Pending

---

### ✅ Task 1.3: Rename Validation Classes
**Time:** 2 minutes
**Risk:** Low (simple rename + import updates)

**Changes:**

1. **Rename `ValidationSystem` → `AggregateValidator`**
   - File: `generators/validation/validation-system.ts`
   - Purpose: Validates DSL aggregates (no root entity, duplicates, property names)

2. **Rename `Validator` → `TemplateValidator`**
   - File: `generators/validation/validator.ts`
   - Purpose: Validates Handlebars template syntax

3. **Update Imports**
   - File: `engine/generator-registry.ts`

**Benefit:** Clear naming, no confusion about responsibilities

**Status:** ⏳ Pending

---

### ✅ Task 1.4: Extract String Utilities
**Time:** 3-5 minutes
**Risk:** Low (pure function extraction)

**Problem:** `capitalize()` implemented **8+ times independently**

**Files with duplicate code:**
```
generators/coordination/functionalities-crud-generator.ts
generators/coordination/functionalities-collection-generator.ts
generators/coordination/functionalities-method-generator.ts
generators/coordination/integration-generator.ts
generators/coordination/functionalities-generator.ts
generators/coordination/functionalities-imports-builder.ts
generators/coordination/event-processing-generator.ts
generators/microservices/repository/repository-interface-generator.ts
generators/microservices/entity/builders/inter-invariant-builder.ts
generators/microservices/events/builders/event-context-builder.ts
generators/sagas/saga-crud-generator.ts
generators/sagas/saga-collection-generator.ts
generators/sagas/saga-functionality-generator.ts
generators/sagas/saga-workflow-generator.ts
generators/sagas/saga-event-processing-generator.ts
generators/sagas/causal-entity-generator.ts
+ OrchestrationBase Handlebars helper
```

**Solution:**

1. **Create `utils/string-utils.ts`**
```typescript
export class StringUtils {
    static capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    static lowercase(str: string): string {
        if (!str) return '';
        return str.charAt(0).toLowerCase() + str.slice(1);
    }

    static pascalCase(str: string): string {
        if (!str) return '';
        return str
            .split(/[-_\s]+/)
            .map(word => this.capitalize(word))
            .join('');
    }

    static camelCase(str: string): string {
        const pascal = this.pascalCase(str);
        return this.lowercase(pascal);
    }
}
```

2. **Replace all duplicate implementations**
   - Remove local `capitalize()` methods
   - Add import: `import { StringUtils } from '../../utils/string-utils';`
   - Replace calls: `capitalize(str)` → `StringUtils.capitalize(str)`

**Lines Removed:** ~100 lines of duplication

**Status:** ⏳ Pending

---

## Tier 2: High-Impact Refactoring 💪 (10-15 minutes)

### ✅ Task 2.1: Consolidate CRUD Generation
**Time:** 5-7 minutes
**Risk:** Medium (affects 3 generator paths)

**Problem:** **60-80% code duplication** across 3 CRUD generators

**Files with duplicate logic:**
1. `generators/microservices/service/default/crud-generator.ts`
2. `generators/coordination/functionalities-crud-generator.ts`
3. `generators/sagas/saga-crud-generator.ts`

**Common duplicated code:**
- Method name generation (create, read, update, delete, findAll)
- DTO type resolution
- Parameter building
- Method body templates
- Cross-aggregate reference detection

**Solution:**

1. **Create `generators/common/crud-base-generator.ts`**
```typescript
export abstract class CrudBaseGenerator {
    protected generateCreateMethod(aggregate: Aggregate, options: any): string {
        const methodName = `create${aggregate.name}`;
        const dtoType = `Create${aggregate.name}RequestDto`;
        // ... shared logic
    }

    protected generateFindByIdMethod(aggregate: Aggregate): string {
        const methodName = `get${aggregate.name}ById`;
        // ... shared logic
    }

    protected generateUpdateMethod(aggregate: Aggregate): string {
        const methodName = `update${aggregate.name}`;
        // ... shared logic
    }

    protected generateDeleteMethod(aggregate: Aggregate): string {
        const methodName = `delete${aggregate.name}`;
        // ... shared logic
    }

    protected generateFindAllMethod(aggregate: Aggregate): string {
        const methodName = `getAll${aggregate.name}s`;
        // ... shared logic
    }

    protected findCrossAggregateReferences(aggregate: Aggregate): Entity[] {
        // ... shared logic
    }

    protected resolveTypeFromReference(entity: Entity, field: string): string {
        // ... shared logic
    }
}
```

2. **Refactor existing generators to extend base**
```typescript
// service/default/crud-generator.ts
export class ServiceCrudGenerator extends CrudBaseGenerator {
    // Only service-specific logic
}

// coordination/functionalities-crud-generator.ts
export class FunctionalitiesCrudGenerator extends CrudBaseGenerator {
    // Only functionalities-specific logic
}

// sagas/saga-crud-generator.ts
export class SagaCrudGenerator extends CrudBaseGenerator {
    // Only saga-specific logic
}
```

**Lines Removed:** ~300 lines of duplication

**Benefit:** Single source of truth for CRUD logic, consistent behavior

**Status:** ⏳ Pending

---

### ✅ Task 2.2: Reorganize Coordination Layer
**Time:** 5-8 minutes
**Risk:** Medium (many imports to update)

**Problem:** Over-granulated with **too many small files**

**Current structure:**
```
coordination/
├── functionalities-crud-generator.ts          (318 lines)
├── functionalities-collection-generator.ts    (256 lines)
├── functionalities-method-generator.ts        (206 lines)
├── functionalities-imports-builder.ts         (213 lines)
├── functionalities-generator.ts               (340 lines) - orchestrator
├── event-processing-generator.ts              (383 lines)
├── webapi-base-generator.ts
├── dto-generator.ts
├── controller-generator.ts
└── config/
    ├── application-config-generator.ts
    ├── configuration-generator.ts
    ├── database-config-generator.ts
    ├── logging-config-generator.ts
    └── integration-generator.ts
```

**Proposed structure:**
```
coordination/
├── functionalities/
│   ├── functionalities-orchestrator.ts    (main entry)
│   ├── crud-generator.ts                  (merged: crud+method+collection)
│   ├── import-builder.ts
│   └── builders/
│       ├── method-builder.ts
│       └── collection-builder.ts
├── event-processing/
│   └── event-processing-generator.ts
├── webapi/
│   ├── webapi-base-generator.ts
│   ├── dto-generator.ts
│   └── controller-generator.ts
└── config/
    ├── configuration-orchestrator.ts
    └── generators/
        ├── application-config-generator.ts
        ├── database-config-generator.ts
        ├── logging-config-generator.ts
        └── integration-generator.ts
```

**Changes:**
1. Create new directory structure
2. Move files to appropriate subdirectories
3. Merge `functionalities-crud-generator.ts` + `functionalities-method-generator.ts` + `functionalities-collection-generator.ts` → `crud-generator.ts`
4. Update all imports in dependent files

**Benefit:** Clearer organization, easier to navigate

**Status:** ⏳ Pending

---

## Tier 3: Polish & Consistency 🔧 (5-10 minutes)

### ✅ Task 3.1: Standardize Template Rendering
**Time:** 3-5 minutes
**Risk:** Low (pattern replacement)

**Problem:** **Three different ways** of rendering templates

**Current inconsistency:**
```typescript
// Method 1: Direct Handlebars (in multiple files)
const compiled = Handlebars.compile(template, { noEscape: true });
const result = compiled(context);

// Method 2: OrchestrationBase.renderTemplate()
protected renderTemplate(template: string, context: any): string

// Method 3: TemplateManager singleton
TemplateManager.getInstance().renderTemplate(templatePath, context)
```

**Solution:** Use `TemplateManager` consistently everywhere

**Changes:**
1. Find all direct `Handlebars.compile()` calls
2. Replace with `TemplateManager.getInstance().renderTemplate()`
3. Ensure all generators have access to TemplateManager

**Benefit:** Single rendering path, consistent error handling, easier debugging

**Status:** ⏳ Pending

---

### ✅ Task 3.2: Fix Old Syntax References
**Time:** 2-3 minutes
**Risk:** Low (comments and variable names only)

**Problem:** Old "uses dto" terminology still in comments/variables

**Files with old references:**
- `entity/builders/import-scanner.ts:29` - "Resolves DTO field mappings from entity's uses dto declarations"
- `entity/constructors.ts` - Multiple "uses" references in comments
- Type properties: `usesPersistence`, `usesLocalDateTime`

**Changes:**
1. Update comments: "uses dto" → "from mapping" or "cross-aggregate reference"
2. Rename property flags:
   - `usesPersistence` → `includesPersistence`
   - `usesLocalDateTime` → `includesLocalDateTime`
   - `usesOptional` → `includesOptional`

**Benefit:** Consistent terminology with new DSL syntax

**Status:** ⏳ Pending

---

## Summary Statistics

### Before Refactoring
- Total Files: 165
- Backup Files: 21
- Dead Code Files: 2
- Duplicate `capitalize()`: 16+ implementations
- CRUD Duplication: ~300 lines
- Total Bloat: ~1000+ lines

### After Refactoring
- Total Files: ~142 (-23)
- Backup Files: 0 (-21)
- Dead Code Files: 0 (-2)
- Duplicate `capitalize()`: 1 (StringUtils)
- CRUD Duplication: 0 (CrudBaseGenerator)
- Total Lines Removed: ~950 lines

### Metrics
- **Code Reduction:** ~950 lines removed
- **Duplication Reduction:** ~400 lines
- **Dead Code Removal:** ~550 lines
- **Better Organization:** 3 new directories, clearer structure
- **Consistency:** Standardized patterns across generators

---

## Execution Order

### Session 1: Quick Wins (5-10 minutes) ⚡
1. Delete backup files (1 min)
2. Delete dead code (2 min)
3. Rename validation classes (2 min)
4. Extract StringUtils (3-5 min)

**Total:** ~8-10 minutes
**Impact:** Clean codebase, better naming, less duplication

---

### Session 2: Consolidation (10-15 minutes) 💪
5. Consolidate CRUD generation (5-7 min)
6. Reorganize coordination layer (5-8 min)

**Total:** ~10-15 minutes
**Impact:** Significantly reduced duplication, better structure

---

### Session 3: Polish (5-10 minutes) 🔧
7. Standardize template rendering (3-5 min)
8. Fix old syntax references (2-3 min)

**Total:** ~5-8 minutes
**Impact:** Consistency, modern terminology

---

## Total Timeline

**Estimated Total Time:** 20-30 minutes
**Risk Assessment:** Low-Medium
**Recommended Approach:** Execute in 3 sessions with build/test verification between each

---

## Verification Steps

After each session:
1. Run TypeScript build: `npm run build`
2. Verify no compilation errors
3. Test code generation: `./bin/cli.js generate ../abstractions/answers/`
4. Verify generated code compiles: `cd ../../applications/answers && mvn compile`

---

## Rollback Plan

All changes tracked in git:
```bash
# If issues arise, rollback to before refactoring
git reset --hard HEAD~N  # where N = number of refactoring commits
```

---

**Status Legend:**
- ⏳ Pending
- 🔄 In Progress
- ✅ Complete
- ❌ Blocked

**Last Updated:** 2026-02-13
