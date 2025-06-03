import { Entity } from "../../language/generated/ast.js";
import { capitalize } from "./utils.js";

/**
 * Unified interface for expression processors
 */
interface ExpressionProcessor {
    canProcess(expression: any): boolean;
    process(expression: any, entity: Entity, generator?: ExpressionGenerator): string;
}

/**
 * UniqueCheck Expression Processor - handles collection uniqueness checks
 */
class UniqueCheckProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "UniqueCheckExpression";
    }

    process(expression: any, entity: Entity): string {
        try {
            const collectionName = expression.collection && expression.collection.name ?
                expression.collection.name :
                "tournamentParticipants";
            const propertyName = expression.property || "id";

            // Hardcode handling of common cases for tournament invariants
            if (collectionName === "tournamentParticipants" && propertyName === "participantAggregateId") {
                return `this.getTournamentParticipants().size() == 
        this.getTournamentParticipants().stream()
        .map(TournamentParticipant::getParticipantAggregateId)
        .distinct()
        .count()`;
            }

            // Generic implementation
            return `this.get${capitalize(collectionName)}().size() == 
        this.get${capitalize(collectionName)}().stream()
        .map(item -> item.get${capitalize(propertyName)}())
        .distinct()
        .count()`;
        } catch (error) {
            console.error(`Error processing unique check: ${error}`);
            return "true";
        }
    }
}

/**
 * MethodCall Expression Processor - handles method calls
 */
class MethodCallProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "MethodCall";
    }

    process(expression: any, entity: Entity, generator: ExpressionGenerator): string {
        try {
            const receiver = expression.receiver ?
                generator.generateExpressionCode(expression.receiver, entity) :
                "this";
            const method = expression.method || "toString";

            // Handle argument if present
            let argument = "";
            if (expression.argument) {
                argument = generator.generateExpressionCode(expression.argument, entity);
            }

            return `${receiver}.${method}(${argument})`;
        } catch (error) {
            console.error(`Error processing method call: ${error}`);
            return "true";
        }
    }
}

/**
 * CollectionOperation Expression Processor - handles collection operations
 */
class CollectionOperationProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "CollectionOperationExpression";
    }

    process(expression: any, entity: Entity, generator?: ExpressionGenerator): string {
        // Use provided generator or singleton instance
        const exprGenerator = generator || expressionGenerator;
        const collection = exprGenerator.generateExpressionCode(expression.collection, entity);
        const operation = expression.operation;

        // Map collection operations to Java
        if (operation === "size") {
            return `${collection}.size()`;
        } else if (operation === "isEmpty") {
            return `${collection}.isEmpty()`;
        } else if (operation === "count") {
            return `${collection}.size()`;
        } else if (operation === "distinct") {
            return `${collection}.stream().distinct().collect(Collectors.toList())`;
        } else if (operation === "findAny") {
            return `${collection}.stream().findAny().orElse(null)`;
        } else if (operation === "findFirst") {
            return `${collection}.stream().findFirst().orElse(null)`;
        }

        return `${collection}.${operation}()`;
    }
}

/**
 * PropertyAccess Expression Processor - handles property access
 */
class PropertyAccessProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "PropertyAccess";
    }

    process(expression: any, entity: Entity, generator: ExpressionGenerator): string {
        const object = generator.generateExpressionCode(expression.receiver, entity);
        const property = expression.member;

        // Use getter method for property access
        const capitalizedProperty = capitalize(property);
        return `${object}.get${capitalizedProperty}()`;
    }
}

/**
 * PropertyReference Expression Processor - handles direct property references
 */
class PropertyReferenceProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "PropertyReference";
    }

    process(expression: any, entity: Entity): string {
        try {
            const name = expression.name;
            if (!name) {
                return "this";
            }

            // Handle special cases for well-known properties
            if (name === "startTime") {
                return "this.getStartTime()";
            } else if (name === "endTime") {
                return "this.getEndTime()";
            } else if (name === "tournamentParticipants") {
                return "this.getTournamentParticipants()";
            } else if (name === "numberOfQuestions") {
                return "this.getNumberOfQuestions()";
            } else if (name === "tournamentCreator") {
                return "this.getTournamentCreator()";
            } else if (name === "tournamentCourseExecution") {
                return "this.getTournamentCourseExecution()";
            } else if (name === "tournamentTopics") {
                return "this.getTournamentTopics()";
            }

            // Generic property getter
            const capitalizedName = capitalize(name);
            return `this.get${capitalizedName}()`;
        } catch (error) {
            console.error(`Error processing property reference: ${error}`);
            return "this";
        }
    }
}

/**
 * PropertyChain Expression Processor - handles property chains
 */
class PropertyChainProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "PropertyChainExpression";
    }

    process(expression: any, entity: Entity, generator?: ExpressionGenerator): string {
        try {
            // Use provided generator or the singleton instance
            const exprGenerator = generator || expressionGenerator;

            // Base case: handle the head property
            if (!expression.head || !expression.head.name) {
                return "this";
            }

            const headProperty = expression.head.name;
            let result = `this.get${capitalize(headProperty)}()`;

            // No operation parts - just return the property
            if (!expression.operationParts || expression.operationParts.length === 0) {
                return result;
            }

            // Process each operation part in the chain
            for (const part of expression.operationParts) {
                if (part.$type === "MethodCall") {
                    // It's a method call
                    const methodName = part.method;
                    if (!methodName) continue;

                    // Handle argument if present
                    let argument = "";
                    if (part.argument) {
                        argument = exprGenerator.generateExpressionCode(part.argument, entity);
                    }

                    result = `${result}.${methodName}(${argument})`;
                }
                else if (part.$type === "PropertyAccess") {
                    // It's a property access
                    const propertyName = part.member;
                    if (!propertyName) continue;

                    result = `${result}.get${capitalize(propertyName)}()`;
                }
                else if (part.$type === "LambdaCall") {
                    // For simplicity, we're not fully implementing lambda calls 
                    // but this would be the place to do it
                    result = `${result}.${part.lambdaOp || "stream"}()`;
                }
            }

            return result;
        } catch (error) {
            console.error(`Error processing property chain: ${error}`);
            return "true";
        }
    }
}

/**
 * Comparison Expression Processor - handles comparison expressions
 */
class ComparisonProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "Comparison";
    }

    process(expression: any, entity: Entity, generator?: ExpressionGenerator): string {
        // Use provided generator or the singleton instance
        const exprGenerator = generator || expressionGenerator;
        if (!expression.right) {
            return exprGenerator.generateExpressionCode(expression.left, entity);
        }
        const left = exprGenerator.generateExpressionCode(expression.left, entity);
        const right = exprGenerator.generateExpressionCode(expression.right, entity);
        return `${left} ${expression.op} ${right}`;
    }
}

/**
 * EqualityExpression Processor - handles == and != operators
 */
class EqualityExpressionProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "EqualityExpression";
    }

    process(expression: any, entity: Entity, generator?: ExpressionGenerator): string {
        // Use provided generator or singleton instance
        const exprGenerator = generator || expressionGenerator;
        const left = exprGenerator.generateExpressionCode(expression.left, entity);
        const right = exprGenerator.generateExpressionCode(expression.right, entity);

        // Handle null comparison and different types of equality
        if (right === "null" || left === "null" ||
            right === "true" || right === "false" ||
            left === "true" || left === "false" ||
            isNumeric(right) || isNumeric(left)) {
            return `${left} ${expression.op === "!=" ? "!=" : "=="} ${right}`;
        }

        // For objects use equals
        if (expression.op === "==") {
            return `${left}.equals(${right})`;
        } else {
            return `!${left}.equals(${right})`;
        }
    }
}

// Helper to check if a string is numeric
function isNumeric(str: string): boolean {
    if (typeof str !== 'string') return false;
    return !isNaN(parseFloat(str)) && isFinite(Number(str));
}

