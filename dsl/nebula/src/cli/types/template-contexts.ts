/**
 * Template Context Types
 *
 * Typed template contexts for Handlebars rendering.
 * Provides IntelliSense support and compile-time type checking for all templates.
 */

/**
 * Base context shared across all templates
 */
export interface BaseTemplateContext {
    packageName: string;
    className: string;
    imports: string[];
}

/**
 * Entity template context
 */
export interface EntityTemplateContext extends BaseTemplateContext {
    fields: FieldContext[];
    constructors: ConstructorContext[];
    methods: MethodContext[];
    invariants?: InvariantContext[];
    eventSubscriptions?: EventSubscriptionContext[];
    annotations: string[];
    isRootEntity: boolean;
    superClass?: string;
}

/**
 * Field context for entity properties
 */
export interface FieldContext {
    type: string;
    name: string;
    capitalizedName: string;
    isFinal: boolean;
    isOptional: boolean;
    isId: boolean;
    isCollection: boolean;
    elementType?: string;
    annotations: string[];
    defaultValue?: string;
    comment?: string;
}

/**
 * Constructor context
 */
export interface ConstructorContext {
    parameters: ParameterContext[];
    body: string;
    isDefault: boolean;
    isCopyConstructor: boolean;
    annotations?: string[];
}

/**
 * Parameter context
 */
export interface ParameterContext {
    type: string;
    name: string;
    annotations?: string[];
}

/**
 * Method context
 */
export interface MethodContext {
    returnType: string;
    name: string;
    parameters: ParameterContext[];
    body: string;
    annotations: string[];
    visibility: 'public' | 'protected' | 'private';
    isStatic?: boolean;
    isFinal?: boolean;
    comment?: string;
}

/**
 * Invariant context
 */
export interface InvariantContext {
    methodName: string;
    condition: string;
    description?: string;
}

/**
 * Event subscription context
 */
export interface EventSubscriptionContext {
    eventName: string;
    subscriptionClassName: string;
    handlerMethod: string;
}

/**
 * Service template context
 */
export interface ServiceTemplateContext extends BaseTemplateContext {
    aggregateName: string;
    entityName: string;
    repositoryName: string;
    factoryName: string;
    crudMethods: CrudMethodContext[];
    collectionMethods: CollectionMethodContext[];
    customMethods: MethodContext[];
    hasCrudMethods: boolean;
    hasCollectionMethods: boolean;
}

/**
 * CRUD method context
 */
export interface CrudMethodContext {
    name: string;
    returnType: string;
    parameters: ParameterContext[];
    body: string;
    httpMethod?: 'GET' | 'POST' | 'PUT' | 'DELETE';
    operation: 'create' | 'read' | 'update' | 'delete' | 'list';
}

/**
 * Collection method context
 */
export interface CollectionMethodContext {
    name: string;
    returnType: string;
    parameters: ParameterContext[];
    body: string;
    operation: 'add' | 'remove' | 'update' | 'addBatch';
    collectionType: 'List' | 'Set';
    elementType: string;
}

/**
 * Repository template context
 */
export interface RepositoryTemplateContext extends BaseTemplateContext {
    entityName: string;
    aggregateIdType: string;
    customQueries: CustomQueryContext[];
    hasCustomQueries: boolean;
}

/**
 * Custom query context
 */
export interface CustomQueryContext {
    name: string;
    query: string;
    returnType: string;
    parameters: QueryParameterContext[];
    annotations: string[];
}

/**
 * Query parameter context
 */
export interface QueryParameterContext {
    name: string;
    type: string;
    annotation: string;
}

/**
 * Factory template context
 */
export interface FactoryTemplateContext extends BaseTemplateContext {
    entityName: string;
    dtoName: string;
    aggregateIdType: string;
    hasCreateMethod: boolean;
    hasCreateDtoMethod: boolean;
    hasCreateFromExistingMethod: boolean;
}

/**
 * Controller template context
 */
export interface ControllerTemplateContext extends BaseTemplateContext {
    basePath: string;
    functionalitiesName: string;
    endpoints: EndpointContext[];
    hasCrudEndpoints: boolean;
    hasCustomEndpoints: boolean;
}

/**
 * Endpoint context
 */
export interface EndpointContext {
    httpMethod: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
    path: string;
    methodName: string;
    parameters: EndpointParameterContext[];
    returnType: string;
    body: string;
    annotations: string[];
}

/**
 * Endpoint parameter context
 */
export interface EndpointParameterContext {
    annotation: string;
    type: string;
    name: string;
}

/**
 * DTO template context
 */
export interface DtoTemplateContext extends BaseTemplateContext {
    fields: DtoFieldContext[];
    isRequest: boolean;
    isResponse: boolean;
    hasValidations: boolean;
}

/**
 * DTO field context
 */
export interface DtoFieldContext {
    type: string;
    name: string;
    capitalizedName: string;
    validations: string[];
    defaultValue?: string;
}

/**
 * Event template context
 */
export interface EventTemplateContext extends BaseTemplateContext {
    fields: EventFieldContext[];
    isPublished: boolean;
    isSubscribed: boolean;
    eventType: 'domain' | 'integration';
}

/**
 * Event field context
 */
export interface EventFieldContext {
    type: string;
    name: string;
    capitalizedName: string;
}

/**
 * Event handler template context
 */
export interface EventHandlerTemplateContext extends BaseTemplateContext {
    eventName: string;
    handlerMethodName: string;
    aggregateName: string;
    handlerBody: string;
}

/**
 * Functionalities template context
 */
export interface FunctionalitiesTemplateContext extends BaseTemplateContext {
    aggregateName: string;
    serviceName: string;
    crudMethods: FunctionalityMethodContext[];
    customMethods: FunctionalityMethodContext[];
    hasCrudMethods: boolean;
}

/**
 * Functionality method context
 */
export interface FunctionalityMethodContext {
    name: string;
    returnType: string;
    parameters: ParameterContext[];
    body: string;
    createsUnitOfWork: boolean;
    operation?: 'create' | 'read' | 'update' | 'delete' | 'list';
}

/**
 * Saga template context
 */
export interface SagaTemplateContext extends BaseTemplateContext {
    workflowName: string;
    steps: SagaStepContext[];
    aggregateName: string;
    operation: string;
}

/**
 * Saga step context
 */
export interface SagaStepContext {
    stepName: string;
    stepType: 'sync' | 'async';
    forwardAction: string;
    compensationAction?: string;
    dependencies: string[];
}

/**
 * Configuration template context (pom.xml, application.properties)
 */
export interface ConfigTemplateContext {
    projectName: string;
    basePackage: string;
    dependencies: DependencyContext[];
    properties: PropertyContext[];
}

/**
 * Maven dependency context
 */
export interface DependencyContext {
    groupId: string;
    artifactId: string;
    version?: string;
    scope?: string;
}

/**
 * Configuration property context
 */
export interface PropertyContext {
    key: string;
    value: string;
    comment?: string;
}

/**
 * Enum template context
 */
export interface EnumTemplateContext extends BaseTemplateContext {
    values: EnumValueContext[];
}

/**
 * Enum value context
 */
export interface EnumValueContext {
    name: string;
    ordinal?: number;
    comment?: string;
}
