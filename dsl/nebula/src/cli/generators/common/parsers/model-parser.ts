import { Aggregate, Entity, Method, Workflow, Model } from "../../../../language/generated/ast.js";
import { ALL_PRIMITIVE_TYPES } from "../utils/type-constants.js";

export { Aggregate, Entity, Method, Workflow, Model };

/**
 * Parse error with location and context information
 */
export interface ParseError {
    code: string;
    message: string;
    context: string;  // e.g., "property 'email' in entity 'User'"
    location?: string;  // e.g., "user.nebula:5:10"
    originalError?: string;
}

export interface EntityData {
    name: string;
    isRoot: boolean;
    properties: PropertyData[];
    rules: RuleData[];
    methods: MethodData[];
    relationships: RelationshipData[];
    parseErrors?: ParseError[];  // Errors encountered during parsing
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
    repository?: RepositoryData;
    webApiEndpoints?: WebAPIEndpointsData;
    relationships: RelationshipData[];
    parseErrors?: ParseError[];  // Errors encountered during parsing
}

export interface RepositoryData {
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
    exceptions?: ExceptionMessagesData;
    parseErrors?: ParseError[];  // Errors encountered during parsing
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
    /**
     * Type resolution cache for performance optimization.
     * Caches resolved Java types to avoid repeated type map lookups and recursive calls.
     */
    private static readonly TYPE_CACHE = new Map<string, string>();

    /**
     * Static type mapping from DSL types to Java types.
     * Created once and reused across all type resolutions.
     */
    private static readonly TYPE_MAP = new Map<string, string>([
        ['String', 'String'],
        ['Integer', 'Integer'],
        ['Long', 'Long'],
        ['Double', 'Double'],
        ['Boolean', 'Boolean'],
        ['Float', 'Float'],
        ['Date', 'LocalDateTime'],
        ['DateTime', 'LocalDateTime'],
        ['LocalDateTime', 'LocalDateTime'],
        ['LocalDate', 'LocalDate'],
        ['BigDecimal', 'BigDecimal'],
        ['void', 'void'],
        ['UnitOfWork', 'UnitOfWork'],
        ['AggregateState', 'AggregateState']
    ]);

    /**
     * Generate a formatted error report from parse errors
     */
    static generateErrorReport(parseErrors: ParseError[]): string {
        if (!parseErrors || parseErrors.length === 0) {
            return '';
        }

        let report = `\n❌ Parse Errors Found (${parseErrors.length} issue${parseErrors.length > 1 ? 's' : ''}):\n\n`;

        for (const error of parseErrors) {
            report += `  [${error.code}] ${error.message}\n`;
            report += `    Context: ${error.context}\n`;
            if (error.originalError) {
                report += `    Details: ${error.originalError}\n`;
            }
            if (error.location) {
                report += `    Location: ${error.location}\n`;
            }
            report += `\n`;
        }

        return report;
    }