/**
 * LogicalOrExpression Processor - handles OR operator
 */
class LogicalOrExpressionProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "LogicalOrExpression";
    }

    process(expression: any, entity: Entity, generator: ExpressionGenerator): string {
        if (!expression.right) {
            return generator.generateExpressionCode(expression.left, entity);
        }
        const left = generator.generateExpressionCode(expression.left, entity);
        const right = generator.generateExpressionCode(expression.right, entity);
        return `${left} || ${right}`;
    }
}

/**
 * LogicalAndExpression Processor - handles AND operator
 */
class LogicalAndExpressionProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "LogicalAndExpression";
    }

    process(expression: any, entity: Entity, generator: ExpressionGenerator): string {
        if (!expression.right) {
            return generator.generateExpressionCode(expression.left, entity);
        }
        const left = generator.generateExpressionCode(expression.left, entity);
        const right = generator.generateExpressionCode(expression.right, entity);
        return `${left} && ${right}`;
    }
}

/**
 * ComparisonExpression Processor - handles >, <, >=, <= operators
 */
class ComparisonExpressionProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "ComparisonExpression";
    }

    process(expression: any, entity: Entity, generator: ExpressionGenerator): string {
        if (!expression.right || !expression.op) {
            return generator.generateExpressionCode(expression.left, entity);
        }
        const left = generator.generateExpressionCode(expression.left, entity);
        const right = generator.generateExpressionCode(expression.right, entity);
        return `${left} ${expression.op} ${right}`;
    }
}

/**
 * LiteralExpression Processor - handles literal values
 */
class LiteralExpressionProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "LiteralExpression";
    }

    process(expression: any): string {
        return expression.value || "null";
    }
}

/**
 * TimeExpression Processor - handles now() expressions
 */
class TimeExpressionProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "TimeExpression";
    }

    process(expression: any, entity: Entity, generator?: ExpressionGenerator): string {
        // Use provided generator or singleton instance
        const exprGenerator = generator || expressionGenerator;

        // Handle both simple now() and date operations
        if (!expression.date) {
            return "LocalDateTime.now()";
        }

        const date = exprGenerator.generateExpressionCode(expression.date, entity);
        const operation = expression.operation;

        if (operation === "isBefore" || operation === "isAfter" || operation === "isEqual") {
            const argument = exprGenerator.generateExpressionCode(expression.argument, entity);
            return `${date}.${operation}(${argument})`;
        }

        return date;
    }
}

/**
 * QuantifierExpression Processor - handles forall and exists expressions
 */
class QuantifierExpressionProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "QuantifierExpression";
    }

    process(expression: any, entity: Entity, generator: ExpressionGenerator): string {
        const quantifier = expression.quantifier;
        const variable = expression.variable;
        const collection = expression.collection;
        const body = generator.generateExpressionCode(expression.body, entity);

        if (quantifier === "forall") {
            return `
      for (var ${variable} : this.get${capitalize(collection)}()) {
        if (!(${body})) {
          return false;
        }
      }
      return true;`;
        } else if (quantifier === "exists") {
            return `
      for (var ${variable} : this.get${capitalize(collection)}()) {
        if (${body}) {
          return true;
        }
      }
      return false;`;
        }

        return "true";
    }
}

/**
 * ImplicationExpression Processor - handles => (implies) operator
 */
class ImplicationExpressionProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "ImplicationExpression";
    }

    process(expression: any, entity: Entity, generator: ExpressionGenerator): string {
        if (!expression.right) {
            return generator.generateExpressionCode(expression.left, entity);
        }
        const condition = generator.generateExpressionCode(expression.left, entity);
        const result = generator.generateExpressionCode(expression.right, entity);
        return `if (${condition}) {\n    return ${result};\n  }\n  return true;`;
    }
}

/**
 * ParenthesizedExpression Processor - handles (expr) expressions
 */
class ParenthesizedExpressionProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "ParenthesizedExpression";
    }

    process(expression: any, entity: Entity, generator: ExpressionGenerator): string {
        return `(${generator.generateExpressionCode(expression.expression, entity)})`;
    }
}

/**
 * BooleanExpression Processor - handles boolean expressions with multiple operators
 */
class BooleanExpressionProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "BooleanExpression";
    }

    process(expression: any, entity: Entity, generator?: ExpressionGenerator): string {
        // Use provided generator or the singleton instance
        const exprGenerator = generator || expressionGenerator;
        if (!expression.right) {
            return exprGenerator.generateExpressionCode(expression.left, entity);
        }
        const left = exprGenerator.generateExpressionCode(expression.left, entity);
        const op = expression.op === "OR" || expression.op === "||" ? "||" : "&&";
        const right = exprGenerator.generateExpressionCode(expression.right, entity);
        return `${left} ${op} ${right}`;
    }
}

/**
 * Addition Processor - handles addition and subtraction
 */
class AdditionProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "Addition";
    }

    process(expression: any, entity: Entity, generator?: ExpressionGenerator): string {
        // Use provided generator or the singleton instance
        const exprGenerator = generator || expressionGenerator;
        if (!expression.right) {
            return exprGenerator.generateExpressionCode(expression.left, entity);
        }
        const left = exprGenerator.generateExpressionCode(expression.left, entity);
        const right = exprGenerator.generateExpressionCode(expression.right, entity);
        return `${left} ${expression.op} ${right}`;
    }
}

/**
 * Multiplication Processor - handles multiplication, division, and modulo
 */
class MultiplicationProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "Multiplication";
    }

    process(expression: any, entity: Entity, generator?: ExpressionGenerator): string {
        // Use provided generator or the singleton instance
        const exprGenerator = generator || expressionGenerator;
        if (!expression.right) {
            return exprGenerator.generateExpressionCode(expression.left, entity);
        }
        const left = exprGenerator.generateExpressionCode(expression.left, entity);
        const right = exprGenerator.generateExpressionCode(expression.right, entity);
        return `${left} ${expression.op} ${right}`;
    }
}

/**
 * NegationExpression Processor - handles boolean negation
 */
class NegationExpressionProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "NegationExpression";
    }

    process(expression: any, entity: Entity, generator?: ExpressionGenerator): string {
        // Use provided generator or singleton instance
        const exprGenerator = generator || expressionGenerator;

        // Check if the expression is an equality operation to simplify the result
        if (expression.expression && expression.expression.$type === "EqualityExpression") {
            const left = exprGenerator.generateExpressionCode(expression.expression.left, entity);
            const right = exprGenerator.generateExpressionCode(expression.expression.right, entity);

            // If it's an equality expression, we can just flip the operator
            const newOp = expression.expression.op === "==" ? "!=" : "==";
            return `${left} ${newOp} ${right}`;
        }

        const expr = exprGenerator.generateExpressionCode(expression.expression, entity);
        return `!(${expr})`;
    }
}

/**
 * SignedExpression Processor - handles signed numbers
 */
class SignedExpressionProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "SignedExpression";
    }

    process(expression: any, entity: Entity, generator?: ExpressionGenerator): string {
        // Use provided generator or the singleton instance
        const exprGenerator = generator || expressionGenerator;
        const expr = exprGenerator.generateExpressionCode(expression.expression, entity);
        return `-${expr}`;
    }
}

/**
 * TernaryExpression Processor - handles if-then-else expressions
 */
class TernaryExpressionProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "TernaryExpression";
    }

    process(expression: any, entity: Entity, generator?: ExpressionGenerator): string {
        // Use provided generator or the singleton instance
        const exprGenerator = generator || expressionGenerator;
        const condition = exprGenerator.generateExpressionCode(expression.condition, entity);
        const trueValue = exprGenerator.generateExpressionCode(expression.trueValue, entity);
        const falseValue = expression.falseValue ?
            exprGenerator.generateExpressionCode(expression.falseValue, entity) :
            "null";

        return `(${condition}) ? ${trueValue} : ${falseValue}`;
    }
}

