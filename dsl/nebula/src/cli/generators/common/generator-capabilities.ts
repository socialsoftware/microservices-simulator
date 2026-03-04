


import { Aggregate, Entity } from "../../../language/generated/ast.js";
import { UnifiedTypeResolver } from "./unified-type-resolver.js";
import { TemplateManager } from "../../utils/template-manager.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../../utils/error-handler.js";
import { FileWriter } from "../../utils/file-writer.js";
import { ImportManager, ImportManagerFactory } from "../../utils/import-manager.js";
import { TemplateContextBuilder } from "./template-context-builder.js";
import { PackageNameBuilder, PackageBuilderFactory } from "../../utils/package-name-builder.js";
import { getGlobalConfig } from "./config.js";



export interface GeneratorCapabilities {
    templateRenderer: TemplateRenderer;
    typeResolver: TypeResolver;
    importBuilder: ImportBuilder;
    contextBuilder: ContextBuilder;
    fileWriter: FileWriter;
    packageBuilder: PackageBuilder;
    validator: GeneratorValidator;
}



export interface TemplateRenderer {
    render(templatePath: string, context: any): string;
    renderRaw(template: string, context: any): string;
    preloadTemplates(paths: string[]): Promise<void>;
}



export interface TypeResolver {
    resolve(type: any, context?: string): string;
    resolveForEntity(type: any): string;
    resolveForDto(type: any): string;
    resolveForWebApi(type: any): string;
    resolveForService(type: any): string;
    isEntityType(type: any): boolean;
    isCollectionType(type: any): boolean;
    isPrimitiveType(type: string): boolean;
}



export interface ImportBuilder {
    addJavaImport(className: string): void;
    addSpringImport(annotation: string): void;
    addProjectImport(projectName: string, ...path: string[]): void;
    addCustomImport(importPath: string): void;
    formatImports(): string[];
    reset(): void;
}



export interface ContextBuilder {
    withAggregate(aggregate: Aggregate): this;
    withEntity(entity: Entity): this;
    withProjectName(projectName: string): this;
    withPackageName(packageName: string): this;
    withCustomData(key: string, value: any): this;
    build(): GeneratorContext;
    reset(): this;
}



export interface PackageBuilder {
    buildMicroservicePackage(projectName: string, aggregate: string, component: string): string;
    buildCoordinationPackage(projectName: string, component: string): string;
    buildSharedPackage(projectName: string, component: string): string;
    buildCustomPackage(projectName: string, ...segments: string[]): string;
}



export interface GeneratorValidator {
    validateAggregate(aggregate: Aggregate): void;
    validateEntity(entity: Entity): void;
    validateOptions(options: any, requiredFields: string[]): void;
    findRootEntity(aggregate: Aggregate): Entity;
}



export interface GeneratorContext {
    aggregate?: Aggregate;
    entity?: Entity;
    projectName?: string;
    packageName?: string;
    customData?: Record<string, any>;
}



export abstract class BaseGenerator {
    protected capabilities: GeneratorCapabilities;

    constructor(capabilities?: Partial<GeneratorCapabilities>) {
        this.capabilities = this.createCapabilities(capabilities);
    }

    

    private createCapabilities(overrides?: Partial<GeneratorCapabilities>): GeneratorCapabilities {
        
        const defaultProjectName = 'project'; 

        const defaults: GeneratorCapabilities = {
            templateRenderer: new DefaultTemplateRenderer(),
            typeResolver: new DefaultTypeResolver(),
            importBuilder: new DefaultImportBuilder(defaultProjectName),
            contextBuilder: new DefaultContextBuilder(),
            fileWriter: FileWriter,
            packageBuilder: new DefaultPackageBuilder(),
            validator: new DefaultGeneratorValidator()
        };

        return { ...defaults, ...overrides };
    }

    

    protected render(templatePath: string, context: any): string {
        return this.capabilities.templateRenderer.render(templatePath, context);
    }

    protected resolveType(type: any, context?: string): string {
        return this.capabilities.typeResolver.resolve(type, context);
    }

    protected buildContext(): ContextBuilder {
        return this.capabilities.contextBuilder.reset();
    }

    protected validateAggregate(aggregate: Aggregate): void {
        this.capabilities.validator.validateAggregate(aggregate);
    }

    protected findRootEntity(aggregate: Aggregate): Entity {
        return this.capabilities.validator.findRootEntity(aggregate);
    }
}



export class DefaultTemplateRenderer implements TemplateRenderer {
    private templateManager = TemplateManager.getInstance();

    render(templatePath: string, context: any): string {
        return this.templateManager.renderTemplate(templatePath, context);
    }

