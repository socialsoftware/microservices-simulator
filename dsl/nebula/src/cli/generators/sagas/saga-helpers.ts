import { StringUtils } from '../../utils/string-utils.js';



export class SagaHelpers {
    private isEntityType(type: any): boolean {
        
        if (typeof type === 'string') {
            return type.charAt(0) === type.charAt(0).toUpperCase() && !['String', 'Integer', 'Long', 'Boolean', 'LocalDateTime'].includes(type);
        }
        return false;
    }

    private resolveJavaType(type: any): string {
        
        if (typeof type === 'string') return type;
        return 'Object';
    }

    private isPrimitiveType(type: string): boolean {
        const primitives = ['String', 'Integer', 'Long', 'Float', 'Double', 'Boolean', 'LocalDateTime', 'int', 'long', 'float', 'double', 'boolean'];
        return primitives.includes(type);
    }

    

    getParamTypeName(paramType: any, aggregateName: string, useSagaDto: boolean = false): string {
        if (!paramType) return 'Object';

        
        if (paramType.typeName) {
            return paramType.typeName;
        }

        
        if (paramType.type?.ref?.name) {
            const entityName = paramType.type.ref.name;
            
            if (useSagaDto && entityName === aggregateName) {
                return `Saga${aggregateName}Dto`;
            }
            return `${entityName}Dto`;
        }

        
        if (paramType.$type === 'ListType') {
            const elementType = typeof paramType.elementType === 'string'
                ? paramType.elementType
                : (paramType.elementType?.typeName || 'Object');
            return `List<${elementType}>`;
        }

        
        if (paramType.$type === 'SetType') {
            const elementType = typeof paramType.elementType === 'string'
                ? paramType.elementType
                : (paramType.elementType?.typeName || 'Object');
            return `Set<${elementType}>`;
        }

        
        if (paramType.$type === 'OptionalType') {
            const elementType = typeof paramType.elementType === 'string'
                ? paramType.elementType
                : (paramType.elementType?.typeName || 'Object');
            return `Optional<${elementType}>`;
        }

        
        if (paramType.name === 'UnitOfWork') {
            return 'SagaUnitOfWork';
        }
        if (paramType.name === 'AggregateState') {
            return 'AggregateState';
        }

        return 'Object';
    }

    

    convertWorkflowArg(arg: any): string {
        if (!arg) return 'null';

        
        const ref = arg.ref || '';
        const chain = arg.chain || [];

        if (chain.length > 0) {
            
            let result = ref;
            for (const part of chain) {
                
                result += `.get${StringUtils.capitalize(part)}()`;
            }
            return result;
        }

        return ref;
    }

    

    convertStepArgument(arg: any): string {
        if (typeof arg === 'string') {
            return arg;
        }
        if (arg.$type === 'PropertyChainExpression' || arg.$type === 'MethodCall') {
            
            return this.convertExpressionToJava(arg);
        }
        if (arg.value !== undefined) {
            return String(arg.value);
        }
        
        if (arg.name) {
            return arg.name;
        }
        
        if (arg.$refText) {
            return arg.$refText;
        }
        
        if (arg.ref && arg.ref.name) {
            return arg.ref.name;
        }
        
        if (typeof arg === 'object' && arg !== null) {
            return 'null /* Unable to resolve argument type */';
        }
        return String(arg);
    }

    

    convertStepExpression(expr: any): string {
        if (typeof expr === 'string') {
            return expr;
        }
        if (expr.$type) {
            return this.convertExpressionToJava(expr);
        }
        return String(expr);
    }

    

    convertExpressionToJava(expr: any): string {
        
        if (expr.$type === 'PropertyChainExpression') {
            const head = expr.head?.name || '';
            let result = `this.${head}`;
            
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

    

    inferVariableType(methodName: string, defaultType?: string): string {
        
        if (methodName.includes('Dto') || methodName.includes('get')) {
            return defaultType || 'Object';
        }
        return defaultType || 'Object';
    }

    

    inferVariableTypeFromExpression(expr: string): string {
        
        if (expr.includes('Dto')) {
            return 'Object'; 
        }
        return 'Object';
    }

    

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

