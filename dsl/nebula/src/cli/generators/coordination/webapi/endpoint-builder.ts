


import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { UnifiedTypeResolver } from "../../common/unified-type-resolver.js";



export enum HttpMethod {
    GET = 'GET',
    POST = 'POST',
    PUT = 'PUT',
    DELETE = 'DELETE',
    PATCH = 'PATCH'
}



export interface EndpointParameter {
    name: string;
    type: string;
    annotation: string; 
    required?: boolean;
}



export interface GeneratedEndpoint {
    method: HttpMethod;
    path: string;
    methodName: string;
    parameters: EndpointParameter[];
    returnType: string;
    description?: string;
    throwsException: boolean;
    annotations: string[];
}



export interface EndpointBuildingOptions {
    includeValidation: boolean;
    includeCrudEndpoints: boolean;
    includeCustomEndpoints: boolean;
    baseUrl?: string;
    throwsExceptions: boolean;
}



export class EndpointBuilder {

    

    buildEndpoints(
        aggregate: Aggregate,
        rootEntity: Entity,
        options: EndpointBuildingOptions = this.getDefaultOptions()
    ): GeneratedEndpoint[] {
        const endpoints: GeneratedEndpoint[] = [];

        
        if (options.includeCrudEndpoints) {
            endpoints.push(...this.buildCrudEndpoints(aggregate, rootEntity, options));
        }

        
        if (options.includeCustomEndpoints && aggregate.webApiEndpoints) {
            endpoints.push(...this.buildCustomEndpoints(aggregate, options));
        }

        return endpoints;
    }

    

    private buildCrudEndpoints(aggregate: Aggregate, rootEntity: Entity, options: EndpointBuildingOptions): GeneratedEndpoint[] {
        const aggregateName = aggregate.name;
        const entityName = rootEntity.name;
        const lowerAggregate = aggregateName.toLowerCase();
        const lowerEntity = entityName.toLowerCase();
        const baseUrl = options.baseUrl || `/${lowerAggregate}`;

        return [
            
            {
                method: HttpMethod.POST,
                path: baseUrl,
                methodName: `create${entityName}`,
                parameters: [
                    {
                        name: `${lowerEntity}Dto`,
                        type: `Create${entityName}RequestDto`,
                        annotation: '@RequestBody',
                        required: true
                    }
                ],
                returnType: `ResponseEntity<${entityName}ResponseDto>`,
                description: `Create a new ${entityName}`,
                throwsException: options.throwsExceptions,
                annotations: ['@PostMapping']
            },

            
            {
                method: HttpMethod.GET,
                path: `${baseUrl}/{id}`,
                methodName: `get${entityName}ById`,
                parameters: [
                    {
                        name: 'id',
                        type: 'Integer',
                        annotation: '@PathVariable',
                        required: true
                    }
                ],
                returnType: `ResponseEntity<${entityName}ResponseDto>`,
                description: `Get ${entityName} by ID`,
                throwsException: options.throwsExceptions,
                annotations: ['@GetMapping("/{id}")']
            },

            
            {
                method: HttpMethod.PUT,
                path: `${baseUrl}/{id}`,
                methodName: `update${entityName}`,
                parameters: [
                    {
                        name: 'id',
                        type: 'Integer',
                        annotation: '@PathVariable',
                        required: true
                    },
                    {
                        name: `${lowerEntity}Dto`,
                        type: `Update${entityName}RequestDto`,
                        annotation: '@RequestBody',
                        required: true
                    }
                ],
                returnType: `ResponseEntity<${entityName}ResponseDto>`,
                description: `Update ${entityName} by ID`,
                throwsException: options.throwsExceptions,
                annotations: ['@PutMapping("/{id}")']
            },

            
            {
                method: HttpMethod.DELETE,
                path: `${baseUrl}/{id}`,
                methodName: `delete${entityName}`,
                parameters: [
                    {
                        name: 'id',
                        type: 'Integer',
                        annotation: '@PathVariable',
                        required: true
                    }
                ],
                returnType: 'ResponseEntity<Void>',
                description: `Delete ${entityName} by ID`,
                throwsException: options.throwsExceptions,
                annotations: ['@DeleteMapping("/{id}")']
            },

            
            {
                method: HttpMethod.GET,
                path: baseUrl,
                methodName: `getAll${aggregateName}s`,
                parameters: [],
                returnType: `ResponseEntity<List<${entityName}ResponseDto>>`,
                description: `Get all ${aggregateName}s`,
                throwsException: options.throwsExceptions,
                annotations: ['@GetMapping']
            }
        ];
    }

    