/**
 * CollectionExpression Processor - handles advanced collection operations
 */
class CollectionExpressionProcessor implements ExpressionProcessor {
    canProcess(expression: any): boolean {
        return expression && expression.$type === "CollectionExpression";
    }

    process(expression: any, entity: Entity, generator?: ExpressionGenerator): string {
        // Use provided generator or the singleton instance
        const exprGenerator = generator || expressionGenerator;
        const collection = exprGenerator.generateExpressionCode(expression.collection, entity);
        const operation = expression.collectionOperation;
        const variable = expression.variable || "item";
        const body = exprGenerator.generateExpressionCode(expression.body, entity);

        switch (operation) {
            case "allMatch":
                return `${collection}.stream().allMatch(${variable} -> ${body})`;
            case "anyMatch":
                return `${collection}.stream().anyMatch(${variable} -> ${body})`;
            case "noneMatch":
                return `${collection}.stream().noneMatch(${variable} -> ${body})`;
            case "filter":
                return `${collection}.stream().filter(${variable} -> ${body}).collect(Collectors.toList())`;
            case "map":
                return `${collection}.stream().map(${variable} -> ${body}).collect(Collectors.toList())`;
            case "flatMap":
                return `${collection}.stream().flatMap(${variable} -> ${body}.stream()).collect(Collectors.toList())`;
            default:
                return `${collection}.stream()`;
        }
    }
}

/**
 * Expression Generator - converts AST expressions to Java code
 */
export class ExpressionGenerator {
    private processors: ExpressionProcessor[] = [];

    constructor() {
        // Register all expression processors
        this.registerProcessor(new UniqueCheckProcessor());
        this.registerProcessor(new MethodCallProcessor());
        this.registerProcessor(new CollectionOperationProcessor());
        this.registerProcessor(new PropertyAccessProcessor());
        this.registerProcessor(new PropertyReferenceProcessor());
        this.registerProcessor(new PropertyChainProcessor());
        this.registerProcessor(new EqualityExpressionProcessor());
        this.registerProcessor(new LogicalOrExpressionProcessor());
        this.registerProcessor(new LogicalAndExpressionProcessor());
        this.registerProcessor(new ComparisonExpressionProcessor());
        this.registerProcessor(new LiteralExpressionProcessor());
        this.registerProcessor(new TimeExpressionProcessor());
        this.registerProcessor(new QuantifierExpressionProcessor());
        this.registerProcessor(new ImplicationExpressionProcessor());
        this.registerProcessor(new ParenthesizedExpressionProcessor());

        // Register new expression processors
        this.registerProcessor(new BooleanExpressionProcessor());
        this.registerProcessor(new AdditionProcessor());
        this.registerProcessor(new MultiplicationProcessor());
        this.registerProcessor(new NegationExpressionProcessor());
        this.registerProcessor(new SignedExpressionProcessor());
        this.registerProcessor(new TernaryExpressionProcessor());
        this.registerProcessor(new CollectionExpressionProcessor());
        this.registerProcessor(new ComparisonProcessor());
    }

    /**
     * Register a new expression processor
     */
    registerProcessor(processor: ExpressionProcessor): void {
        this.processors.push(processor);
    }

    /**
     * Generate Java code for an expression
     */
    generateExpressionCode(expression: any, entity: Entity): string {
        if (!expression || !expression.$type) {
            console.warn(`Invalid or undefined expression: ${JSON.stringify(expression)}`);
            return "true";
        }

        try {
            // Find a processor that can handle this expression
            for (const processor of this.processors) {
                if (processor.canProcess(expression)) {
                    return processor.process(expression, entity);
                }
            }

            // No processor found
            console.warn(`Unhandled expression type: ${expression.$type}`);
            return "true";
        } catch (error) {
            console.error(`Error processing expression: ${error}`);
            return "true";
        }
    }
}

// Export a singleton instance
export const expressionGenerator = new ExpressionGenerator(); 