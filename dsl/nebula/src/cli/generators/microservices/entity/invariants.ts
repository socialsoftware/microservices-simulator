import { Entity } from "../../../../language/generated/ast.js";
import { capitalize } from "../../../utils/generator-utils.js";
import { ImportRequirements } from "./types.js";

export function generateInvariants(entity: Entity): { code: string, imports?: ImportRequirements } {
    const hasInvariants = entity.invariants && entity.invariants.length > 0;

    if (!hasInvariants) {
        const verifyMethod = `
    @Override
    public void verifyInvariants() {
        // No invariants defined
    }`;

        return {
            code: verifyMethod,
            imports: undefined
        };
    }

    const imports: ImportRequirements = {};

    // Generate individual invariant methods
    const invariantMethods = entity.invariants.map((invariant: any, index: number) => {
        const methodName = `invariant${capitalize(invariant.name)}`;

        // Convert DSL expression to Java code
        const condition = getInvariantConditionText(invariant);

        // Only add divider comment before the first invariant
        const dividerComment = index === 0 ? '\n    // ============================================================================\n    // INVARIANTS\n    // ============================================================================\n' : '';

        return `${dividerComment}
    private boolean ${methodName}() {
        return ${condition};
    }`;
    }).join('\n');

    // Generate individual checks with custom messages (error messages are now required)
    const individualChecks = entity.invariants.map((invariant: any) => {
        const methodName = `invariant${capitalize(invariant.name)}()`;
        // Remove quotes from the error message string literal
        const message = invariant.errorMessage.replace(/^["']|["']$/g, '');
        return `        if (!${methodName}) {
            throw new SimulatorException(INVARIANT_BREAK, "${message}");
        }`;
    }).join('\n');

    const verifyMethod = `
    @Override
    public void verifyInvariants() {
${individualChecks}
    }`;

    // Add necessary imports
    imports.customImports = new Set([
        'import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;',
        'import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;'
    ]);

    return {
        code: invariantMethods + verifyMethod,
        imports
    };
}

function getInvariantConditionText(invariant: any): string {
    // Try to get the source text from the first condition
    if (invariant.conditions && invariant.conditions.length > 0) {
        const firstCondition = invariant.conditions[0];

        // Try to get the source text from the CST node
        const sourceText = firstCondition.expression?.$cstNode?.text;
        if (sourceText) {
            // Convert DSL property references to Java getter calls
            return convertDslToJava(sourceText.trim());
        }

        // Fallback: try to extract from the expression structure
        if (firstCondition.expression) {
            return convertExpressionToJava(firstCondition.expression);
        }
    }

    return 'true'; // Safe fallback
}

function convertDslToJava(dslText: string): string {
    // Simple conversions for common patterns
    let javaCode = dslText;

    // Handle quantifier expressions: forall/exists
    javaCode = handleQuantifierExpressions(javaCode);

    // Handle collection stream operations: allMatch, anyMatch, noneMatch
    javaCode = handleCollectionStreamOperations(javaCode);

    // Handle specific DSL patterns - these add "this." prefix to properties

    // 1. Handle temporal comparisons first
    if (javaCode.includes('.isBefore(') || javaCode.includes('.isAfter(')) {
        // Time comparison: startTime.isBefore(endTime) -> this.startTime.isBefore(this.endTime)
        javaCode = javaCode.replace(/(\w+)\.isBefore\((\w+)\)/g, (match, prop1, prop2) => {
            const left = prop1.startsWith('this.') ? prop1 : `this.${prop1}`;
            const right = prop2.startsWith('this.') ? prop2 : `this.${prop2}`;
            return `${left}.isBefore(${right})`;
        });
        javaCode = javaCode.replace(/(\w+)\.isAfter\((\w+)\)/g, (match, prop1, prop2) => {
            const left = prop1.startsWith('this.') ? prop1 : `this.${prop1}`;
            const right = prop2.startsWith('this.') ? prop2 : `this.${prop2}`;
            return `${left}.isAfter(${right})`;
        });
    }

    // 2. Handle unique checks
    if (javaCode.includes('.unique(')) {
        javaCode = javaCode.replace(/(\w+)\.unique\((\w+)\)/g, (match, collection, field) => {
            const coll = collection.startsWith('this.') ? collection : `this.${collection}`;
            const capitalizedField = capitalize(field);
            return `${coll}.stream().map(item -> item.get${capitalizedField}()).distinct().count() == ${coll}.size()`;
        });
    }

    // 3. Handle string length checks with null safety
    if (javaCode.includes('.length()')) {
        javaCode = javaCode.replace(/(\w+)\.length\(\)\s*(>|<|>=|<=|==|!=)\s*(\d+)/g, (match, prop, op, num) => {
            // Don't add this. if already present
            if (prop.includes('this')) {
                return match;
            }
            return `this.${prop} != null && this.${prop}.length() ${op} ${num}`;
        });
    }

    // 4. Handle collection size and isEmpty
    if (javaCode.includes('.size()')) {
        javaCode = javaCode.replace(/(\w+)\.size\(\)/g, (match, prop) => {
            const property = prop.startsWith('this.') ? prop : `this.${prop}`;
            return `${property}.size()`;
        });
    }

    if (javaCode.includes('.isEmpty()')) {
        javaCode = javaCode.replace(/(\w+)\.isEmpty\(\)/g, (match, prop) => {
            const property = prop.startsWith('this.') ? prop : `this.${prop}`;
            return `${property}.isEmpty()`;
        });
    }

    // 5. Handle simple property comparisons (!=, ==)
    // Only add "this." if not already present
    // Match patterns like: "propertyName != value" or "propertyName == value"
    // Use lookahead/lookbehind to avoid capturing characters, or handle start of string
    javaCode = javaCode.replace(/(^|[^\w.])(\w+)(\s*(?:!=|==)\s*)(\w+|null|true|false|\d+)/g,
        (match, before, prop, opWithWs, value) => {
            // Skip if prop is 'this' keyword
            if (prop === 'this') {
                return match;
            }
            // Skip if the character before was a dot (meaning it's already qualified like "this.prop")
            if (before === '.') {
                return match;
            }
            // Add this. prefix
            return `${before}this.${prop}${opWithWs}${value}`;
        });

    // 6. Handle comparison operators with properties on both sides (>, <, >=, <=)
    javaCode = javaCode.replace(/\b(\w+)\s*(>|<|>=|<=)\s*(\w+)/g,
        (match, leftProp, op, rightProp) => {
            // Skip if already qualified or if it's a number
            if (leftProp.includes('.') || rightProp.match(/^\d/)) {
                return match;
            }
            return `this.${leftProp} ${op} this.${rightProp}`;
        });

    return javaCode;
}

// Handle quantifier expressions: forall p : collection | condition
function handleQuantifierExpressions(javaCode: string): string {
    // Pattern: forall variable : collection | body
    const forallPattern = /forall\s+(\w+)\s*:\s*(\w+)\s*\|\s*([^;]+)/g;
    javaCode = javaCode.replace(forallPattern, (match, variable, collection, body) => {
        // Convert body to use lambda parameter instead of 'this'
        const lambdaBody = body.trim()
            .replace(/\bthis\./g, '')  // Remove 'this.' if present
            .replace(new RegExp(`\\b${variable}\\.`, 'g'), `${variable}.get`)  // p.field -> p.getField
            .replace(/\.(\w+)(?!\()/g, (_m: string, prop: string) => `.get${capitalize(prop)}()`);  // Add getters

        return `this.${collection}.stream().allMatch(${variable} -> ${lambdaBody})`;
    });

    // Pattern: exists variable : collection | body
    const existsPattern = /exists\s+(\w+)\s*:\s*(\w+)\s*\|\s*([^;]+)/g;
    javaCode = javaCode.replace(existsPattern, (match, variable, collection, body) => {
        const lambdaBody = body.trim()
            .replace(/\bthis\./g, '')
            .replace(new RegExp(`\\b${variable}\\.`, 'g'), `${variable}.get`)
            .replace(/\.(\w+)(?!\()/g, (_m: string, prop: string) => `.get${capitalize(prop)}()`);

        return `this.${collection}.stream().anyMatch(${variable} -> ${lambdaBody})`;
    });

    return javaCode;
}

// Handle collection stream operations: allMatch, anyMatch, noneMatch
function handleCollectionStreamOperations(javaCode: string): string {
    // Pattern: collection.allMatch(variable -> body)
    const allMatchPattern = /(\w+)\.allMatch\((\w+)\s*->\s*([^)]+)\)/g;
    javaCode = javaCode.replace(allMatchPattern, (match, collection, variable, body) => {
        const lambdaBody = body.trim()
            .replace(new RegExp(`\\b${variable}\\.`, 'g'), `${variable}.get`)
            .replace(/\.(\w+)(?!\()/g, (_m: string, prop: string) => `.get${capitalize(prop)}()`);

        return `this.${collection}.stream().allMatch(${variable} -> ${lambdaBody})`;
    });

    // Pattern: collection.anyMatch(variable -> body)
    const anyMatchPattern = /(\w+)\.anyMatch\((\w+)\s*->\s*([^)]+)\)/g;
    javaCode = javaCode.replace(anyMatchPattern, (match, collection, variable, body) => {
        const lambdaBody = body.trim()
            .replace(new RegExp(`\\b${variable}\\.`, 'g'), `${variable}.get`)
            .replace(/\.(\w+)(?!\()/g, (_m: string, prop: string) => `.get${capitalize(prop)}()`);

        return `this.${collection}.stream().anyMatch(${variable} -> ${lambdaBody})`;
    });

    // Pattern: collection.noneMatch(variable -> body)
    const noneMatchPattern = /(\w+)\.noneMatch\((\w+)\s*->\s*([^)]+)\)/g;
    javaCode = javaCode.replace(noneMatchPattern, (match, collection, variable, body) => {
        const lambdaBody = body.trim()
            .replace(new RegExp(`\\b${variable}\\.`, 'g'), `${variable}.get`)
            .replace(/\.(\w+)(?!\()/g, (_m: string, prop: string) => `.get${capitalize(prop)}()`);

        return `this.${collection}.stream().noneMatch(${variable} -> ${lambdaBody})`;
    });

    return javaCode;
}

// Helper function to convert DSL expressions to Java code
function convertExpressionToJava(expression: any): string {
    if (!expression) {
        return 'true';
    }

    // Handle different expression types
    if (expression.$type === 'BooleanExpression') {
        const left = convertExpressionToJava(expression.left);
        if (expression.right) {
            const right = convertExpressionToJava(expression.right);
            const op = expression.op === '&&' ? '&&' : '||';
            return `${left} ${op} ${right}`;
        }
        return left;
    }

    if (expression.$type === 'Comparison') {
        const left = convertExpressionToJava(expression.left);
        if (expression.right) {
            const right = convertExpressionToJava(expression.right);
            return `${left} ${expression.op} ${right}`;
        }
        return left;
    }

    if (expression.$type === 'PropertyChainExpression') {
        return convertPropertyChainToJava(expression);
    }

    if (expression.$type === 'TimeExpression') {
        if (expression.date && expression.operation) {
            const dateExpr = convertExpressionToJava(expression.date);
            const arg = convertExpressionToJava(expression.argument);
            return `${dateExpr}.${expression.operation}(${arg})`;
        }
    }

    if (expression.$type === 'UniqueCheckExpression') {
        const collection = convertExpressionToJava(expression.collection);
        return `${collection}.stream().map(item -> item.get${capitalize(expression.property)}()).distinct().count() == ${collection}.size()`;
    }

    if (expression.$type === 'CollectionOperationExpression') {
        const collection = convertExpressionToJava(expression.collection);
        return `${collection}.${expression.operation}()`;
    }

    if (expression.$type === 'PropertyReference') {
        return `this.${expression.name}`;
    }

    if (expression.$type === 'LiteralExpression') {
        return expression.value;
    }

    // Fallback
    return 'true';
}

function convertPropertyChainToJava(expression: any): string {
    let result = `this.${expression.head.name}`;

    // Handle method calls and property access in the chain
    let current = expression;
    while (current && current.receiver) {
        if (current.$type === 'MethodCall') {
            result += `.${current.method}()`;
        } else if (current.$type === 'PropertyAccess') {
            result += `.get${capitalize(current.member)}()`;
        }
        current = current.receiver;
    }

    return result;
}