    renderRaw(template: string, context: any): string {
        
        const Handlebars = require('handlebars');
        const compiledTemplate = Handlebars.compile(template, { noEscape: true });
        return compiledTemplate(context);
    }

    async preloadTemplates(paths: string[]): Promise<void> {
        await this.templateManager.preloadTemplates(paths);
    }
}

export class DefaultTypeResolver implements TypeResolver {
    resolve(type: any, context?: string): string {
        switch (context) {
            case 'entity': return UnifiedTypeResolver.resolveForEntity(type);
            case 'dto': return UnifiedTypeResolver.resolveForDto(type);
            case 'webapi': return UnifiedTypeResolver.resolveForWebApi(type);
            case 'service': return UnifiedTypeResolver.resolveForService(type);
            default: return UnifiedTypeResolver.resolve(type);
        }
    }

    resolveForEntity(type: any): string {
        return UnifiedTypeResolver.resolveForEntity(type);
    }

    resolveForDto(type: any): string {
        return UnifiedTypeResolver.resolveForDto(type);
    }

    resolveForWebApi(type: any): string {
        return UnifiedTypeResolver.resolveForWebApi(type);
    }

    resolveForService(type: any): string {
        return UnifiedTypeResolver.resolveForService(type);
    }

    isEntityType(type: any): boolean {
        return UnifiedTypeResolver.isEntityType(type);
    }

    isCollectionType(type: any): boolean {
        return UnifiedTypeResolver.isCollectionType(type);
    }

    isPrimitiveType(type: string): boolean {
        return UnifiedTypeResolver.isPrimitiveType(type);
    }
}

export class DefaultImportBuilder implements ImportBuilder {
    private importManager: ImportManager;

    constructor(projectName: string) {
        this.importManager = ImportManagerFactory.createForMicroservice(projectName);
    }

    addJavaImport(className: string): void {
        this.importManager.addJavaImport(className);
    }

    addSpringImport(annotation: string): void {
        this.importManager.addSpringImport(annotation);
    }

    addProjectImport(projectName: string, ...path: string[]): void {
        const fullPath = path.join('.');
        this.importManager.addCustomImport(`${getGlobalConfig().getBasePackage()}.${projectName.toLowerCase()}.${fullPath}.*`);
    }

    addCustomImport(importPath: string): void {
        this.importManager.addCustomImport(importPath);
    }

    formatImports(): string[] {
        return this.importManager.formatImports();
    }

    reset(): void {
        this.importManager.clear();
    }

    
    getImportManager(): ImportManager {
        return this.importManager;
    }
}

export class DefaultContextBuilder implements ContextBuilder {
    private templateContextBuilder: TemplateContextBuilder;

    constructor() {
        this.templateContextBuilder = TemplateContextBuilder.create();
    }

    withAggregate(aggregate: Aggregate): this {
        this.templateContextBuilder.withAggregate(aggregate);
        return this;
    }

    withEntity(entity: Entity): this {
        this.templateContextBuilder.withEntity(entity);
        return this;
    }

    withProjectName(projectName: string): this {
        this.templateContextBuilder.withProject(projectName);
        return this;
    }

    withPackageName(packageName: string): this {
        this.templateContextBuilder.withPackage(packageName);
        return this;
    }

    withCustomData(key: string, value: any): this {
        this.templateContextBuilder.withCustomData(key, value);
        return this;
    }

    build(): GeneratorContext {
        const templateContext = this.templateContextBuilder.buildPartial();

        
        return {
            aggregate: templateContext.customData?.aggregate,
            entity: templateContext.customData?.entity,
            projectName: templateContext.projectName,
            packageName: templateContext.packageName,
            customData: templateContext.customData
        };
    }

    reset(): this {
        this.templateContextBuilder.reset();
        return this;
    }

    
    getTemplateContextBuilder(): TemplateContextBuilder {
        return this.templateContextBuilder;
    }
}

export class DefaultPackageBuilder implements PackageBuilder {
    private packageBuilder: PackageNameBuilder;

    constructor() {
        this.packageBuilder = PackageBuilderFactory.createStandard();
    }

    buildMicroservicePackage(projectName: string, aggregate: string, component: string): string {
        return this.packageBuilder.buildMicroservicePackage(projectName, aggregate, component);
    }

    buildCoordinationPackage(projectName: string, component: string): string {
        return this.packageBuilder.buildCoordinationPackage(projectName, component);
    }

    buildSharedPackage(projectName: string, component: string): string {
        return this.packageBuilder.buildSharedPackage(projectName, component);
    }

