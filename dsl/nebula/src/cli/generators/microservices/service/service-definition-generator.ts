import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { GeneratorCapabilities, GeneratorCapabilitiesFactory } from "../../common/generator-capabilities.js";
import { getGlobalConfig } from "../../common/config.js";
import { CrudMethodGenerator } from "./crud-method-generator.js";
import { CollectionMethodGenerator } from "./collection-method-generator.js";
import { PRIMITIVE_TYPES } from "../../common/utils/type-constants.js";

export interface ServiceGenerationOptions {
    architecture?: string;
    projectName: string;
}

export class ServiceDefinitionGenerator {
    private capabilities: GeneratorCapabilities;
    private crudGenerator: CrudMethodGenerator;
    private collectionGenerator: CollectionMethodGenerator;

    constructor(capabilities?: GeneratorCapabilities) {
        this.capabilities = capabilities || GeneratorCapabilitiesFactory.createServiceCapabilities();
        this.crudGenerator = new CrudMethodGenerator();
        this.collectionGenerator = new CollectionMethodGenerator();
    }

    
    private getBasePackage(): string {
        return this.capabilities.packageBuilder.buildCustomPackage('').split('.').slice(0, -1).join('.');
    }

    private getFrameworkAnnotations(): any {
        return {
            service: '@Service',
            repository: '@Repository',
            component: '@Component'
        };
    }

    private loadTemplate(templatePath: string): string {
        return templatePath;
    }

    private renderTemplate(templatePath: string, context: any): string {
        return this.capabilities.templateRenderer.render(templatePath, context);
    }
    async generateServiceFromDefinition(aggregate: Aggregate, rootEntity: Entity, options: ServiceGenerationOptions): Promise<string> {
        const serviceDefinition = (aggregate as any).serviceDefinition;
        if (!serviceDefinition) {
            return this.generateDefaultService(aggregate, rootEntity, options);
        }

        const context = this.buildServiceContext(aggregate, rootEntity, serviceDefinition, options);
        const template = this.loadTemplate('service/service-definition.hbs');
        return this.renderTemplate(template, context);
    }

    private buildServiceContext(aggregate: Aggregate, rootEntity: Entity, serviceDefinition: any, options: ServiceGenerationOptions): any {
        const aggregateName = aggregate.name;
        const entityName = rootEntity.name;
        const lowerEntityName = entityName.charAt(0).toLowerCase() + entityName.slice(1);
        const serviceName = serviceDefinition.name || `${aggregateName}Service`;
        const packageName = `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregateName.toLowerCase()}.service`;

        const methods = this.buildServiceMethods(serviceDefinition, aggregate, rootEntity);
        const imports = this.buildServiceImports(aggregate, serviceDefinition, options, rootEntity, methods);
        const dependencies = this.buildServiceDependencies(aggregate, serviceDefinition);

        return {
            packageName,
            serviceName,
            aggregateName,
            entityName,
            lowerEntityName,
            lowerAggregate: aggregateName.toLowerCase(),
            rootEntityName: rootEntity.name,
            imports,
            methods,
            dependencies,
            generateCrud: true,
            transactional: serviceDefinition.transactional || false,
            projectName: options.projectName.toLowerCase(),
            annotations: this.getFrameworkAnnotations()
        };
    }

    private buildServiceImports(aggregate: Aggregate, serviceDefinition: any, options: ServiceGenerationOptions, rootEntity: Entity, methods: any[]): string[] {
        const imports = [
            'import java.sql.SQLException;',
            'import java.util.List;',
            'import java.util.Set;',
            'import java.util.stream.Collectors;',
            '',
            'import org.springframework.beans.factory.annotation.Autowired;',
            'import org.springframework.retry.annotation.Backoff;',
            'import org.springframework.retry.annotation.Retryable;',
            'import org.springframework.stereotype.Service;',
            'import org.springframework.transaction.annotation.Isolation;',
            'import org.springframework.transaction.annotation.Transactional;',
            '',
            'import org.springframework.dao.CannotAcquireLockException;',
            '',
            `import ${getGlobalConfig().getFrameworkPackage()}.coordination.unitOfWork.UnitOfWork;`,
            `import ${getGlobalConfig().getFrameworkPackage()}.coordination.unitOfWork.UnitOfWorkService;`,
            `import ${getGlobalConfig().getFrameworkPackage()}.domain.aggregate.AggregateIdGeneratorService;`,
            `import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', aggregate.name.toLowerCase(), 'aggregate')}.*;`,
            `import ${getGlobalConfig().buildPackageName(options.projectName, 'shared', 'dtos')}.*;`
        ];

        const aggregateName = aggregate.name;
        const lowerAggregate = aggregateName.toLowerCase();
        const eventPackage = getGlobalConfig().buildPackageName(options.projectName, 'events');
        imports.push(`import ${eventPackage}.${aggregateName}UpdatedEvent;`);
        imports.push(`import ${eventPackage}.${aggregateName}DeletedEvent;`);

        const requestDtoPackage = getGlobalConfig().buildPackageName(options.projectName, 'microservices', lowerAggregate, 'coordination', 'webapi', 'requestDtos');
        imports.push(`import ${requestDtoPackage}.Create${aggregateName}RequestDto;`);

        
        const aggregateEntityNames = new Set<string>();
        aggregate.entities?.forEach((entity: Entity) => {
            aggregateEntityNames.add(entity.name);
        });

        const enumTypes = new Set<string>();
        methods.forEach(method => {
            this.extractEnumTypes(method.returnType, enumTypes, aggregateEntityNames);
            method.parameters?.forEach((param: any) => {
                this.extractEnumTypes(param.type, enumTypes, aggregateEntityNames);
            });
        });

        enumTypes.forEach(enumType => {
            const importPath = this.resolveEnumImportPath(enumType, options);
            if (importPath) {
                imports.push(`import ${importPath};`);
            }
        });

        return imports;
    }

