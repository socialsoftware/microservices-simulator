import { OrchestrationBase } from '../common/orchestration-base.js';

/**
 * Helper utilities for saga generation - type conversion, argument handling, etc.
 */
export class SagaHelpers extends OrchestrationBase {

    /**
     * Get parameter type name, resolving entity references to DTOs
     */
    getParamTypeName(paramType: any, aggregateName: string, useSagaDto: boolean = false): string {
        if (!paramType) return 'Object';

        // PrimitiveType
        if (paramType.typeName) {
            return paramType.typeName;
        }

        // EntityType (reference to entity)
        if (paramType.type?.ref?.name) {
            const entityName = paramType.type.ref.name;
            // Use SagaDto only if explicitly requested and for root entity
            if (useSagaDto && entityName === aggregateName) {
                return `Saga${aggregateName}Dto`;
            }
            return `${entityName}Dto`;
        }

        // ListType
        if (paramType.$type === 'ListType') {
            const elementType = typeof paramType.elementType === 'string'
                ? paramType.elementType
                : (paramType.elementType?.typeName || 'Object');
            return `List<${elementType}>`;
        }

        // SetType
        if (paramType.$type === 'SetType') {
            const elementType = typeof paramType.elementType === 'string'
                ? paramType.elementType
                : (paramType.elementType?.typeName || 'Object');
            return `Set<${elementType}>`;
        }

        // OptionalType
        if (paramType.$type === 'OptionalType') {
            const elementType = typeof paramType.elementType === 'string'
                ? paramType.elementType
                : (paramType.elementType?.typeName || 'Object');
            return `Optional<${elementType}>`;
        }

        // BuiltinType
        if (paramType.name === 'UnitOfWork') {
            return 'SagaUnitOfWork';
        }
        if (paramType.name === 'AggregateState') {
            return 'AggregateState';
        }

        return 'Object';
    }

    /**
     * Convert a workflow argument to Java code
     */
    convertWorkflowArg(arg: any): string {
        if (!arg) return 'null';

        // WorkflowArg has ref and chain properties
        const ref = arg.ref || '';
        const chain = arg.chain || [];

        if (chain.length > 0) {
            // Build the chain: ref.chain[0].chain[1]...
            let result = ref;
            for (const part of chain) {
                // Use getter for each part
                result += `.get${this.capitalize(part)}()`;
            }
            return result;
        }

        return ref;
    }

    /**
     * Convert a step argument to Java code
     */
    convertStepArgument(arg: any): string {
        if (typeof arg === 'string') {
            return arg;
        }
        if (arg.$type === 'PropertyChainExpression' || arg.$type === 'MethodCall') {
            // Convert expression to Java code
            return this.convertExpressionToJava(arg);
        }
        if (arg.value !== undefined) {
            return String(arg.value);
        }
        // Handle parameter references (objects with name property)
        if (arg.name) {
            return arg.name;
        }
        // Handle reference objects
        if (arg.$refText) {
            return arg.$refText;
        }
        // Handle ref objects (Langium references)
        if (arg.ref && arg.ref.name) {
            return arg.ref.name;
        }
        // Fallback - try to get a meaningful string
        if (typeof arg === 'object' && arg !== null) {
            return 'null /* TODO: fix argument */';
        }
        return String(arg);
    }

    /**
     * Convert a step expression to Java code
     */
    convertStepExpression(expr: any): string {
        if (typeof expr === 'string') {
            return expr;
        }
        if (expr.$type) {
            return this.convertExpressionToJava(expr);
        }
        return String(expr);
    }

    /**
     * Convert an expression to Java code
     */
    convertExpressionToJava(expr: any): string {
        // Basic expression conversion - can be expanded
        if (expr.$type === 'PropertyChainExpression') {
            const head = expr.head?.name || '';
            let result = `this.${head}`;
            // Handle property access chains
            return result;
        }
        if (expr.$type === 'MethodCall') {
            const receiver = this.convertExpressionToJava(expr.receiver);
            const method = expr.method;
            const args = (expr.arguments || []).map((a: any) => this.convertStepArgument(a)).join(', ');
            return `${receiver}.${method}(${args})`;
        }
        return String(expr);
    }

    /**
     * Infer variable type from method name
     */
    inferVariableType(methodName: string, defaultType?: string): string {
        // Simple inference - can be improved
        if (methodName.includes('Dto') || methodName.includes('get')) {
            return defaultType || 'Object';
        }
        return defaultType || 'Object';
    }

    /**
     * Infer variable type from expression
     */
    inferVariableTypeFromExpression(expr: string): string {
        // Simple inference based on expression
        if (expr.includes('Dto')) {
            return 'Object'; // Will need proper type resolution
        }
        return 'Object';
    }

    /**
     * Get searchable properties from an entity (String, Boolean, enums, related aggregateIds)
     */
    getSearchableProperties(entity: any): { name: string; type: string }[] {
        if (!entity.properties) return [];

        const searchableTypes = ['String', 'Boolean'];
        const properties: { name: string; type: string }[] = [];

        for (const prop of entity.properties) {
            const propType = (prop as any).type;
            const typeName = propType?.typeName || propType?.type?.$refText || propType?.$refText || '';

            let isEnum = false;
            if (propType && typeof propType === 'object' && propType.$type === 'EntityType' && propType.type) {
                const ref = propType.type.ref;
                if (ref && typeof ref === 'object' && '$type' in ref && (ref as any).$type === 'EnumDefinition') {
                    isEnum = true;
                } else if (propType.type.$refText) {
                    const javaType = this.resolveJavaType(prop.type);
                    if (!this.isPrimitiveType(javaType) && !this.isEntityType(javaType) &&
                        !javaType.startsWith('List<') && !javaType.startsWith('Set<')) {
                        isEnum = true;
                    }
                }
            }

            if (searchableTypes.includes(typeName) || isEnum) {
                const javaType = this.resolveJavaType(prop.type);
                properties.push({
                    name: prop.name,
                    type: javaType
                });
            }
        }

        // Also expose aggregateId-like fields from entity-type properties
        for (const prop of entity.properties) {
            const typeNode: any = (prop as any).type;
            if (!typeNode || typeNode.$type !== 'EntityType' || !typeNode.type) continue;

            const refEntity = typeNode.type.ref as any;
            if (!refEntity || !refEntity.properties) continue;

            for (const relProp of refEntity.properties as any[]) {
                if (!relProp.name || !relProp.name.endsWith('AggregateId')) continue;

                const relType = relProp.type;
                const relTypeName = relType?.typeName || relType?.type?.$refText || relType?.$refText || '';
                if (relTypeName !== 'Integer' && relTypeName !== 'Long') continue;

                properties.push({
                    name: relProp.name,
                    type: relTypeName
                });
            }
        }

        return properties;
    }
}

