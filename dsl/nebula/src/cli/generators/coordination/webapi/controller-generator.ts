import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { WebApiGenerationOptions } from "./webapi-types.js";
import { WebApiBaseGenerator } from "./webapi-base-generator.js";
import { getGlobalConfig } from "../../common/config.js";

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
            functionalitiesName: `${aggregate.name.charAt(0).toLowerCase() + aggregate.name.slice(1)}Functionalities`,
            packageName: getGlobalConfig().buildPackageName(options.projectName, 'coordination', 'webapi'),
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
        const lowerAggregate = aggregateName.toLowerCase();

        // Check for auto CRUD generation
        if (aggregate.webApiEndpoints?.autoCrud) {
            const crudEndpoints = this.generateCrudEndpoints(aggregateName, lowerAggregate);
            endpoints.push(...crudEndpoints);
        }

        if (aggregate.webApiEndpoints && aggregate.webApiEndpoints.endpoints.length > 0) {
            aggregate.webApiEndpoints.endpoints.forEach((endpoint: any) => {
                const resolvedReturnType = endpoint.returnType ? this.resolveParameterType(endpoint.returnType) : null;
                endpoints.push({
                    method: this.resolveHttpMethod(endpoint.method?.method || endpoint.httpMethod?.method),
                    path: endpoint.path,
                    methodName: endpoint.methodName,
                    parameters: endpoint.parameters.map((param: any) => ({
                        name: param.name,
                        type: this.resolveParameterType(param.type),
                        annotation: param.annotation
                    })),
                    returnType: resolvedReturnType,
                    description: endpoint.description || endpoint.desc,
                    throwsException: endpoint.throwsException === 'true'
                });
            });
        }
        return endpoints;
    }

    private generateCrudEndpoints(aggregateName: string, lowerAggregate: string): any[] {
        const dtoType = `${aggregateName}Dto`;

        return [
            {
                method: 'Post',
                path: `/${lowerAggregate}s/create`,
                methodName: `create${aggregateName}`,
                parameters: [{
                    name: `${lowerAggregate}Dto`,
                    type: dtoType,
                    annotation: '@RequestBody'
                }],
                returnType: dtoType,
                description: `Create a new ${aggregateName}`,
                isCrud: true
            },
            {
                method: 'Get',
                path: `/${lowerAggregate}s/{${lowerAggregate}AggregateId}`,
                methodName: `get${aggregateName}ById`,
                parameters: [{
                    name: `${lowerAggregate}AggregateId`,
                    type: 'Integer',
                    annotation: '@PathVariable'
                }],
                returnType: dtoType,
                description: `Get ${aggregateName} by aggregate ID`,
                isCrud: true
            },
            {
                method: 'Put',
                path: `/${lowerAggregate}s/{${lowerAggregate}AggregateId}`,
                methodName: `update${aggregateName}`,
                parameters: [
                    {
                        name: `${lowerAggregate}AggregateId`,
                        type: 'Integer',
                        annotation: '@PathVariable'
                    },
                    {
                        name: `${lowerAggregate}Dto`,
                        type: dtoType,
                        annotation: '@RequestBody'
                    }
                ],
                returnType: dtoType,
                description: `Update ${aggregateName}`,
                isCrud: true
            },
            {
                method: 'Delete',
                path: `/${lowerAggregate}s/{${lowerAggregate}AggregateId}`,
                methodName: `delete${aggregateName}`,
                parameters: [{
                    name: `${lowerAggregate}AggregateId`,
                    type: 'Integer',
                    annotation: '@PathVariable'
                }],
                returnType: null,
                description: `Delete ${aggregateName}`,
                isCrud: true
            }
        ];
    }


    private buildControllerImports(aggregate: Aggregate, options: WebApiGenerationOptions, endpoints: any[]): string[] {
        const imports = new Set<string>();

        imports.add('import org.springframework.web.bind.annotation.*;');
        imports.add('import org.springframework.beans.factory.annotation.Autowired;');

        imports.add(`import ${getGlobalConfig().buildPackageName(options.projectName, 'coordination', 'functionalities')}.${aggregate.name}Functionalities;`);

        // Only add collection imports when needed
        const hasListType = endpoints.some(e => e.returnType && e.returnType.includes('List<'));
        if (hasListType) {
            imports.add('import java.util.List;');
        }

        const hasSetType = endpoints.some(e => e.returnType && e.returnType.includes('Set<'));
        if (hasSetType) {
            imports.add('import java.util.Set;');
        }

        const dtoTypes = new Set<string>();

        endpoints.forEach(endpoint => {
            this.extractDtoTypes(endpoint.returnType, dtoTypes);

            endpoint.parameters?.forEach((param: any) => {
                this.extractDtoTypes(param.type, dtoTypes);
            });
        });

        dtoTypes.forEach(dtoType => {
            const importPath = this.resolveDtoImportPath(dtoType, options);
            if (importPath) {
                imports.add(`import ${importPath};`);
            }
        });

        if (endpoints.some(e => e.throwsException)) {
            imports.add(`import ${getGlobalConfig().buildPackageName(options.projectName, 'microservices', 'exception')}.*;`);
        }

        return Array.from(imports);
    }

    private extractDtoTypes(type: string, dtoSet: Set<string>): void {
        if (!type) return;

        const dtoMatches = type.match(/(\w+Dto)/g);
        if (dtoMatches) {
            dtoMatches.forEach(dto => {
                if (dto !== 'Dto') {
                    dtoSet.add(dto);
                }
            });
        }
    }

    async generateEmptyController(aggregate: Aggregate, options: WebApiGenerationOptions): Promise<string> {
        const packageName = getGlobalConfig().buildPackageName(options.projectName, 'coordination', 'webapi');

        const context = {
            packageName,
            aggregateName: aggregate.name
        };

        const template = this.getEmptyControllerTemplate();
        return this.renderTemplate(template, context);
    }

    private getControllerTemplate(): string {
        return this.loadTemplate('web/controller.hbs');
    }

    private getEmptyControllerTemplate(): string {
        return this.loadTemplate('web/empty-controller.hbs');
    }

    private resolveDtoImportPath(dtoType: string, options: WebApiGenerationOptions): string | null {
        if (!dtoType || !dtoType.endsWith('Dto')) {
            return null;
        }

        // DTOs are located in shared.dtos package
        const packageName = getGlobalConfig().buildPackageName(
            options.projectName,
            'shared',
            'dtos'
        );
        return `${packageName}.${dtoType}`;
    }
}
