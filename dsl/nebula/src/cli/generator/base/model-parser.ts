import { Aggregate, Entity, Method, Workflow, Model } from "../../../language/generated/ast.js";

export { Aggregate, Entity, Method, Workflow, Model };

export interface EntityData {
    name: string;
    isRoot: boolean;
    properties: PropertyData[];
    rules: RuleData[];
    methods: MethodData[];
    relationships: RelationshipData[];
}

export interface PropertyData {
    name: string;
    type: string;
    javaType: string;
    isRequired: boolean;
    isCollection: boolean;
    isEntity: boolean;
    isPrimitive: boolean;
    annotations: string[];
    validationRules: ValidationRuleData[];
}

export interface MethodData {
    name: string;
    parameters: ParameterData[];
    returnType: string;
    javaReturnType: string;
    body?: string;
    isPublic: boolean;
    isStatic: boolean;
    annotations: string[];
}

export interface ParameterData {
    name: string;
    type: string;
    javaType: string;
    isRequired: boolean;
    isCollection: boolean;
    isEntity: boolean;
    isPrimitive: boolean;
}

export interface WorkflowData {
    name: string;
    steps: WorkflowStepData[];
    parameters: ParameterData[];
    returnType: string;
    javaReturnType: string;
    isPublic: boolean;
    annotations: string[];
}

export interface WorkflowStepData {
    name: string;
    type: 'method' | 'condition' | 'loop' | 'assignment';
    expression?: string;
    condition?: string;
    body?: string;
    nextStep?: string;
}

export interface RuleData {
    name: string;
    condition: string;
    message: string;
    severity: 'error' | 'warning' | 'info';
    properties: string[];
}

export interface ValidationRuleData {
    type: 'required' | 'min' | 'max' | 'pattern' | 'custom';
    value?: string;
    message: string;
    properties: string[];
}

export interface RelationshipData {
    name: string;
    type: 'OneToMany' | 'ManyToOne' | 'ManyToMany' | 'OneToOne';
    targetEntity: string;
    sourceProperty: string;
    targetProperty: string;
    isBidirectional: boolean;
    cascade: string[];
    fetch: 'EAGER' | 'LAZY';
}

export interface AggregateData {
    name: string;
    entities: EntityData[];
    methods: MethodData[];
    workflows: WorkflowData[];
    customRepository?: CustomRepositoryData;
    webApiEndpoints?: WebAPIEndpointsData;
    relationships: RelationshipData[];
}

export interface CustomRepositoryData {
    name: string;
    methods: MethodData[];
    returnType: string;
    javaReturnType: string;
}


export interface WebAPIEndpointsData {
    endpoints: EndpointData[];
    globalEndpoints: EndpointData[];
}

export interface EndpointData {
    name: string;
    path: string;
    method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
    parameters: ParameterData[];
    returnType: string;
    javaReturnType: string;
    isPublic: boolean;
    annotations: string[];
}

export interface ModelData {
    aggregates: AggregateData[];
    imports: ImportData[];
    exceptions?: ExceptionMessagesData;
}

export interface ImportData {
    name: string;
    path: string;
    isRelative: boolean;
    isExternal: boolean;
}

export interface ExceptionMessagesData {
    messages: ExceptionMessageData[];
}

export interface ExceptionMessageData {
    name: string;
    message: string;
    code: string;
    severity: 'error' | 'warning' | 'info';
}

export class ModelParser {


    parseModel(model: Model): ModelData {
        return {
            aggregates: model.aggregates.map(aggregate => this.parseAggregate(aggregate)),
            imports: this.parseImports(model.imports || []),
            exceptions: model.exceptions ? this.parseExceptionMessages(model.exceptions) : undefined
        };
    }

    parseAggregate(aggregate: Aggregate): AggregateData {
        const entities = aggregate.entities.map(entity => this.parseEntity(entity));
        const methods = aggregate.methods.map(method => this.parseMethod(method));
        const workflows = aggregate.workflows.map(workflow => this.parseWorkflow(workflow));
        const relationships = this.extractRelationships(entities);

        return {
            name: aggregate.name,
            entities,
            methods,
            workflows,
            customRepository: aggregate.customRepository ? this.parseCustomRepository(aggregate.customRepository) : undefined,
            webApiEndpoints: aggregate.webApiEndpoints ? this.parseWebAPIEndpoints(aggregate.webApiEndpoints) : undefined,
            relationships
        };
    }

