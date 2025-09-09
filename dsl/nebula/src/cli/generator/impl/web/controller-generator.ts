import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { WebApiGenerationOptions } from "./webapi-types.js";
import { WebApiBaseGenerator } from "./webapi-base-generator.js";

export class ControllerGenerator extends WebApiBaseGenerator {
    async generateController(aggregate: Aggregate, rootEntity: Entity, options: WebApiGenerationOptions): Promise<string> {
        const context = this.buildControllerContext(aggregate, rootEntity, options);
        const template = this.getControllerTemplate();
        return this.renderTemplate(template, context);
    }

    private buildControllerContext(aggregate: Aggregate, rootEntity: Entity, options: WebApiGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const endpoints = this.buildEndpoints(rootEntity, capitalizedAggregate, aggregate);
        const imports = this.buildControllerImports(aggregate, options, endpoints);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            serviceName: `${lowerAggregate}Service`,
            packageName: `pt.ulisboa.tecnico.socialsoftware.${options.projectName.toLowerCase()}.coordination.webapi`,
            endpoints,
            imports,
            projectName: options.projectName.toLowerCase(),
            ProjectName: this.capitalize(options.projectName),
            hasLocalDtos: options.architecture === 'external-dto-removal',
            hasExternalDtos: options.architecture === 'default'
        };
    }

    private buildEndpoints(rootEntity: Entity, aggregateName: string, aggregate: Aggregate): any[] {
        const endpoints: any[] = [];

        if (aggregate.webApiEndpoints && aggregate.webApiEndpoints.endpoints.length > 0) {
            aggregate.webApiEndpoints.endpoints.forEach((endpoint: any) => {
                endpoints.push({
                    method: this.resolveHttpMethod(endpoint.method || endpoint.httpMethod),
                    path: endpoint.path,
                    methodName: endpoint.methodName,
                    parameters: endpoint.parameters.map((param: any) => ({
                        name: param.name,
                        type: this.resolveParameterType(param.type),
                        annotation: param.annotation
                    })),
                    returnType: this.resolveParameterType(endpoint.returnType),
                    description: endpoint.description || endpoint.desc,
                    throwsException: endpoint.throwsException === 'true'
                });
            });
        } else {
            this.generateDefaultEndpoints(endpoints, rootEntity, aggregateName);
        }

        return endpoints;
    }

    private generateDefaultEndpoints(endpoints: any[], rootEntity: Entity, aggregateName: string): void {
        const rootEntityName = rootEntity.name;
        const dtoType = `${rootEntityName}Dto`;

        endpoints.push({
            method: this.resolveHttpMethod('GET'),
            path: `/${aggregateName.toLowerCase()}s`,
            methodName: `getAll${aggregateName}s`,
            parameters: [],
            returnType: `List<${dtoType}>`,
            description: `Get all ${aggregateName.toLowerCase()}s`,
            throwsException: false
        });

        endpoints.push({
            method: this.resolveHttpMethod('GET'),
            path: `/${aggregateName.toLowerCase()}s/{id}`,
            methodName: `get${aggregateName}`,
            parameters: [{ name: 'id', type: 'Long', annotation: '@PathVariable' }],
            returnType: dtoType,
            description: `Get ${aggregateName.toLowerCase()} by ID`,
            throwsException: true
        });

        endpoints.push({
            method: this.resolveHttpMethod('POST'),
            path: `/${aggregateName.toLowerCase()}s`,
            methodName: `create${aggregateName}`,
            parameters: [{ name: `${aggregateName.toLowerCase()}Dto`, type: dtoType, annotation: '@RequestBody' }],
            returnType: dtoType,
            description: `Create new ${aggregateName.toLowerCase()}`,
            throwsException: true
        });

        endpoints.push({
            method: this.resolveHttpMethod('PUT'),
            path: `/${aggregateName.toLowerCase()}s/{id}`,
            methodName: `update${aggregateName}`,
            parameters: [
                { name: 'id', type: 'Long', annotation: '@PathVariable' },
                { name: `${aggregateName.toLowerCase()}Dto`, type: dtoType, annotation: '@RequestBody' }
            ],
            returnType: dtoType,
            description: `Update ${aggregateName.toLowerCase()}`,
            throwsException: true
        });

        endpoints.push({
            method: this.resolveHttpMethod('DELETE'),
            path: `/${aggregateName.toLowerCase()}s/{id}`,
            methodName: `delete${aggregateName}`,
            parameters: [{ name: 'id', type: 'Long', annotation: '@PathVariable' }],
            returnType: 'void',
            description: `Delete ${aggregateName.toLowerCase()}`,
            throwsException: true
        });
    }

    private buildControllerImports(aggregate: Aggregate, options: WebApiGenerationOptions, endpoints: any[]): string[] {
        const imports = new Set<string>();

        imports.add('import org.springframework.web.bind.annotation.*;');
        imports.add('import org.springframework.http.ResponseEntity;');
        imports.add('import org.springframework.beans.factory.annotation.Autowired;');

        if (options.architecture === 'external-dto-removal') {
            endpoints.forEach(endpoint => {
                if (endpoint.returnType && endpoint.returnType.includes('Dto')) {
                    imports.add(`import pt.ulisboa.tecnico.socialsoftware.${options.projectName.toLowerCase()}.aggregate.${endpoint.returnType.replace('Dto', '')}.${endpoint.returnType};`);
                }
                endpoint.parameters?.forEach((param: any) => {
                    if (param.type && param.type.includes('Dto')) {
                        imports.add(`import pt.ulisboa.tecnico.socialsoftware.${options.projectName.toLowerCase()}.aggregate.${param.type.replace('Dto', '')}.${param.type};`);
                    }
                });
            });
        }

        imports.add(`import pt.ulisboa.tecnico.socialsoftware.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.service.${aggregate.name}Service;`);

        endpoints.forEach(endpoint => {
            if (endpoint.returnType && endpoint.returnType.includes('Dto')) {
                const dtoType = endpoint.returnType.match(/(\w*Dto)/)?.[1];
                if (dtoType) {
                    imports.add(`import pt.ulisboa.tecnico.socialsoftware.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${dtoType};`);
                }
            }
            endpoint.parameters?.forEach((param: any) => {
                if (param.type && param.type.includes('Dto')) {
                    const dtoType = param.type.match(/(\w*Dto)/)?.[1];
                    if (dtoType) {
                        imports.add(`import pt.ulisboa.tecnico.socialsoftware.${options.projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.${dtoType};`);
                    }
                }
            });
        });

        if (endpoints.some(e => e.throwsException)) {
            imports.add(`import pt.ulisboa.tecnico.socialsoftware.${options.projectName.toLowerCase()}.microservices.exception.*;`);
        }

        return Array.from(imports);
    }

    private getControllerTemplate(): string {
        return this.loadTemplate('web/controller.hbs');
    }
}
