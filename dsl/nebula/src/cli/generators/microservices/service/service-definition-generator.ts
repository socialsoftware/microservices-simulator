import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { OrchestrationBase } from "../../common/orchestration-base.js";
import { getGlobalConfig } from "../../common/config.js";
import { MethodImplementationProcessor } from "./method-implementation-processor.js";
import { CrudMethodGenerator } from "./crud-method-generator.js";

export interface ServiceGenerationOptions {
    architecture?: string;
    features?: string[];
    projectName: string;
}

export class ServiceDefinitionGenerator extends OrchestrationBase {
    private methodProcessor: MethodImplementationProcessor;
    private crudGenerator: CrudMethodGenerator;

    constructor() {
        super();
        this.methodProcessor = new MethodImplementationProcessor();
        this.crudGenerator = new CrudMethodGenerator();
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
        const serviceName = serviceDefinition.name || `${aggregateName}Service`;
        const packageName = `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${aggregateName.toLowerCase()}.service`;

        const imports = this.buildServiceImports(aggregate, serviceDefinition, options);
        const methods = this.buildServiceMethods(serviceDefinition, aggregate, rootEntity);
        const dependencies = this.buildServiceDependencies(aggregate, serviceDefinition);

        return {
            packageName,
            serviceName,
            aggregateName,
            lowerAggregate: aggregateName.toLowerCase(),
            rootEntityName: rootEntity.name,
            imports,
            methods,
            dependencies,
            generateCrud: serviceDefinition.generateCrud || false,
            transactional: serviceDefinition.transactional || false,
            projectName: options.projectName.toLowerCase(),
            annotations: this.getFrameworkAnnotations()
        };
    }

    private buildServiceImports(aggregate: Aggregate, serviceDefinition: any, options: ServiceGenerationOptions): string[] {
        const imports = [
            'import org.springframework.stereotype.Service;',
            'import org.springframework.beans.factory.annotation.Autowired;',
            `import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', aggregate.name.toLowerCase(), 'aggregate')}.*;`,
            `import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', aggregate.name.toLowerCase(), 'repository')}.*;`,
            'import java.util.List;',
            'import java.util.Set;',
            'import java.util.Optional;'
        ];

        if (serviceDefinition.transactional) {
            imports.push('import org.springframework.transaction.annotation.Transactional;');
        }

        // Check if any method uses business logic that requires additional imports
        const hasBusinessLogic = serviceDefinition.serviceMethods?.some((method: any) =>
            method.implementation?.actions?.length > 0
        );

        if (hasBusinessLogic) {
            imports.push(
                'import java.sql.SQLException;',
                'import org.springframework.dao.CannotAcquireLockException;',
                'import org.springframework.retry.annotation.Backoff;',
                'import org.springframework.retry.annotation.Retryable;',
                'import org.springframework.transaction.annotation.Isolation;',
                'import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;',
                'import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;',
                `import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', 'exception')}.${this.capitalize(options.projectName)}Exception;`,
                `import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', 'exception')}.${this.capitalize(options.projectName)}ErrorMessage;`
            );
        }

        // Add imports based on method parameters and return types
        const hasUserDto = serviceDefinition.serviceMethods?.some((method: any) =>
            method.parameters?.some((param: any) => param.type === 'UserDto') ||
            method.returnType === 'UserDto'
        );

        const hasAggregateState = serviceDefinition.serviceMethods?.some((method: any) =>
            method.parameters?.some((param: any) => param.type === 'AggregateState') ||
            method.returnType === 'AggregateState'
        );

        if (hasUserDto) {
            imports.push(`import ${getGlobalConfig().buildPackageName(options.projectName, 'shared', 'dtos')}.UserDto;`);
        }

        if (hasAggregateState) {
            imports.push('import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateState;');
        }

        return imports;
    }

    private buildServiceMethods(serviceDefinition: any, aggregate: Aggregate, rootEntity: Entity): any[] {
        const methods: any[] = [];

        // Generate CRUD methods using the dedicated generator
        if (serviceDefinition.generateCrud) {
            const crudOptions = CrudMethodGenerator.createOptions({
                transactional: serviceDefinition.transactional || false,
                includeValidation: true
            });
            methods.push(...this.crudGenerator.generateCrudMethods(aggregate, rootEntity, crudOptions));
        }

        // Generate custom service methods
        if (serviceDefinition.serviceMethods) {
            methods.push(...this.buildCustomServiceMethods(serviceDefinition.serviceMethods, aggregate));
        }

        return methods;
    }

    private buildCustomServiceMethods(serviceMethods: any[], aggregate: Aggregate): any[] {
        return serviceMethods.map((method: any) => {
            const methodImplementation = this.methodProcessor.processMethodImplementation(method.implementation, aggregate);

            return {
                name: method.name,
                parameters: method.parameters?.map((param: any) => ({
                    type: this.resolveJavaType(param.type || 'Object'),
                    name: param.name || 'param'
                })) || [],
                returnType: this.resolveJavaType(method.returnType || 'void'),
                annotations: method.annotations || [],
                implementation: methodImplementation
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
            generateCrud: false,
            transactional: false,
            projectName: options.projectName.toLowerCase()
        };

        const template = this.loadTemplate('service/service-definition.hbs');
        return this.renderTemplate(template, context);
    }

    protected override resolveJavaType(type: string | any): string {
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

        // Check if any method uses business logic that requires additional dependencies
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
