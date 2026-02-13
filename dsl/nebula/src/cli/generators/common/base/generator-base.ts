/**
 * Unified Generator Base Class
 *
 * Provides common functionality for all code generators in the system.
 * Consolidates functionality that was previously duplicated across:
 * - OrchestrationBase
 * - EventBaseGenerator
 * - ValidationBaseGenerator
 * - ConfigBaseGenerator
 * - WebAPIBaseGenerator
 *
 * All generators should extend this class to ensure consistency and reduce duplication.
 */

import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { TemplateManager } from "../../../utils/template-manager.js";
import { UnifiedTypeResolver } from "../unified-type-resolver.js";
import { getGlobalConfig } from "../config.js";
import * as fs from 'fs';

/**
 * Base options that all generators receive
 */
export interface GeneratorOptions {
    projectName: string;
    basePackage?: string;
    architecture?: string;
    outputDir?: string;
}

/**
 * Unified base class for all code generators.
 *
 * Provides:
 * - Template rendering utilities
 * - String manipulation helpers
 * - Package name generation
 * - File I/O operations
 * - Common aggregate utilities
 */
export abstract class GeneratorBase {
    protected templateManager: TemplateManager;
    protected typeResolver: typeof UnifiedTypeResolver;

    constructor(protected options?: GeneratorOptions) {
        this.templateManager = TemplateManager.getInstance();
        this.typeResolver = UnifiedTypeResolver;
    }

    // ============================================================================
    // TEMPLATE RENDERING
    // ============================================================================

    /**
     * Render a template file using the cached template manager.
     * This is the preferred method for rendering templates.
     */
    protected renderTemplate(templatePath: string, context: any): string {
        return this.templateManager.renderTemplate(templatePath, context);
    }

    /**
     * Get template manager instance for advanced usage
     */
    protected getTemplateManager(): TemplateManager {
        return this.templateManager;
    }

    // ============================================================================
    // STRING UTILITIES
    // ============================================================================

    /**
     * Capitalize first letter of a string
     */
    protected capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    /**
     * Convert string to lowercase
     */
    protected lowercase(str: string): string {
        return str ? str.toLowerCase() : '';
    }

    /**
     * Convert string to UPPERCASE
     */
    protected uppercase(str: string): string {
        return str ? str.toUpperCase() : '';
    }

    /**
     * Convert string to camelCase
     */
    protected toCamelCase(str: string): string {
        if (!str) return '';
        return str.charAt(0).toLowerCase() + str.slice(1);
    }

    /**
     * Convert string to kebab-case
     */
    protected toKebabCase(str: string): string {
        return str.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase();
    }

    /**
     * Convert string to snake_case
     */
    protected toSnakeCase(str: string): string {
        return str.replace(/([a-z])([A-Z])/g, '$1_$2').toLowerCase();
    }

    // ============================================================================
    // AGGREGATE UTILITIES
    // ============================================================================

    /**
     * Find the root entity of an aggregate
     * @throws Error if no root entity found
     */
    protected findRootEntity(aggregate: Aggregate): Entity {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }
        return rootEntity;
    }

    /**
     * Create a naming object with various case formats for an aggregate
     */
    protected createAggregateNaming(aggregateName: string) {
        return {
            original: aggregateName,
            capitalized: this.capitalize(aggregateName),
            lower: aggregateName.toLowerCase(),
            camelCase: this.toCamelCase(aggregateName),
            kebabCase: this.toKebabCase(aggregateName),
            snakeCase: this.toSnakeCase(aggregateName)
        };
    }

    // ============================================================================
    // PACKAGE NAME GENERATION
    // ============================================================================

    /**
     * Get the base package from configuration
     */
    protected getBasePackage(): string {
        if (this.options?.basePackage) {
            return this.options.basePackage;
        }
        const config = getGlobalConfig();
        return config.getBasePackage();
    }

    /**
     * Generate a full Java package name
     */
    protected generatePackageName(
        projectName: string,
        aggregateName: string,
        subPackage: string,
        ...additionalSubPackages: string[]
    ): string {
        const basePackage = this.getBasePackage();
        const microservicePackage = `microservices.${aggregateName.toLowerCase()}`;
        const subPackages = [subPackage, ...additionalSubPackages].filter(p => p).join('.');
        return `${basePackage}.${projectName.toLowerCase()}.${microservicePackage}.${subPackages}`;
    }

    /**
     * Build a simple package name from segments
     */
    protected buildPackageName(...segments: string[]): string {
        return segments.filter(s => s).join('.');
    }

    // ============================================================================
    // TYPE RESOLUTION
    // ============================================================================

    /**
     * Resolve a DSL type to Java type
     */
    protected resolveJavaType(type: any): string {
        return this.typeResolver.resolveJavaType(type);
    }

    /**
     * Check if a type is a collection (List, Set)
     */
    protected isCollectionType(type: any): boolean {
        return this.typeResolver.isCollectionType(type);
    }

    /**
     * Check if a type is an entity type
     */
    protected isEntityType(type: any): boolean {
        return this.typeResolver.isEntityType(type);
    }

    /**
     * Get the element type from a collection (List<T> → T)
     */
    protected getElementType(type: any): string | undefined {
        return this.typeResolver.getElementType(type);
    }

    // ============================================================================
    // FILE I/O OPERATIONS
    // ============================================================================

    /**
     * Write content to a file
     */
    protected async writeFile(filePath: string, content: string, description?: string): Promise<void> {
        await fs.promises.writeFile(filePath, content);
        if (description) {
            console.log(`\t- Generated ${description}`);
        }
    }

    /**
     * Ensure a directory exists (creates if needed)
     */
    protected async ensureDirectory(dirPath: string): Promise<void> {
        await fs.promises.mkdir(dirPath, { recursive: true });
    }

    /**
     * Check if a file exists
     */
    protected async fileExists(filePath: string): Promise<boolean> {
        try {
            await fs.promises.access(filePath);
            return true;
        } catch {
            return false;
        }
    }

    /**
     * Read file content
     */
    protected async readFile(filePath: string): Promise<string> {
        return await fs.promises.readFile(filePath, 'utf-8');
    }

    // ============================================================================
    // FRAMEWORK UTILITIES
    // ============================================================================

    /**
     * Get common Spring framework annotations
     */
    protected getFrameworkAnnotations(): any {
        return {
            service: '@Service',
            repository: '@Repository',
            component: '@Component',
            transactional: '@Transactional',
            autowired: '@Autowired',
            inject: '@Inject',
            controller: '@Controller',
            restController: '@RestController',
            requestMapping: '@RequestMapping',
            getMapping: '@GetMapping',
            postMapping: '@PostMapping',
            putMapping: '@PutMapping',
            deleteMapping: '@DeleteMapping',
            pathVariable: '@PathVariable',
            requestBody: '@RequestBody',
            requestParam: '@RequestParam'
        };
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Combine multiple import sets and remove duplicates
     */
    protected combineImports(...importSets: string[][]): string[] {
        const allImports = new Set<string>();
        for (const importSet of importSets) {
            importSet.forEach(imp => allImports.add(imp));
        }
        return Array.from(allImports).sort();
    }

    /**
     * Check if aggregate has a specific annotation
     */
    protected hasAnnotation(aggregate: Aggregate, annotationName: string): boolean {
        return aggregate.annotations?.some((a: any) =>
            a === annotationName || a.name === annotationName
        ) || false;
    }

    /**
     * Get project name from options or throw error
     */
    protected getProjectName(): string {
        if (!this.options?.projectName) {
            throw new Error('Project name is required in generator options');
        }
        return this.options.projectName;
    }
}
