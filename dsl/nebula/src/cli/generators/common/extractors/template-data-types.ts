import { AggregateData, EntityData, MethodData, WorkflowData } from "../parsers/model-parser.js";
import { RelationshipMap } from "../analyzers/relationship-analyzer.js";
import { ExceptionsSummary } from "../parsers/exception-parser.js";

export interface GeneratorContext {
    projectName: string;
    packageName: string;
    architecture: string;
    features: string[];
    javaPath: string;
    options: any;
}

export interface TemplateData {
    project: ProjectData;

    aggregate: AggregateData;

    entities: EntityData[];

    methods: MethodData[];

    workflows: WorkflowData[];

    relationships: RelationshipMap;

    validations: ValidationData[];

    configuration: ConfigurationData;

    metadata: MetadataData;

    exceptions?: ExceptionsSummary;
}

export interface ProjectData {
    name: string;
    packageName: string;
    architecture: string;
    features: string[];
    javaPath: string;
    version: string;
    description?: string;
    author?: string;
    license?: string;
    dependencies: string[];
    buildTool: string;
    javaVersion: string;
    springBootVersion: string;
}

export interface ValidationData {
    entityName: string;
    propertyName: string;
    ruleType: string;
    message: string;
    condition: string;
    severity: 'error' | 'warning' | 'info';
    customValidator?: string;
    parameters?: { [key: string]: any };
}

export interface ConfigurationData {
    database: DatabaseConfig;
    events: EventConfig;
    validation: ValidationConfig;
    webApi: WebApiConfig;
    coordination: CoordinationConfig;
    saga: SagaConfig;
}

export interface DatabaseConfig {
    driver: string;
    url: string;
    username: string;
    password: string;
    dialect: string;
    showSql: boolean;
    generateDdl: boolean;
}

export interface EventConfig {
    enabled: boolean;
    broker: string;
    topics: string[];
    serialization: string;
    compression: boolean;
}

export interface ValidationConfig {
    enabled: boolean;
    failFast: boolean;
    customValidators: string[];
    groups: string[];
}

export interface WebApiConfig {
    enabled: boolean;
    basePath: string;
    version: string;
    cors: boolean;
    swagger: boolean;
}

export interface CoordinationConfig {
    enabled: boolean;
    type: string;
    timeout: number;
    retryAttempts: number;
}

export interface SagaConfig {
    enabled: boolean;
    type: string;
    compensationEnabled: boolean;
    timeout: number;
}

export interface MetadataData {
    generatedAt: string;
    version: string;
    generator: string;
    features: string[];
    statistics: {
        aggregates: number;
        entities: number;
        methods: number;
        workflows: number;
        validations: number;
    };
    checksums: {
        [filename: string]: string;
    };
}

export interface ExtractionOptions {
    includeValidations: boolean;
    includeMetadata: boolean;
    includeExceptions: boolean;
    includeRelationships: boolean;
    includeWorkflows: boolean;
}

export interface DataExtractionResult {
    success: boolean;
    data?: TemplateData;
    errors: string[];
    warnings: string[];
}

export interface PropertyValidationRule {
    type: string;
    parameters: any;
    message: string;
    severity: 'error' | 'warning';
}

export interface EntityValidationRules {
    entityName: string;
    properties: { [propertyName: string]: PropertyValidationRule[] };
    classLevel: PropertyValidationRule[];
}
