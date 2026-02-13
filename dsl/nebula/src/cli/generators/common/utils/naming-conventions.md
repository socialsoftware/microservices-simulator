# Naming Conventions for Code Generators

This document defines standard naming conventions used across all code generators to ensure consistency and prevent variable shadowing.

## Aggregate Name Variables

When working with `Aggregate` objects, use these naming patterns:

```typescript
// ✅ CORRECT PATTERN
const aggregateName = aggregate.name;              // Original case (e.g., "User", "Quiz")
const lowerAggregate = aggregateName.toLowerCase(); // Lowercase (e.g., "user", "quiz")
const capitalizedAggregate = StringUtils.capitalize(aggregateName);  // Capitalized

// ❌ INCORRECT PATTERN (causes shadowing)
const aggregateName = aggregate.name.toLowerCase();  // WRONG - aggregateName should be original case
const capitalizedAggregate = aggregate.name;         // WRONG - use StringUtils.capitalize()
```

### Standard Variable Names

| Variable Name | Purpose | Example Value |
|--------------|---------|---------------|
| `aggregateName` | Original aggregate name (from AST) | `"User"`, `"Quiz"` |
| `lowerAggregate` | Lowercase aggregate name (for packages/paths) | `"user"`, `"quiz"` |
| `capitalizedAggregate` | Capitalized aggregate name (for class names) | `"User"`, `"Quiz"` |

## Entity Name Variables

Similar pattern for entities:

```typescript
// ✅ CORRECT
const entityName = entity.name;                  // Original case
const lowerEntity = entityName.toLowerCase();     // Lowercase
const capitalizedEntity = StringUtils.capitalize(entityName);  // Capitalized
```

## Project Name Variables

```typescript
// ✅ CORRECT
const projectName = options.projectName;          // Original case
const lowerProject = projectName.toLowerCase();   // Lowercase (for packages)
const ProjectName = StringUtils.capitalize(projectName);  // Capitalized (for class names)
```

## Why This Matters

1. **Prevents Variable Shadowing**: Using consistent names prevents accidentally overwriting original values
2. **Improves Readability**: Clear naming makes it obvious what case each variable contains
3. **Reduces Bugs**: Consistent patterns reduce cognitive load and prevent case-related bugs
4. **Easier Refactoring**: Standardized names make search-and-replace operations safer

## Quick Reference

```typescript
// Template for any new generator method:
function generateSomething(aggregate: Aggregate, options: GenerationOptions): string {
    // 1. Extract names with original case
    const aggregateName = aggregate.name;
    const projectName = options.projectName;

    // 2. Create lowercase variants for packages/paths
    const lowerAggregate = aggregateName.toLowerCase();
    const lowerProject = projectName.toLowerCase();

    // 3. Create capitalized variants for class names (if different from original)
    const capitalizedAggregate = StringUtils.capitalize(aggregateName);

    // 4. Use variables consistently
    const packageName = `${basePackage}.${lowerProject}.microservices.${lowerAggregate}`;
    const className = `${capitalizedAggregate}Service`;

    return template;
}
```

## Common Mistakes to Avoid

❌ `const aggregateName = aggregate.name.toLowerCase();` - Shadowing original case
❌ `const name = aggregate.name;` - Too generic, use `aggregateName`
❌ `const aggName = aggregate.name;` - Abbreviations reduce readability
❌ `const Aggregate = aggregate.name;` - Capital variable names reserved for types/classes

## ESLint Rules (Future)

Consider adding these ESLint rules to enforce conventions:
- No variable shadowing
- Enforce naming patterns for aggregate/entity/project variables
- Require explicit `toLowerCase()` calls (no implicit case changes)
