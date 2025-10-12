import { Entity } from "../../../../../../language/generated/ast.js";
import { capitalize } from "../../../../../utils/generator-utils.js";
import { ImportRequirements } from "./types.js";

export function generateInvariants(entity: Entity): { code: string, imports?: ImportRequirements } {
    if (!entity.invariants || entity.invariants.length === 0) {
        return { code: '', imports: undefined };
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
    public boolean ${methodName}() {
        return ${condition};
    }`;
    }).join('\n');

    // Generate verifyInvariants method that calls all individual invariants
    const invariantCalls = entity.invariants.map((invariant: any) =>
        `invariant${capitalize(invariant.name)}()`
    ).join('\n               && ');

    const verifyMethod = `
    @Override
    public void verifyInvariants() {
        if (!(${invariantCalls})) {
            throw new SimulatorException(INVARIANT_BREAK, getAggregateId());
        }
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

    // Handle specific DSL patterns
    if (javaCode.includes('.isBefore(') || javaCode.includes('.isAfter(')) {
        // Time comparison: startTime.isBefore(endTime) -> this.startTime.isBefore(this.endTime)
        javaCode = javaCode.replace(/\b(\w+)\.isBefore\((\w+)\)/g, 'this.$1.isBefore(this.$2)');
        javaCode = javaCode.replace(/\b(\w+)\.isAfter\((\w+)\)/g, 'this.$1.isAfter(this.$2)');
    } else if (javaCode.includes('.unique(')) {
        // Unique check: collection.unique(field) -> proper stream logic
        javaCode = javaCode.replace(/(\w+)\.unique\((\w+)\)/g,
            'this.$1.stream().map(item -> item.get${capitalize($2)}()).distinct().count() == this.$1.size()');
    } else if (javaCode.includes('.length()')) {
        // String length: name.length() > 0 -> this.name.length() > 0
        javaCode = javaCode.replace(/\b(\w+)\.length\(\)/g, 'this.$1.length()');
    } else if (javaCode.includes('!=') || javaCode.includes('==')) {
        // Simple comparisons: field != null -> this.field != null
        javaCode = javaCode.replace(/\b(\w+)\s*(!=|==)\s*(\w+)/g, 'this.$1 $2 $3');
    } else if (javaCode.includes('.isEmpty()')) {
        // Collection empty check: collection.isEmpty() -> this.collection.isEmpty()
        javaCode = javaCode.replace(/\b(\w+)\.isEmpty\(\)/g, 'this.$1.isEmpty()');
    } else {
        // Fallback: just add this. to property references
        javaCode = javaCode.replace(/\b(startTime|endTime|numberOfQuestions|cancelled|tournamentParticipants|tournamentCreator)\b/g, 'this.$1');
    }

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