    private buildCustomEndpoints(aggregate: Aggregate, options: EndpointBuildingOptions): GeneratedEndpoint[] {
        const endpoints: GeneratedEndpoint[] = [];

        if (aggregate.webApiEndpoints?.endpoints) {
            aggregate.webApiEndpoints.endpoints.forEach((endpoint: any) => {
                const builtEndpoint = this.buildCustomEndpoint(endpoint, options);
                endpoints.push(builtEndpoint);
            });
        }

        return endpoints;
    }

    

    private buildCustomEndpoint(endpoint: any, options: EndpointBuildingOptions): GeneratedEndpoint {
        const method = this.resolveHttpMethod(endpoint.method?.method || endpoint.httpMethod?.method);
        const returnType = endpoint.returnType
            ? UnifiedTypeResolver.resolveForWebApi(endpoint.returnType)
            : 'ResponseEntity<Void>';

        const parameters: EndpointParameter[] = endpoint.parameters?.map((param: any) => ({
            name: param.name,
            type: UnifiedTypeResolver.resolveForWebApi(param.type),
            annotation: param.annotation || '@RequestParam',
            required: param.required !== false
        })) || [];

        const annotations = this.buildEndpointAnnotations(method, endpoint.path);

        return {
            method,
            path: endpoint.path,
            methodName: endpoint.methodName,
            parameters,
            returnType,
            description: endpoint.description || endpoint.desc,
            throwsException: endpoint.throwsException === 'true' || options.throwsExceptions,
            annotations
        };
    }

    

    private buildEndpointAnnotations(method: HttpMethod, path: string): string[] {
        const annotations: string[] = [];

        switch (method) {
            case HttpMethod.GET:
                annotations.push(path ? `@GetMapping("${path}")` : '@GetMapping');
                break;
            case HttpMethod.POST:
                annotations.push(path ? `@PostMapping("${path}")` : '@PostMapping');
                break;
            case HttpMethod.PUT:
                annotations.push(path ? `@PutMapping("${path}")` : '@PutMapping');
                break;
            case HttpMethod.DELETE:
                annotations.push(path ? `@DeleteMapping("${path}")` : '@DeleteMapping');
                break;
            case HttpMethod.PATCH:
                annotations.push(path ? `@PatchMapping("${path}")` : '@PatchMapping');
                break;
        }

        return annotations;
    }

    

    private resolveHttpMethod(method: string | any): HttpMethod {
        if (!method || typeof method !== 'string') return HttpMethod.GET;

        const upperMethod = method.toUpperCase();
        switch (upperMethod) {
            case 'GET': return HttpMethod.GET;
            case 'POST': return HttpMethod.POST;
            case 'PUT': return HttpMethod.PUT;
            case 'DELETE': return HttpMethod.DELETE;
            case 'PATCH': return HttpMethod.PATCH;
            default: return HttpMethod.GET;
        }
    }

    

    private getDefaultOptions(): EndpointBuildingOptions {
        return {
            includeValidation: true,
            includeCrudEndpoints: true,
            includeCustomEndpoints: true,
            throwsExceptions: true
        };
    }

    

    static createOptions(overrides: Partial<EndpointBuildingOptions> = {}): EndpointBuildingOptions {
        const defaults: EndpointBuildingOptions = {
            includeValidation: true,
            includeCrudEndpoints: true,
            includeCustomEndpoints: true,
            throwsExceptions: true
        };

        return { ...defaults, ...overrides };
    }
}