    parseEntity(entity: Entity): EntityData {
        const properties = entity.properties?.map(prop => this.parseProperty(prop)) || [];
        const rules: any[] = []; // Business rules removed
        const methods = entity.methods?.map(method => this.parseMethod(method)) || [];

        return {
            name: entity.name,
            isRoot: entity.isRoot || false,
            properties,
            rules,
            methods,
            relationships: []
        };
    }

    parseProperty(property: any): PropertyData {
        const type = this.extractType(property.type);
        const javaType = this.resolveJavaType(type);
        const isCollection = this.isCollectionType(type);
        const isEntity = this.isEntityType(type);
        const isPrimitive = this.isPrimitiveType(type);

        return {
            name: property.name,
            type,
            javaType,
            isRequired: property.required || false,
            isCollection,
            isEntity,
            isPrimitive,
            annotations: this.extractAnnotations(property),
            validationRules: this.extractValidationRules(property)
        };
    }

    parseMethod(method: Method): MethodData {
        const parameters = method.parameters.map(param => this.parseParameter(param));
        const returnType = this.extractReturnType(method.returnType);
        const javaReturnType = this.resolveJavaType(returnType);

        return {
            name: method.name,
            parameters,
            returnType,
            javaReturnType,
            body: method.body,
            isPublic: true, // Default to public
            isStatic: false, // Default to instance
            annotations: this.extractAnnotations(method)
        };
    }

    parseWorkflow(workflow: Workflow): WorkflowData {
        const parameters = (workflow as any).parameters?.map((param: any) => this.parseParameter(param)) || [];
        const returnType = this.extractReturnType((workflow as any).returnType);
        const javaReturnType = this.resolveJavaType(returnType);
        const steps = (workflow as any).steps?.map((step: any) => this.parseWorkflowStep(step)) || [];

        return {
            name: workflow.name,
            steps,
            parameters,
            returnType,
            javaReturnType,
            isPublic: true,
            annotations: this.extractAnnotations(workflow)
        };
    }

    parseWorkflowStep(step: any): WorkflowStepData {
        return {
            name: step.name,
            type: step.type || 'method',
            expression: step.expression,
            condition: step.condition,
            body: step.body,
            nextStep: step.nextStep
        };
    }

    parseParameter(parameter: any): ParameterData {
        const type = this.extractType(parameter.type);
        const javaType = this.resolveJavaType(type);
        const isCollection = this.isCollectionType(type);
        const isEntity = this.isEntityType(type);
        const isPrimitive = this.isPrimitiveType(type);

        return {
            name: parameter.name,
            type,
            javaType,
            isRequired: parameter.required || false,
            isCollection,
            isEntity,
            isPrimitive
        };
    }

    parseRule(rule: any): RuleData {
        return {
            name: rule.name,
            condition: rule.condition,
            message: rule.message,
            severity: rule.severity || 'error',
            properties: rule.properties || []
        };
    }

    parseCustomRepository(repository: any): CustomRepositoryData {
        const methods = repository.methods?.map((method: any) => this.parseMethod(method)) || [];
        const returnType = this.extractReturnType(repository.returnType);
        const javaReturnType = this.resolveJavaType(returnType);

        return {
            name: repository.name,
            methods,
            returnType,
            javaReturnType
        };
    }


    parseWebAPIEndpoints(endpoints: any): WebAPIEndpointsData {
        return {
            endpoints: endpoints.endpoints?.map((endpoint: any) => this.parseEndpoint(endpoint)) || [],
            globalEndpoints: endpoints.globalEndpoints?.map((endpoint: any) => this.parseEndpoint(endpoint)) || []
        };
    }

    parseEndpoint(endpoint: any): EndpointData {
        const parameters = endpoint.parameters?.map((param: any) => this.parseParameter(param)) || [];
        const returnType = this.extractReturnType(endpoint.returnType);
        const javaReturnType = this.resolveJavaType(returnType);

        return {
            name: endpoint.name,
            path: endpoint.path,
            method: endpoint.method || 'GET',
            parameters,
            returnType,
            javaReturnType,
            isPublic: true,
            annotations: this.extractAnnotations(endpoint)
        };
    }

