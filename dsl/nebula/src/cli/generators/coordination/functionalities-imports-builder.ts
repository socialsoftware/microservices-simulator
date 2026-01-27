import { Aggregate, Entity } from '../common/parsers/model-parser.js';
import { CoordinationGenerationOptions } from '../microservices/types.js';
import { EntityRegistry } from '../common/utils/entity-registry.js';
import { OrchestrationBase } from '../common/orchestration-base.js';

/**
 * Builds imports for functionalities classes
 */
export class FunctionalitiesImportsBuilder extends OrchestrationBase {

    /**
     * Build all required imports for a functionalities class
     */
    buildImports(
        aggregate: Aggregate,
        rootEntity: Entity | undefined,
        options: CoordinationGenerationOptions,
        dependencies: any[],
        entityRegistry: EntityRegistry,
        businessMethods: any[]
    ): string[] {
        const imports: string[] = [];
        const projectName = options.projectName.toLowerCase();
        const basePackage = this.getBasePackage();

        // Base imports
        imports.push('import java.util.Arrays;');
        imports.push(`import ${basePackage}.${projectName}.microservices.exception.${this.capitalize(options.projectName)}Exception;`);
        imports.push('import org.springframework.beans.factory.annotation.Autowired;');
        imports.push('import org.springframework.core.env.Environment;');
        imports.push('import org.springframework.stereotype.Service;');
        imports.push('import jakarta.annotation.PostConstruct;');
        imports.push(`import ${basePackage}.ms.TransactionalModel;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
        imports.push(`import ${basePackage}.${projectName}.sagas.coordination.${aggregate.name.toLowerCase()}.*;`);

        // Service imports
        dependencies.forEach(dep => {
            if (dep.required && !dep.name.includes('UnitOfWorkService')) {
                const serviceName = dep.name.toLowerCase().replace('service', '');
                imports.push(`import ${basePackage}.${projectName}.microservices.${serviceName}.service.${dep.type};`);
            }
        });

        // DTO imports
        this.addDtoImports(aggregate, imports, projectName, entityRegistry, businessMethods);

        // List import if needed
        const hasListReturnType = businessMethods.some(method =>
            method.returnType && method.returnType.includes('List<')
        );
        if (hasListReturnType) {
            imports.push('import java.util.List;');
        }

        // Enum imports
        const enumTypes = new Set<string>();
        businessMethods.forEach(method => {
            this.extractEnumTypes(method.returnType, enumTypes);
            method.parameters?.forEach((param: any) => {
                this.extractEnumTypes(param.type, enumTypes);
            });
        });

        enumTypes.forEach(enumType => {
            const importPath = this.resolveEnumImportPath(enumType, options);
            if (importPath) {
                imports.push(`import ${importPath};`);
            }
        });

        return Array.from(new Set(imports));
    }

    /**
     * Extract enum types from a type string
     */
    private extractEnumTypes(type: string, enumSet: Set<string>): void {
        if (!type) return;

        const primitiveTypes = ['String', 'Integer', 'Long', 'Boolean', 'Double', 'Float', 'LocalDateTime', 'LocalDate', 'BigDecimal', 'void'];
        const typeName = type.replace(/List<|Set<|>/g, '').trim();

        if (typeName &&
            !primitiveTypes.includes(typeName) &&
            !typeName.endsWith('Dto') &&
            !typeName.includes('<') &&
            typeName.charAt(0) === typeName.charAt(0).toUpperCase()) {
            enumSet.add(typeName);
        }
    }

    /**
     * Resolve import path for an enum type
     */
    private resolveEnumImportPath(enumType: string, options: CoordinationGenerationOptions): string | null {
        if (!enumType) return null;

        const basePackage = this.getBasePackage();
        const projectName = options.projectName.toLowerCase();
        return `${basePackage}.${projectName}.shared.enums.${enumType}`;
    }

    /**
     * Add DTO imports based on business methods
     */
    private addDtoImports(aggregate: Aggregate, imports: string[], projectName: string, entityRegistry: EntityRegistry, businessMethods?: any[]): void {
        const usedDtoTypes = new Set<string>();
        const usedRequestDtoTypes = new Set<string>();

        if (businessMethods && businessMethods.length > 0) {
            businessMethods.forEach(method => {
                this.collectDtoTypesFromReturnType(method.returnType, usedDtoTypes);
                method.parameters?.forEach((param: any) => {
                    // Check if this is a request DTO (Create/Update RequestDto)
                    if (param.type && param.type.includes('RequestDto')) {
                        usedRequestDtoTypes.add(param.type);
                    } else {
                        this.collectDtoTypesFromReturnType(param.type, usedDtoTypes);
                    }
                });
            });
        } else {
            if (aggregate.methods) {
                aggregate.methods.forEach((method: any) => {
                    const returnType = this.extractReturnType(method.returnType, entityRegistry);
                    this.collectDtoTypesFromReturnType(returnType, usedDtoTypes);
                });
            }

            if (aggregate.workflows) {
                aggregate.workflows.forEach((workflow: any) => {
                    const returnType = this.extractReturnType(workflow.returnType, entityRegistry);
                    this.collectDtoTypesFromReturnType(returnType, usedDtoTypes);
                });
            }
        }

        const basePackage = this.getBasePackage();
        const dtoPackage = `${basePackage}.${projectName}.shared.dtos`;
        usedDtoTypes.forEach(dtoType => {
            imports.push(`import ${dtoPackage}.${dtoType};`);
        });

        // Import request DTOs from webapi.requestDtos package
        const requestDtoPackage = `${basePackage}.${projectName}.coordination.webapi.requestDtos`;
        usedRequestDtoTypes.forEach(requestDtoType => {
            imports.push(`import ${requestDtoPackage}.${requestDtoType};`);
        });
    }

    /**
     * Collect DTO types from a return type string
     */
    private collectDtoTypesFromReturnType(returnType: string, usedDtoTypes: Set<string>): void {
        if (returnType && returnType.includes('Dto')) {
            const match = returnType.match(/(\w+Dto)/g);
            if (match) {
                match.forEach(dto => usedDtoTypes.add(dto));
            }
        }
    }

    /**
     * Extract return type from AST node
     */
    private extractReturnType(returnType: any, entityRegistry: EntityRegistry): string {
        if (!returnType) return 'void';

        if (typeof returnType === 'string') {
            if (entityRegistry.isEntityName(returnType)) {
                return returnType + 'Dto';
            }
            return returnType;
        }

        const type = returnType;

        if (type.$type === 'PrimitiveType') {
            return type.typeName || 'void';
        } else if (type.$type === 'EntityType' && type.type && type.type.ref) {
            return type.type.ref.name + 'Dto';
        } else if (type.$type === 'BuiltinType') {
            return type.name;
        } else if (type.$type === 'ListType' && type.elementType) {
            const elementType = this.extractReturnType(type.elementType, entityRegistry);
            return `List<${elementType}>`;
        } else if (type.$type === 'SetType' && type.elementType) {
            const elementType = this.extractReturnType(type.elementType, entityRegistry);
            return `Set<${elementType}>`;
        } else if (type.name) {
            if (entityRegistry.isEntityName(type.name)) {
                return type.name + 'Dto';
            }
            return type.name;
        }

        return 'void';
    }
}