    private extractEnumTypes(type: string, enumSet: Set<string>, aggregateEntityNames: Set<string> = new Set()): void {
        if (!type) return;

        const typeName = type.replace(/List<|Set<|>/g, '').trim();

        
        if (aggregateEntityNames.has(typeName)) {
            return;
        }

        if (typeName &&
            !PRIMITIVE_TYPES.includes(typeName as any) &&
            !typeName.endsWith('Dto') &&
            !typeName.includes('<') &&
            typeName.charAt(0) === typeName.charAt(0).toUpperCase()) {
            enumSet.add(typeName);
        }
    }

    private resolveEnumImportPath(enumType: string, options: ServiceGenerationOptions): string | null {
        if (!enumType) return null;

        return getGlobalConfig().buildPackageName(options.projectName, 'shared', 'enums') + '.' + enumType;
    }

    private buildServiceMethods(serviceDefinition: any, aggregate: Aggregate, rootEntity: Entity): any[] {
        const methods: any[] = [];
        const entityName = rootEntity.name;

        const crudOptions = CrudMethodGenerator.createOptions({
            transactional: serviceDefinition.transactional || false,
            includeValidation: true
        });
        methods.push(...this.crudGenerator.generateCrudMethods(aggregate, rootEntity, crudOptions));
        methods.push(...this.collectionGenerator.generateCollectionMethods(aggregate, rootEntity));

        if (serviceDefinition.serviceMethods) {
            const crudMethodNames = new Set([
                `create${entityName}`,
                `get${entityName}ById`,
                `update${entityName}`,
                `delete${entityName}`,
                `getAll${aggregate.name}s`
            ]);

            const filteredMethods = serviceDefinition.serviceMethods.filter((method: any) =>
                !crudMethodNames.has(method.name)
            );
            methods.push(...this.buildCustomServiceMethods(filteredMethods));
        }

        return methods;
    }

    private buildCustomServiceMethods(serviceMethods: any[]): any[] {
        return serviceMethods.map((method: any) => {
            return {
                name: method.name,
                parameters: method.parameters?.map((param: any) => ({
                    type: this.resolveJavaType(param.type || 'Object'),
                    name: param.name || 'param'
                })) || [],
                returnType: this.resolveJavaType(method.returnType || 'void'),
                annotations: method.annotations || []
            };
        });
    }


    private generateDefaultService(aggregate: Aggregate, rootEntity: Entity, options: ServiceGenerationOptions): string {
        const context = {
            packageName: getGlobalConfig().buildPackageName(options.projectName, 'microservices', aggregate.name.toLowerCase(), 'service'),
            serviceName: `${aggregate.name}Service`,
            aggregateName: aggregate.name,
            lowerAggregate: aggregate.name.toLowerCase(),
            rootEntityName: rootEntity.name,
            imports: [
                'import org.springframework.stereotype.Service;',
                'import org.springframework.beans.factory.annotation.Autowired;',
                `import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', aggregate.name.toLowerCase(), 'aggregate')}.*;`,
                `import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', aggregate.name.toLowerCase(), 'repository')}.*;`
            ],
            methods: [],
            generateCrud: true,
            transactional: false,
            projectName: options.projectName.toLowerCase()
        };

        const template = this.loadTemplate('service/service-definition.hbs');
        return this.renderTemplate(template, context);
    }

    private resolveJavaType(type: string | any): string {
        let typeStr: string;

        if (typeof type === 'string') {
            typeStr = type;
        } else if (type?.$refText) {
            typeStr = type.$refText;
        } else if (type?.typeName) {
            typeStr = type.typeName;
        } else if (type?.type) {
            return this.resolveJavaType(type.type);
        } else {
            typeStr = 'Object';
        }

        const typeMap: { [key: string]: string } = {
            'Integer': 'Integer',
            'Long': 'Long',
            'String': 'String',
            'Boolean': 'Boolean',
            'LocalDateTime': 'LocalDateTime',
            'UnitOfWork': 'UnitOfWork',
            'void': 'void',
            'UserDto': 'UserDto',
            'AggregateState': 'AggregateState'
        };

        if (typeStr.startsWith('List<') && typeStr.endsWith('>')) {
            const innerType = typeStr.slice(5, -1);
            return `List<${this.resolveJavaType(innerType)}>`;
        }

        if (typeStr.startsWith('Set<') && typeStr.endsWith('>')) {
            const innerType = typeStr.slice(4, -1);
            return `Set<${this.resolveJavaType(innerType)}>`;
        }

        return typeMap[typeStr] || typeStr;
    }


    private buildServiceDependencies(aggregate: Aggregate, serviceDefinition: any): any[] {
        const dependencies = [
            {
                type: `${aggregate.name}Repository`,
                name: `${aggregate.name.toLowerCase()}Repository`
            }
        ];

        
        const hasBusinessLogic = serviceDefinition.serviceMethods?.some((method: any) =>
            method.implementation?.actions?.length > 0
        );

        if (hasBusinessLogic) {
            dependencies.push(
                {
                    type: 'UnitOfWorkService',
                    name: 'unitOfWorkService'
                },
                {
                    type: `${aggregate.name}Factory`,
                    name: `${aggregate.name.toLowerCase()}Factory`
                }
            );
        }

        return dependencies;
    }

}
