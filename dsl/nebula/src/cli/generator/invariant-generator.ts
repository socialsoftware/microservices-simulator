import { Entity, isPrimitiveType } from "../../language/generated/ast.js";
import { capitalize, findPropertyByName } from "./utils.js";
import { expressionGenerator } from "./expression-generator.js";
import { ImportRequirements } from "./entity-generator.js";


function analyzeExpression(expression: any, importReqs: ImportRequirements, entity: Entity): void {
  // Handle the case where no expression exists
  if (!expression) return;

  // Check expression type
  const exprType = expression.$type;

  if (exprType === 'PropertyChainExpression') {
    // For PropertyChainExpression, we need to check the property type in the entity
    if (expression.head?.$type === 'PropertyReference') {
      const propName = expression.head.name;

      // Find the property in the entity
      const property = findPropertyByName(entity, propName);

      if (property && property.type && isPrimitiveType(property.type)) {
        if (property.type.typeName === 'LocalDateTime') {
          importReqs.usesLocalDateTime = true;

          // Look for time-related method calls
          if (expression.operationParts && expression.operationParts.length > 0) {
            const firstOp = expression.operationParts[0];
            if (firstOp.$type === "MethodCall" &&
              (firstOp.method === "isBefore" || firstOp.method === "isAfter")) {
              importReqs.usesLocalDateTime = true;
            }
          }
        }
      }
    }

    // Check for collection methods
    if (expression.operationParts && expression.operationParts.length > 0) {
      const firstOp = expression.operationParts[0];
      if (firstOp.$type === "MethodCall" &&
        (firstOp.method?.toLowerCase().includes('stream') ||
          firstOp.method?.toLowerCase().includes('collect') ||
          firstOp.method === 'distinct' ||
          firstOp.method === 'map')) {
        importReqs.usesStreams = true;
      }
    }
  }
  else if (exprType === 'CollectionOperationExpression') {
    // For collection operations like size(), isEmpty()
    if (expression.operation === 'size' || expression.operation === 'isEmpty') {
      importReqs.usesSet = true;
    }
  }
  else if (exprType === 'UniqueCheckExpression') {
    // For uniqueness checks we'll need streams
    importReqs.usesStreams = true;
  }
  else if (exprType === 'TimeExpression') {
    // For time expressions like now()
    importReqs.usesLocalDateTime = true;
  }
  else if (exprType === 'LambdaCall') {
    // For lambda calls we'll need streams
    importReqs.usesStreams = true;
  }
  else if (exprType === 'BooleanExpression') {
    // Recursively check both sides of Boolean expressions
    if (expression.left) analyzeExpression(expression.left, importReqs, entity);
    if (expression.right) analyzeExpression(expression.right, importReqs, entity);
  }
  else if (exprType === 'LogicalOrExpression' || exprType === 'LogicalAndExpression') {
    // Recursively check both sides
    if (expression.left) analyzeExpression(expression.left, importReqs, entity);
    if (expression.right) analyzeExpression(expression.right, importReqs, entity);
  }
  else if (exprType === 'EqualityExpression' || exprType === 'ComparisonExpression') {
    // Recursively check both sides
    if (expression.left) analyzeExpression(expression.left, importReqs, entity);
    if (expression.right) analyzeExpression(expression.right, importReqs, entity);
  }
  else if (exprType === 'Addition' || exprType === 'Multiplication') {
    // Recursively check both sides of arithmetic expressions
    if (expression.left) analyzeExpression(expression.left, importReqs, entity);
    if (expression.right) analyzeExpression(expression.right, importReqs, entity);
  }
  else if (exprType === 'NegationExpression') {
    // Analyze the inner expression
    if (expression.expression) analyzeExpression(expression.expression, importReqs, entity);
  }
  else if (exprType === 'SignedExpression') {
    // Analyze the inner expression
    if (expression.expression) analyzeExpression(expression.expression, importReqs, entity);
  }
  else if (exprType === 'TernaryExpression') {
    // Analyze all parts of the ternary expression
    if (expression.condition) analyzeExpression(expression.condition, importReqs, entity);
    if (expression.trueValue) analyzeExpression(expression.trueValue, importReqs, entity);
    if (expression.falseValue) analyzeExpression(expression.falseValue, importReqs, entity);
  }
  else if (exprType === 'CollectionExpression') {
    // For collection operations like stream, map, filter, etc.
    importReqs.usesStreams = true;

    // Also add Collectors import
    importReqs.customImports = importReqs.customImports || new Set<string>();
    importReqs.customImports.add('import java.util.stream.Collectors;');

    // Analyze the body expression
    if (expression.body) analyzeExpression(expression.body, importReqs, entity);
  }
  else if (exprType === 'QuantifierExpression') {
    // For forall/exists expressions
    importReqs.usesStreams = true;
    if (expression.body) analyzeExpression(expression.body, importReqs, entity);
  }
}

export function generateInvariants(entity: Entity): { code: string, imports: ImportRequirements } {
  const importReqs: ImportRequirements = {
    customImports: new Set<string>([
      'import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;',
      'import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;'
    ])
  };

  // Generate invariant methods for each explicit invariant
  const invariantMethods = entity.invariants?.map(invariant => {
    const methodName = `invariant${capitalize(invariant.name)}`;
    const condition = invariant.conditions[0]?.expression;

    if (!condition) {
      return `
  private boolean ${methodName}() {
    // No condition specified
    return true;
  }`;
    }

    // Analyze the expression to determine required imports
    analyzeExpression(condition, importReqs, entity);

    // Special handling for unique check expressions
    if (isUniqueCheckExpression(condition)) {
      const uniqueExpr = condition as any;
      const collection = uniqueExpr.collection.name;
      const property = uniqueExpr.property;
      return `
  private boolean ${methodName}() {
    return this.get${capitalize(collection)}().size() == 
           this.get${capitalize(collection)}().stream()
               .map(item -> item.get${capitalize(property)}())
               .distinct()
               .count();
  }`;
    }

    // For other expressions, use the expression generator
    try {
      const javaCondition = expressionGenerator.generateExpressionCode(condition, entity);
      return `
  private boolean ${methodName}() {
    ${javaCondition.includes('return') ? javaCondition : `return ${javaCondition};`}
  }`;
    } catch (error) {
      console.error(`Error generating code for invariant ${invariant.name}: ${error}`);
      return `
  private boolean ${methodName}() {
    // Could not generate code for this invariant
    return true;
  }`;
    }
  }).join('\n') || '';

  // Generate verifyInvariants method that calls all the invariant methods
  const verifyInvariantsMethod = entity.invariants && entity.invariants.length > 0 ? `
  @Override
  public void verifyInvariants() {
    if (!(${entity.invariants.map(inv => `invariant${capitalize(inv.name)}()`).join('\n            && ')})) {
      throw new SimulatorException(INVARIANT_BREAK, getAggregateId());
    }
  }` : '';

  // Standard invariant checks
  let standardInvariants = `
\t/************************* INVARIANTS *************************/
${invariantMethods}
${verifyInvariantsMethod}`;

  return {
    code: standardInvariants,
    imports: importReqs
  };
}

export function isUniqueCheckExpression(expression: any): boolean {
  return expression && expression.$type === "UniqueCheckExpression";
} 