import { OrchestrationBase } from "../../base/orchestration-base.js";

export abstract class WebApiBaseGenerator extends OrchestrationBase {
    protected resolveHttpMethod(method: string | any): string {
        if (!method || typeof method !== 'string') return 'Get';
        const upperMethod = method.toUpperCase();
        switch (upperMethod) {
            case 'GET': return 'Get';
            case 'POST': return 'Post';
            case 'PUT': return 'Put';
            case 'DELETE': return 'Delete';
            case 'PATCH': return 'Patch';
            default: return 'Get';
        }
    }

    protected resolveParameterType(type: any): string {
        if (!type) return 'Object';

        // Check if it's an EntityType reference
        if (typeof type === 'object' && type.$type === 'EntityType' && type.type) {
            const entityName = type.type.ref?.name || type.type.$refText;
            if (entityName) {
                return `${entityName}Dto`;
            }
        }

        // Handle AST types
        if (typeof type === 'object' && type.$type) {
            // Handle ListType
            if (type.$type === 'ListType' && type.elementType) {
                const elementType = this.extractElementTypeName(type.elementType);
                const dtoType = this.shouldConvertToDto(elementType) ? `${elementType}Dto` : elementType;
                return `List<${dtoType}>`;
            }
            // Handle SetType
            if (type.$type === 'SetType' && type.elementType) {
                const elementType = this.extractElementTypeName(type.elementType);
                const dtoType = this.shouldConvertToDto(elementType) ? `${elementType}Dto` : elementType;
                return `Set<${dtoType}>`;
            }
        }

        const resolvedType = this.resolveJavaType(type);

        // Only convert entity types to DTOs, not primitives or built-in types
        if (this.shouldConvertToDto(resolvedType)) {
            return `${resolvedType}Dto`;
        }

        return resolvedType;
    }

    private shouldConvertToDto(type: string): boolean {
        // Don't convert primitives and built-in Java types
        const doNotConvert = [
            'String', 'Integer', 'Boolean', 'Double', 'Float', 'Long',
            'LocalDateTime', 'Object', 'void',
            'UnitOfWork', 'AggregateState'
        ];

        if (doNotConvert.includes(type)) {
            return false;
        }

        // Don't convert types that already end with 'Dto'
        if (type.endsWith('Dto')) {
            return false;
        }

        // Convert entity types to DTOs
        return this.isEntityType(type);
    }

    private extractElementTypeName(elementType: any): string {
        if (typeof elementType === 'string') {
            return elementType;
        }
        if (elementType && typeof elementType === 'object') {
            // Handle PrimitiveType
            if (elementType.$type === 'PrimitiveType' && elementType.name) {
                return elementType.name;
            }
            // Handle ID references
            if (elementType.$refText) {
                return elementType.$refText;
            }
            if (elementType.ref && elementType.ref.name) {
                return elementType.ref.name;
            }
            if (elementType.name) {
                return elementType.name;
            }
        }
        return 'Object';
    }
}
