import { Aggregate, Entity } from '../../common/parsers/model-parser.js';
import { CoordinationGenerationOptions } from '../../microservices/types.js';
import { EntityRegistry } from '../../common/utils/entity-registry.js';
import { StringUtils } from '../../../utils/string-utils.js';
import { TypeExtractor } from '../../common/utils/type-extractor.js';



export class FunctionalitiesImportsBuilder {

    private getBasePackage(options: CoordinationGenerationOptions): string {
        if (!options.basePackage) {
            throw new Error('basePackage is required in CoordinationGenerationOptions');
        }
        return options.basePackage;
    }

    

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
        const basePackage = this.getBasePackage(options);

        
        imports.push('import java.util.Arrays;');
        imports.push(`import ${basePackage}.${projectName}.microservices.exception.${StringUtils.capitalize(options.projectName)}Exception;`);
        imports.push('import org.springframework.beans.factory.annotation.Autowired;');
        imports.push('import org.springframework.core.env.Environment;');
        imports.push('import org.springframework.stereotype.Service;');
        imports.push('import jakarta.annotation.PostConstruct;');
        imports.push(`import ${basePackage}.ms.TransactionalModel;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`);
        imports.push(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
        imports.push(`import ${basePackage}.ms.coordination.workflow.CommandGateway;`);
        imports.push(`import ${basePackage}.${projectName}.microservices.${aggregate.name.toLowerCase()}.coordination.sagas.*;`);

        
        dependencies.forEach(dep => {
            if (dep.required && !dep.name.includes('UnitOfWorkService') && !dep.name.includes('commandGateway')) {
                const serviceName = dep.name.toLowerCase().replace('service', '');
                imports.push(`import ${basePackage}.${projectName}.microservices.${serviceName}.service.${dep.type};`);
            }
        });

        
        this.addDtoImports(aggregate, imports, projectName, entityRegistry, businessMethods, options);

        
        const hasListReturnType = businessMethods.some(method =>
            method.returnType && method.returnType.includes('List<')
        );
        if (hasListReturnType) {
            imports.push('import java.util.List;');
        }

        
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

    

    private extractEnumTypes(type: string, enumSet: Set<string>): void {
        TypeExtractor.extractEnumTypes(type, enumSet);
    }

    

    private resolveEnumImportPath(enumType: string, options: CoordinationGenerationOptions): string | null {
        if (!enumType) return null;

        const basePackage = this.getBasePackage(options);
        const projectName = options.projectName.toLowerCase();
        return `${basePackage}.${projectName}.shared.enums.${enumType}`;
    }

    

    private addDtoImports(aggregate: Aggregate, imports: string[], projectName: string, entityRegistry: EntityRegistry, businessMethods: any[] | undefined, options: CoordinationGenerationOptions): void {
        const usedDtoTypes = new Set<string>();
        const usedRequestDtoTypes = new Set<string>();

        if (businessMethods && businessMethods.length > 0) {
            businessMethods.forEach(method => {
                this.collectDtoTypesFromReturnType(method.returnType, usedDtoTypes);
                method.parameters?.forEach((param: any) => {
                    
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

        const basePackage = this.getBasePackage(options);
        const dtoPackage = `${basePackage}.${projectName}.shared.dtos`;
        usedDtoTypes.forEach(dtoType => {
            imports.push(`import ${dtoPackage}.${dtoType};`);
        });

        const requestDtoPackage = `${basePackage}.${projectName}.microservices.${aggregate.name.toLowerCase()}.coordination.webapi.requestDtos`;
        usedRequestDtoTypes.forEach(requestDtoType => {
            imports.push(`import ${requestDtoPackage}.${requestDtoType};`);
        });
    }

    

    private collectDtoTypesFromReturnType(returnType: string, usedDtoTypes: Set<string>): void {
        if (returnType && returnType.includes('Dto')) {
            const match = returnType.match(/(\w+Dto)/g);
            if (match) {
                match.forEach(dto => usedDtoTypes.add(dto));
            }
        }
    }

    

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
            return type.typeName;
        } else if (type.$type === 'ListType' && type.elementType) {
            const elementType = this.extractReturnType(type.elementType, entityRegistry);
            return `List<${elementType}>`;
        } else if (type.$type === 'SetType' && type.elementType) {
            const elementType = this.extractReturnType(type.elementType, entityRegistry);
            return `Set<${elementType}>`;
        } else if (type.typeName || type.name) {
            const typeName = type.typeName || type.name;
            if (entityRegistry.isEntityName(typeName)) {
                return typeName + 'Dto';
            }
            return typeName;
        }

        return 'void';
    }
}