    parseImports(imports: any[]): ImportData[] {
        return imports.map(imp => ({
            name: imp.name,
            path: imp.path,
            isRelative: imp.isRelative || false,
            isExternal: imp.isExternal || false
        }));
    }

    parseExceptionMessages(exceptions: any): ExceptionMessagesData {
        return {
            messages: exceptions.messages?.map((msg: any) => ({
                name: msg.name,
                message: msg.message,
                code: msg.code,
                severity: msg.severity || 'error'
            })) || []
        };
    }

    extractRelationships(entities: EntityData[]): RelationshipData[] {
        const relationships: RelationshipData[] = [];

        for (const entity of entities) {
            for (const property of entity.properties) {
                if (property.isEntity && !property.isCollection) {
                    // OneToOne or ManyToOne
                    relationships.push({
                        name: `${entity.name}_${property.name}`,
                        type: 'ManyToOne',
                        targetEntity: property.type,
                        sourceProperty: property.name,
                        targetProperty: 'id',
                        isBidirectional: false,
                        cascade: ['PERSIST'],
                        fetch: 'LAZY'
                    });
                } else if (property.isEntity && property.isCollection) {
                    relationships.push({
                        name: `${entity.name}_${property.name}`,
                        type: 'OneToMany',
                        targetEntity: property.type,
                        sourceProperty: property.name,
                        targetProperty: 'id',
                        isBidirectional: false,
                        cascade: ['PERSIST'],
                        fetch: 'LAZY'
                    });
                }
            }
        }

        return relationships;
    }

    private extractType(type: any): string {
        if (!type) return 'void';
        if (typeof type === 'string') return type;
        if (type.name) return type.name;
        if (type.type) return type.type;
        return 'void';
    }

    private extractReturnType(returnType: any): string {
        if (!returnType) return 'void';
        if (typeof returnType === 'string') return returnType;
        if (returnType.name) return returnType.name;
        if (returnType.type) return returnType.type;
        return 'void';
    }

    private resolveJavaType(type: string): string {
        if (typeof type !== 'string') {
            return 'Object';
        }

        const typeMap: { [key: string]: string } = {
            'String': 'String',
            'Integer': 'Integer',
            'Long': 'Long',
            'Double': 'Double',
            'Boolean': 'Boolean',
            'Date': 'LocalDateTime',
            'DateTime': 'LocalDateTime',
            'BigDecimal': 'BigDecimal',
            'void': 'void',
            'UnitOfWork': 'UnitOfWork',
            'UserDto': 'UserDto',
            'AggregateState': 'AggregateState'
        };

        if (typeMap[type]) {
            return typeMap[type];
        }

        if (type.startsWith('List<') || type.startsWith('Set<')) {
            const innerType = type.substring(type.indexOf('<') + 1, type.lastIndexOf('>'));
            const javaInnerType = this.resolveJavaType(innerType);
            return type.startsWith('List<') ? `List<${javaInnerType}>` : `Set<${javaInnerType}>`;
        }

        return type;
    }

    private isCollectionType(type: string): boolean {
        if (typeof type !== 'string') return false;
        return type.startsWith('List<') || type.startsWith('Set<') || type.startsWith('Collection<');
    }

    private isEntityType(type: string): boolean {
        if (typeof type !== 'string') return false;
        const primitiveTypes = ['String', 'Integer', 'Long', 'Double', 'Boolean', 'Date', 'DateTime', 'BigDecimal', 'void'];
        return !primitiveTypes.includes(type) && !this.isCollectionType(type);
    }

    private isPrimitiveType(type: string): boolean {
        if (typeof type !== 'string') return false;
        const primitiveTypes = ['String', 'Integer', 'Long', 'Double', 'Boolean', 'Date', 'DateTime', 'BigDecimal'];
        return primitiveTypes.includes(type);
    }

    private extractAnnotations(node: any): string[] {
        if (!node.annotations) return [];
        return Array.isArray(node.annotations) ? node.annotations : [node.annotations];
    }

    private extractValidationRules(property: any): ValidationRuleData[] {
        if (!property.validationRules) return [];
        return property.validationRules.map((rule: any) => ({
            type: rule.type || 'required',
            value: rule.value,
            message: rule.message || `${property.name} is invalid`,
            properties: rule.properties || [property.name]
        }));
    }
}
