




export interface BaseTemplateContext {
    packageName: string;
    className: string;
    imports: string[];
}



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



export interface ConstructorContext {
    parameters: ParameterContext[];
    body: string;
    isDefault: boolean;
    isCopyConstructor: boolean;
    annotations?: string[];
}



export interface ParameterContext {
    type: string;
    name: string;
    annotations?: string[];
}



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



export interface InvariantContext {
    methodName: string;
    condition: string;
    description?: string;
}



export interface EventSubscriptionContext {
    eventName: string;
    subscriptionClassName: string;
    handlerMethod: string;
}



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



export interface CrudMethodContext {
    name: string;
    returnType: string;
    parameters: ParameterContext[];
    body: string;
    httpMethod?: 'GET' | 'POST' | 'PUT' | 'DELETE';
    operation: 'create' | 'read' | 'update' | 'delete' | 'list';
}



export interface CollectionMethodContext {
    name: string;
    returnType: string;
    parameters: ParameterContext[];
    body: string;
    operation: 'add' | 'remove' | 'update' | 'addBatch';
    collectionType: 'List' | 'Set';
    elementType: string;
}



export interface RepositoryTemplateContext extends BaseTemplateContext {
    entityName: string;
    aggregateIdType: string;
    customQueries: CustomQueryContext[];
    hasCustomQueries: boolean;
}



export interface CustomQueryContext {
    name: string;
    query: string;
    returnType: string;
    parameters: QueryParameterContext[];
    annotations: string[];
}



export interface QueryParameterContext {
    name: string;
    type: string;
    annotation: string;
}



export interface FactoryTemplateContext extends BaseTemplateContext {
    entityName: string;
    dtoName: string;
    aggregateIdType: string;
    hasCreateMethod: boolean;
    hasCreateDtoMethod: boolean;
    hasCreateFromExistingMethod: boolean;
}



export interface ControllerTemplateContext extends BaseTemplateContext {
    basePath: string;
    functionalitiesName: string;
    endpoints: EndpointContext[];
    hasCrudEndpoints: boolean;
    hasCustomEndpoints: boolean;
}



export interface EndpointContext {
    httpMethod: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
    path: string;
    methodName: string;
    parameters: EndpointParameterContext[];
    returnType: string;
    body: string;
    annotations: string[];
}



export interface EndpointParameterContext {
    annotation: string;
    type: string;
    name: string;
}



export interface DtoTemplateContext extends BaseTemplateContext {
    fields: DtoFieldContext[];
    isRequest: boolean;
    isResponse: boolean;
    hasValidations: boolean;
}



export interface DtoFieldContext {
    type: string;
    name: string;
    capitalizedName: string;
    validations: string[];
    defaultValue?: string;
}



export interface EventTemplateContext extends BaseTemplateContext {
    fields: EventFieldContext[];
    isPublished: boolean;
    isSubscribed: boolean;
    eventType: 'domain' | 'integration';
}



export interface EventFieldContext {
    type: string;
    name: string;
    capitalizedName: string;
}



export interface EventHandlerTemplateContext extends BaseTemplateContext {
    eventName: string;
    handlerMethodName: string;
    aggregateName: string;
    handlerBody: string;
}



export interface FunctionalitiesTemplateContext extends BaseTemplateContext {
    aggregateName: string;
    serviceName: string;
    crudMethods: FunctionalityMethodContext[];
    customMethods: FunctionalityMethodContext[];
    hasCrudMethods: boolean;
}



export interface FunctionalityMethodContext {
    name: string;
    returnType: string;
    parameters: ParameterContext[];
    body: string;
    createsUnitOfWork: boolean;
    operation?: 'create' | 'read' | 'update' | 'delete' | 'list';
}



export interface SagaTemplateContext extends BaseTemplateContext {
    workflowName: string;
    steps: SagaStepContext[];
    aggregateName: string;
    operation: string;
}



export interface SagaStepContext {
    stepName: string;
    stepType: 'sync' | 'async';
    forwardAction: string;
    compensationAction?: string;
    dependencies: string[];
}



export interface ConfigTemplateContext {
    projectName: string;
    basePackage: string;
    dependencies: DependencyContext[];
    properties: PropertyContext[];
}



export interface DependencyContext {
    groupId: string;
    artifactId: string;
    version?: string;
    scope?: string;
}



export interface PropertyContext {
    key: string;
    value: string;
    comment?: string;
}



export interface EnumTemplateContext extends BaseTemplateContext {
    values: EnumValueContext[];
}



export interface EnumValueContext {
    name: string;
    ordinal?: number;
    comment?: string;
}