    buildCustomPackage(projectName: string, ...segments: string[]): string {
        return this.packageBuilder.buildCustomPackage(projectName, ...segments);
    }

    
    getPackageBuilder(): PackageNameBuilder {
        return this.packageBuilder;
    }
}

export class DefaultGeneratorValidator implements GeneratorValidator {
    validateAggregate(aggregate: Aggregate): void {
        if (!aggregate.name) {
            ErrorHandler.handle(
                new Error('Aggregate must have a name'),
                ErrorUtils.aggregateContext('validate aggregate', 'unknown', 'validator'),
                ErrorSeverity.FATAL
            );
        }

        if (!aggregate.entities || aggregate.entities.length === 0) {
            ErrorHandler.handle(
                new Error('Aggregate must have at least one entity'),
                ErrorUtils.aggregateContext('validate aggregate', aggregate.name, 'validator'),
                ErrorSeverity.FATAL
            );
        }

        const rootEntities = aggregate.entities.filter((e: any) => e.isRoot);
        if (rootEntities.length === 0) {
            ErrorHandler.handle(
                new Error('Aggregate must have a root entity'),
                ErrorUtils.aggregateContext('validate aggregate', aggregate.name, 'validator'),
                ErrorSeverity.FATAL
            );
        }

        if (rootEntities.length > 1) {
            ErrorHandler.handle(
                new Error('Aggregate can have only one root entity'),
                ErrorUtils.aggregateContext('validate aggregate', aggregate.name, 'validator'),
                ErrorSeverity.FATAL
            );
        }
    }

    validateEntity(entity: Entity): void {
        if (!entity.name) {
            ErrorHandler.handle(
                new Error('Entity must have a name'),
                ErrorUtils.entityContext('validate entity', 'unknown', 'unknown', 'validator'),
                ErrorSeverity.FATAL
            );
        }
    }

    validateOptions(options: any, requiredFields: string[]): void {
        const missing = requiredFields.filter(field => !options[field]);
        if (missing.length > 0) {
            ErrorHandler.handle(
                new Error(`Missing required options: ${missing.join(', ')}`),
                ErrorUtils.aggregateContext('validate options', 'unknown', 'validator', { missing }),
                ErrorSeverity.FATAL
            );
        }
    }

    findRootEntity(aggregate: Aggregate): Entity {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            ErrorHandler.handle(
                new Error(`No root entity found in aggregate ${aggregate.name}`),
                ErrorUtils.aggregateContext('find root entity', aggregate.name, 'validator'),
                ErrorSeverity.FATAL
            );
            throw new Error('This will never be reached'); 
        }
        return rootEntity;
    }
}




export class GeneratorCapabilitiesFactory {
    private static createCapabilitiesForProject(projectName: string, type: 'microservice' | 'coordination' | 'sagas' = 'microservice'): GeneratorCapabilities {
        let importManager: ImportManager;

        switch (type) {
            case 'coordination':
                importManager = ImportManagerFactory.createForCoordination(projectName);
                break;
            case 'sagas':
                importManager = ImportManagerFactory.createForSagas(projectName);
                break;
            default:
                importManager = ImportManagerFactory.createForMicroservice(projectName);
                break;
        }

        const importBuilder = new DefaultImportBuilder(projectName);
        
        (importBuilder as any).importManager = importManager;

        return {
            templateRenderer: new DefaultTemplateRenderer(),
            typeResolver: new DefaultTypeResolver(),
            importBuilder,
            contextBuilder: new DefaultContextBuilder(),
            fileWriter: FileWriter,
            packageBuilder: new DefaultPackageBuilder(),
            validator: new DefaultGeneratorValidator()
        };
    }

    

    static createWebApiCapabilities(projectName: string = 'project'): GeneratorCapabilities {
        return this.createCapabilitiesForProject(projectName, 'coordination');
    }

    

    static createEntityCapabilities(projectName: string = 'project'): GeneratorCapabilities {
        return this.createCapabilitiesForProject(projectName, 'microservice');
    }

    

    static createServiceCapabilities(projectName: string = 'project'): GeneratorCapabilities {
        return this.createCapabilitiesForProject(projectName, 'microservice');
    }

    

    static createValidationCapabilities(projectName: string = 'project'): GeneratorCapabilities {
        return this.createCapabilitiesForProject(projectName, 'microservice');
    }

    

    static createEventCapabilities(projectName: string = 'project'): GeneratorCapabilities {
        return this.createCapabilitiesForProject(projectName, 'microservice');
    }

    

    static createSagaCapabilities(projectName: string = 'project'): GeneratorCapabilities {
        return this.createCapabilitiesForProject(projectName, 'sagas');
    }
}
