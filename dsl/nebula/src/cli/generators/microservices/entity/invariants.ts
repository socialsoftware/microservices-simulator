import { Entity } from "../../../../language/generated/ast.js";
import { capitalize } from "../../../utils/generator-utils.js";
import { ImportRequirements } from "./types.js";
import { getGlobalConfig } from "../../common/config.js";

export function generateInvariants(entity: Entity): { code: string, imports?: ImportRequirements } {
    const hasInvariants = entity.invariants && entity.invariants.length > 0;

    if (!hasInvariants) {
        const verifyMethod = `
    @Override
    public void verifyInvariants() {
    }`;

        return {
            code: verifyMethod,
            imports: undefined
        };
    }

    const imports: ImportRequirements = {};

    
    const invariantMethods = entity.invariants.map((invariant: any, index: number) => {
        const invariantName = invariant.name || `rule${index}`;
        const methodName = `invariant${capitalize(invariantName)}`;

        
        const condition = getInvariantConditionText(invariant);

        
        const dividerComment = index === 0 ? '\n' : '';

        return `${dividerComment}
    private boolean ${methodName}() {
        return ${condition};
    }`;
    }).join('\n');

    
    const individualChecks = entity.invariants.map((invariant: any, index: number) => {
        const invariantName = invariant.name || `rule${index}`;
        const methodName = `invariant${capitalize(invariantName)}()`;
        
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

    const fwk = getGlobalConfig().getFrameworkPackage();
    imports.customImports = new Set([
        `import static ${fwk}.exception.SimulatorErrorMessage.INVARIANT_BREAK;`,
        `import ${fwk}.exception.SimulatorException;`
    ]);

    return {
        code: invariantMethods + verifyMethod,
        imports
    };
}

function getInvariantConditionText(invariant: any): string {
    if (invariant.conditions && invariant.conditions.length > 0) {
        const firstCondition = invariant.conditions[0];

        const sourceText = firstCondition.expression?.$cstNode?.text;
        if (sourceText) {
            return convertDslToJava(sourceText.trim());
        }

        if (firstCondition.expression) {
            return convertExpressionToJava(firstCondition.expression);
        }
    }

    return 'true'; // Safe fallback
}

function convertDslToJava(dslText: string): string {
    let javaCode = dslText;

    javaCode = handleQuantifierExpressions(javaCode);

    javaCode = handleCollectionStreamOperations(javaCode);


    if (javaCode.includes('.isBefore(') || javaCode.includes('.isAfter(')) {
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

    if (javaCode.includes('.unique(')) {
        javaCode = javaCode.replace(/(\w+)\.unique\((\w+)\)/g, (match, collection, field) => {
            const coll = collection.startsWith('this.') ? collection : `this.${collection}`;
            const capitalizedField = capitalize(field);
            return `${coll}.stream().map(item -> item.get${capitalizedField}()).distinct().count() == ${coll}.size()`;
        });
    }

    if (javaCode.includes('.length()')) {
        javaCode = javaCode.replace(/(\w+)\.length\(\)\s*(>|<|>=|<=|==|!=)\s*(\d+)/g, (match, prop, op, num) => {
            if (prop.includes('this')) {
                return match;
            }
            return `this.${prop} != null && this.${prop}.length() ${op} ${num}`;
        });
    }

    
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

    
    
    
    
    javaCode = javaCode.replace(/(^|[^\w.])(\w+)(\s*(?:!=|==)\s*)(\w+|null|true|false|\d+)/g,
        (match, before, prop, opWithWs, value) => {
            
            if (prop === 'this') {
                return match;
            }
            
            if (before === '.') {
                return match;
            }
            
            return `${before}this.${prop}${opWithWs}${value}`;
        });

    
    javaCode = javaCode.replace(/\b(\w+)\s*(>|<|>=|<=)\s*(\w+)/g,
        (match, leftProp, op, rightProp) => {
            
            if (leftProp.includes('.') || rightProp.match(/^\d/)) {
                return match;
            }
            return `this.${leftProp} ${op} this.${rightProp}`;
        });

    return javaCode;
}


function handleQuantifierExpressions(javaCode: string): string {
    
    const forallPattern = /forall\s+(\w+)\s*:\s*(\w+)\s*\|\s*([^;]+)/g;
    javaCode = javaCode.replace(forallPattern, (match, variable, collection, body) => {
        
        const lambdaBody = body.trim()
            .replace(/\bthis\./g, '')  
            .replace(new RegExp(`\\b${variable}\\.`, 'g'), `${variable}.get`)  
            .replace(/\.(\w+)(?!\()/g, (_m: string, prop: string) => `.get${capitalize(prop)}()`);  

        return `this.${collection}.stream().allMatch(${variable} -> ${lambdaBody})`;
    });

    
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


function handleCollectionStreamOperations(javaCode: string): string {
    
    const allMatchPattern = /(\w+)\.allMatch\((\w+)\s*->\s*([^)]+)\)/g;
    javaCode = javaCode.replace(allMatchPattern, (match, collection, variable, body) => {
        const lambdaBody = body.trim()
            .replace(new RegExp(`\\b${variable}\\.`, 'g'), `${variable}.get`)
            .replace(/\.(\w+)(?!\()/g, (_m: string, prop: string) => `.get${capitalize(prop)}()`);

        return `this.${collection}.stream().allMatch(${variable} -> ${lambdaBody})`;
    });

    
    const anyMatchPattern = /(\w+)\.anyMatch\((\w+)\s*->\s*([^)]+)\)/g;
    javaCode = javaCode.replace(anyMatchPattern, (match, collection, variable, body) => {
        const lambdaBody = body.trim()
            .replace(new RegExp(`\\b${variable}\\.`, 'g'), `${variable}.get`)
            .replace(/\.(\w+)(?!\()/g, (_m: string, prop: string) => `.get${capitalize(prop)}()`);

        return `this.${collection}.stream().anyMatch(${variable} -> ${lambdaBody})`;
    });

    
    const noneMatchPattern = /(\w+)\.noneMatch\((\w+)\s*->\s*([^)]+)\)/g;
    javaCode = javaCode.replace(noneMatchPattern, (match, collection, variable, body) => {
        const lambdaBody = body.trim()
            .replace(new RegExp(`\\b${variable}\\.`, 'g'), `${variable}.get`)
            .replace(/\.(\w+)(?!\()/g, (_m: string, prop: string) => `.get${capitalize(prop)}()`);

        return `this.${collection}.stream().noneMatch(${variable} -> ${lambdaBody})`;
    });

    return javaCode;
}


function convertExpressionToJava(expression: any): string {
    if (!expression) {
        return 'true';
    }

    
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

    
    return 'true';
}

function convertPropertyChainToJava(expression: any): string {
    let result = `this.${expression.head.name}`;

    
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