    /**
     * Check if ModelData has any parse errors
     */
    static hasParseErrors(modelData: ModelData): boolean {
        if (modelData.parseErrors && modelData.parseErrors.length > 0) {
            return true;
        }

        for (const aggregate of modelData.aggregates) {
            if (aggregate.parseErrors && aggregate.parseErrors.length > 0) {
                return true;
            }

            for (const entity of aggregate.entities) {
                if (entity.parseErrors && entity.parseErrors.length > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Collect all parse errors from ModelData
     */
    static collectAllErrors(modelData: ModelData): ParseError[] {
        const allErrors: ParseError[] = [];

        if (modelData.parseErrors) {
            allErrors.push(...modelData.parseErrors);
        }

        for (const aggregate of modelData.aggregates) {
            if (aggregate.parseErrors) {
                allErrors.push(...aggregate.parseErrors);
            }

            for (const entity of aggregate.entities) {
                if (entity.parseErrors) {
                    allErrors.push(...entity.parseErrors);
                }
            }
        }

        return allErrors;
    }

    /**
     * Clear the type resolution cache.
     *
     * Useful for:
     * - Testing scenarios where cache needs to be reset
     * - Hot-reloading during development
     * - Memory cleanup after processing large projects
     *
     * Note: Cache is automatically managed and rarely needs manual clearing.
     */
    static clearTypeCache(): void {
        ModelParser.TYPE_CACHE.clear();
    }

    /**
     * Get cache statistics for monitoring and debugging.
     *
     * @returns Object with cache size and hit rate information
     */
    static getTypeCacheStats(): { size: number; entries: string[] } {
        return {
            size: ModelParser.TYPE_CACHE.size,
            entries: Array.from(ModelParser.TYPE_CACHE.keys())
        };
    }

    parseModel(model: Model): ModelData {
        const aggregates: AggregateData[] = [];
        const parseErrors: ParseError[] = [];

        // Parse aggregates with error recovery
        for (const aggregate of model.aggregates) {
            try {
                const aggregateData = this.parseAggregate(aggregate);
                aggregates.push(aggregateData);

                // Collect aggregate-level parse errors
                if (aggregateData.parseErrors) {
                    parseErrors.push(...aggregateData.parseErrors);
                }
            } catch (error) {
                parseErrors.push({
                    code: 'AGGREGATE_PARSE_ERROR',
                    message: `Failed to parse aggregate '${aggregate.name || 'unknown'}'`,
                    context: `aggregate '${aggregate.name || 'unknown'}'`,
                    originalError: error instanceof Error ? error.message : String(error)
                });
                // Continue parsing remaining aggregates
            }
        }

        return {
            aggregates,
            exceptions: model.exceptions ? this.parseExceptionMessages(model.exceptions) : undefined,
            parseErrors: parseErrors.length > 0 ? parseErrors : undefined
        };
    }

    parseAggregate(aggregate: Aggregate): AggregateData {
        const entities: EntityData[] = [];
        const methods: MethodData[] = [];
        const workflows: WorkflowData[] = [];
        const parseErrors: ParseError[] = [];

        // Parse entities with error recovery
        for (const entity of aggregate.entities) {
            try {
                const entityData = this.parseEntity(entity);
                entities.push(entityData);

                // Collect entity-level parse errors
                if (entityData.parseErrors) {
                    parseErrors.push(...entityData.parseErrors);
                }
            } catch (error) {
                parseErrors.push({
                    code: 'ENTITY_PARSE_ERROR',
                    message: `Failed to parse entity '${entity.name || 'unknown'}'`,
                    context: `entity '${entity.name || 'unknown'}' in aggregate '${aggregate.name}'`,
                    originalError: error instanceof Error ? error.message : String(error)
                });
                // Continue parsing remaining entities
            }
        }

        // Parse methods with error recovery
        for (const method of aggregate.methods) {
            try {
                methods.push(this.parseMethod(method));
            } catch (error) {
                parseErrors.push({
                    code: 'METHOD_PARSE_ERROR',
                    message: `Failed to parse method '${method.name || 'unknown'}'`,
                    context: `method '${method.name || 'unknown'}' in aggregate '${aggregate.name}'`,
                    originalError: error instanceof Error ? error.message : String(error)
                });
                // Continue parsing remaining methods
            }
        }

        // Parse workflows with error recovery
        for (const workflow of aggregate.workflows) {
            try {
                workflows.push(this.parseWorkflow(workflow));
            } catch (error) {
                parseErrors.push({
                    code: 'WORKFLOW_PARSE_ERROR',
                    message: `Failed to parse workflow '${workflow.name || 'unknown'}'`,
                    context: `workflow '${workflow.name || 'unknown'}' in aggregate '${aggregate.name}'`,
                    originalError: error instanceof Error ? error.message : String(error)
                });
                // Continue parsing remaining workflows
            }
        }

        const relationships = this.extractRelationships(entities);

        return {
            name: aggregate.name,
            entities,
            methods,
            workflows,
            repository: aggregate.repository ? this.parseRepository(aggregate.repository) : undefined,
            webApiEndpoints: aggregate.webApiEndpoints ? this.parseWebAPIEndpoints(aggregate.webApiEndpoints) : undefined,
            relationships,
            parseErrors: parseErrors.length > 0 ? parseErrors : undefined
        };
    }

    parseEntity(entity: Entity): EntityData {
        const properties: PropertyData[] = [];
        const parseErrors: ParseError[] = [];
        const rules: any[] = []; // Business rules removed

        // Parse properties with error recovery
        if (entity.properties) {
            for (const prop of entity.properties) {
                try {
                    properties.push(this.parseProperty(prop));
                } catch (error) {
                    parseErrors.push({
                        code: 'PROPERTY_PARSE_ERROR',
                        message: `Failed to parse property '${prop.name || 'unknown'}'`,
                        context: `property '${prop.name || 'unknown'}' in entity '${entity.name}'`,
                        originalError: error instanceof Error ? error.message : String(error)
                    });
                    // Continue parsing remaining properties
                }
            }
        }

        return {
            name: entity.name,
            isRoot: entity.isRoot || false,
            properties,
            rules,
            methods: [], // Methods removed from grammar
            relationships: [],
            parseErrors: parseErrors.length > 0 ? parseErrors : undefined
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

    parseRepository(repository: any): RepositoryData {
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

    /**
     * Resolve DSL type to Java type with caching.
     *
     * This method checks the cache first before performing type resolution.
     * Caching significantly improves performance for large files with many properties.
     *
     * Performance:
     * - Cache hit: O(1) - instant lookup
     * - Cache miss: O(1) for simple types, O(n) for nested collections (where n = nesting depth)
     *
     * @param type DSL type string (e.g., "String", "List<User>", "Set<Optional<Product>>")
     * @returns Resolved Java type string
     */
    private resolveJavaType(type: string): string {
        // Quick validation
        if (typeof type !== 'string') {
            return 'Object';
        }

        // Check cache first (O(1) lookup)
        const cached = ModelParser.TYPE_CACHE.get(type);
        if (cached !== undefined) {
            return cached;
        }

        // Cache miss - resolve type
        const resolved = this.resolveTypeInternal(type);

        // Store in cache for future calls
        ModelParser.TYPE_CACHE.set(type, resolved);

        return resolved;
    }

    /**
     * Internal type resolution logic (called only on cache miss).
     *
     * Handles:
     * - Simple primitive types (String, Integer, etc.)
     * - Collection types (List<T>, Set<T>)
     * - Custom types (entities, DTOs, enums)
     *
     * @param type DSL type string
     * @returns Resolved Java type string
     */
    private resolveTypeInternal(type: string): string {
        // Check static type map for primitives and built-in types
        const mapped = ModelParser.TYPE_MAP.get(type);
        if (mapped !== undefined) {
            return mapped;
        }

        // Handle collection types (List<T>, Set<T>)
        if (type.startsWith('List<') || type.startsWith('Set<')) {
            const innerType = type.substring(type.indexOf('<') + 1, type.lastIndexOf('>'));
            const javaInnerType = this.resolveJavaType(innerType);  // Recursive - benefits from cache
            return type.startsWith('List<') ? `List<${javaInnerType}>` : `Set<${javaInnerType}>`;
        }

        // Handle Optional<T>
        if (type.startsWith('Optional<')) {
            const innerType = type.substring(type.indexOf('<') + 1, type.lastIndexOf('>'));
            const javaInnerType = this.resolveJavaType(innerType);  // Recursive - benefits from cache
            return `Optional<${javaInnerType}>`;
        }

        // Unknown type - pass through as-is (could be custom entity, DTO, or enum)
        return type;
    }

    private isCollectionType(type: string): boolean {
        if (typeof type !== 'string') return false;
        return type.startsWith('List<') || type.startsWith('Set<') || type.startsWith('Collection<');
    }

    private isEntityType(type: string): boolean {
        if (typeof type !== 'string') return false;
        return !ALL_PRIMITIVE_TYPES.includes(type as any) && !this.isCollectionType(type);
    }

    private isPrimitiveType(type: string): boolean {
        if (typeof type !== 'string') return false;
        return ALL_PRIMITIVE_TYPES.includes(type as any);
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
