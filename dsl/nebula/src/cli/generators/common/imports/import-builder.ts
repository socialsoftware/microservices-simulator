/**
 * Import Builder System - Unified approach to import management
 *
 * This module provides a flexible import building system that can be used across
 * all generators. It supports both pattern-based scanning and explicit import building.
 */

import { getGlobalConfig } from '../config.js';

/**
 * Common import categories
 */
export enum ImportCategory {
    JPA = 'jpa',
    JAVA_UTIL = 'java_util',
    JAVA_TIME = 'java_time',
    FRAMEWORK = 'framework',
    PROJECT_DTO = 'project_dto',
    PROJECT_ENUM = 'project_enum',
    PROJECT_SERVICE = 'project_service',
    PROJECT_EVENT = 'project_event',
    CUSTOM = 'custom'
}

/**
 * Configuration for import building
 */
export interface ImportBuilderConfig {
    projectName: string;
    basePackage?: string;
    aggregateName?: string;
    targetContext?: 'entity' | 'dto' | 'service' | 'webapi' | 'coordination' | 'saga';
}

/**
 * Base interface for all import builders
 */
export interface IImportBuilder {
    /**
     * Add an import statement
     */
    addImport(importStatement: string, category?: ImportCategory): void;

    /**
     * Add multiple imports
     */
    addImports(imports: string[]): void;

    /**
     * Build final list of imports (sorted and deduplicated)
     */
    buildImports(): string[];

    /**
     * Clear all imports
     */
    clear(): void;
}

/**
 * Base implementation of import builder with common functionality
 */
export abstract class BaseImportBuilder implements IImportBuilder {
    protected imports = new Map<string, ImportCategory>();
    protected config: ImportBuilderConfig;

    constructor(config: ImportBuilderConfig) {
        this.config = config;
        this.addDefaultImports();
    }

    /**
     * Override to add context-specific default imports
     */
    protected abstract addDefaultImports(): void;

    addImport(importStatement: string, category: ImportCategory = ImportCategory.CUSTOM): void {
        if (importStatement && !this.imports.has(importStatement)) {
            this.imports.set(importStatement, category);
        }
    }

    addImports(imports: string[]): void {
        imports.forEach(imp => this.addImport(imp));
    }

    buildImports(): string[] {
        // Sort imports by category and then alphabetically
        const sortedImports = Array.from(this.imports.entries())
            .sort((a, b) => {
                // JPA first, then Java standard library, then framework, then project imports
                const categoryOrder = {
                    [ImportCategory.JPA]: 0,
                    [ImportCategory.JAVA_UTIL]: 1,
                    [ImportCategory.JAVA_TIME]: 2,
                    [ImportCategory.FRAMEWORK]: 3,
                    [ImportCategory.PROJECT_DTO]: 4,
                    [ImportCategory.PROJECT_ENUM]: 5,
                    [ImportCategory.PROJECT_SERVICE]: 6,
                    [ImportCategory.PROJECT_EVENT]: 7,
                    [ImportCategory.CUSTOM]: 8
                };

                const catA = categoryOrder[a[1]] ?? 99;
                const catB = categoryOrder[b[1]] ?? 99;

                if (catA !== catB) return catA - catB;
                return a[0].localeCompare(b[0]);
            })
            .map(([imp]) => imp);

        return sortedImports;
    }

    clear(): void {
        this.imports.clear();
        this.addDefaultImports();
    }

    // ============================================================================
    // HELPER METHODS - Common import patterns
    // ============================================================================

    protected getBasePackage(): string {
        return this.config.basePackage || getGlobalConfig().getBasePackage();
    }

    protected getProjectName(): string {
        return this.config.projectName.toLowerCase();
    }

    /**
     * Add JPA annotation imports based on code scanning
     */
    protected scanAndAddJPAImports(javaCode: string): void {
        if (javaCode.includes('@Entity')) {
            this.addImport('import jakarta.persistence.Entity;', ImportCategory.JPA);
        }
        if (javaCode.includes('@Id')) {
            this.addImport('import jakarta.persistence.Id;', ImportCategory.JPA);
        }
        if (javaCode.includes('@GeneratedValue')) {
            this.addImport('import jakarta.persistence.GeneratedValue;', ImportCategory.JPA);
        }
        if (javaCode.includes('@OneToOne')) {
            this.addImport('import jakarta.persistence.OneToOne;', ImportCategory.JPA);
        }
        if (javaCode.includes('@OneToMany')) {
            this.addImport('import jakarta.persistence.OneToMany;', ImportCategory.JPA);
        }
        if (javaCode.includes('@ManyToOne')) {
            this.addImport('import jakarta.persistence.ManyToOne;', ImportCategory.JPA);
        }
        if (javaCode.includes('CascadeType')) {
            this.addImport('import jakarta.persistence.CascadeType;', ImportCategory.JPA);
        }
        if (javaCode.includes('FetchType')) {
            this.addImport('import jakarta.persistence.FetchType;', ImportCategory.JPA);
        }
        if (javaCode.includes('@Enumerated')) {
            this.addImport('import jakarta.persistence.Enumerated;', ImportCategory.JPA);
        }
        if (javaCode.includes('EnumType')) {
            this.addImport('import jakarta.persistence.EnumType;', ImportCategory.JPA);
        }
    }

    /**
     * Add Java standard library imports based on code scanning
     */
    protected scanAndAddJavaImports(javaCode: string): void {
        if (javaCode.includes('LocalDateTime')) {
            this.addImport('import java.time.LocalDateTime;', ImportCategory.JAVA_TIME);
        }
        if (javaCode.includes('LocalDate')) {
            this.addImport('import java.time.LocalDate;', ImportCategory.JAVA_TIME);
        }
        if (javaCode.includes('BigDecimal')) {
            this.addImport('import java.math.BigDecimal;', ImportCategory.JAVA_UTIL);
        }
        if (javaCode.includes('Set<') || javaCode.includes('HashSet')) {
            this.addImport('import java.util.Set;', ImportCategory.JAVA_UTIL);
            this.addImport('import java.util.HashSet;', ImportCategory.JAVA_UTIL);
        }
        if (javaCode.includes('List<') || javaCode.includes('ArrayList')) {
            this.addImport('import java.util.List;', ImportCategory.JAVA_UTIL);
            this.addImport('import java.util.ArrayList;', ImportCategory.JAVA_UTIL);
        }
        if (javaCode.includes('Collectors')) {
            this.addImport('import java.util.stream.Collectors;', ImportCategory.JAVA_UTIL);
        }
        if (javaCode.includes('Arrays')) {
            this.addImport('import java.util.Arrays;', ImportCategory.JAVA_UTIL);
        }
    }

    /**
     * Add framework-specific imports
     */
    protected addFrameworkImports(isRoot: boolean = false): void {
        const basePackage = this.getBasePackage();

        if (isRoot) {
            this.addImport(`import ${basePackage}.ms.domain.aggregate.Aggregate;`, ImportCategory.FRAMEWORK);
        }
    }

    /**
     * Add project DTO import
     */
    protected addDtoImport(dtoName: string): void {
        const basePackage = this.getBasePackage();
        const projectName = this.getProjectName();
        this.addImport(
            `import ${basePackage}.${projectName}.shared.dtos.${dtoName};`,
            ImportCategory.PROJECT_DTO
        );
    }

    /**
     * Add project enum import
     */
    protected addEnumImport(enumName: string): void {
        const basePackage = this.getBasePackage();
        const projectName = this.getProjectName();
        this.addImport(
            `import ${basePackage}.${projectName}.shared.enums.${enumName};`,
            ImportCategory.PROJECT_ENUM
        );
    }

    /**
     * Add project service import
     */
    protected addServiceImport(serviceName: string, aggregateName?: string): void {
        const basePackage = this.getBasePackage();
        const projectName = this.getProjectName();
        const aggName = (aggregateName || this.config.aggregateName || 'unknown').toLowerCase();
        this.addImport(
            `import ${basePackage}.${projectName}.microservices.${aggName}.service.${serviceName};`,
            ImportCategory.PROJECT_SERVICE
        );
    }

    /**
     * Scan code for DTO references and add imports
     */
    protected scanAndAddDtoImports(javaCode: string): void {
        const dtoPattern = /(\w+Dto)\b/g;
        const dtos = new Set<string>();
        let match;

        while ((match = dtoPattern.exec(javaCode)) !== null) {
            dtos.add(match[1]);
        }

        dtos.forEach(dto => this.addDtoImport(dto));
    }

    /**
     * Scan code for enum references and add imports (excludes JPA enums)
     */
    protected scanAndAddEnumImports(javaCode: string): void {
        const enumPattern = /\b([A-Z][a-zA-Z]*(?:Type|Role|State))\b/g;
        const excludedEnums = ['EnumType', 'CascadeType', 'FetchType', 'AggregateState', 'LocalDateTime'];
        const enums = new Set<string>();
        let match;

        while ((match = enumPattern.exec(javaCode)) !== null) {
            const enumType = match[1];
            if (!excludedEnums.includes(enumType) &&
                !enumType.endsWith('Dto') &&
                !enumType.includes('List') &&
                !enumType.includes('Set')) {
                enums.add(enumType);
            }
        }

        enums.forEach(enumType => this.addEnumImport(enumType));
    }
}

/**
 * Import builder for entity generation
 */
export class EntityImportBuilder extends BaseImportBuilder {
    protected addDefaultImports(): void {
        // Entities don't have universal default imports
        // They scan code and add as needed
    }

    /**
     * Scan generated entity code and add all required imports
     */
    scanAndAddAllImports(javaCode: string, isRoot: boolean = false): void {
        this.scanAndAddJPAImports(javaCode);
        this.scanAndAddJavaImports(javaCode);
        this.scanAndAddDtoImports(javaCode);
        this.scanAndAddEnumImports(javaCode);

        if (isRoot) {
            this.addFrameworkImports(true);
        }

        // Exception imports for invariants
        if (javaCode.includes('INVARIANT_BREAK')) {
            this.addImport(
                `import static ${this.getBasePackage()}.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;`,
                ImportCategory.FRAMEWORK
            );
        }
        if (javaCode.includes('SimulatorException')) {
            this.addImport(
                `import ${this.getBasePackage()}.ms.exception.SimulatorException;`,
                ImportCategory.FRAMEWORK
            );
        }

        // AggregateState
        if (javaCode.includes('AggregateState')) {
            this.addImport(
                `import ${this.getBasePackage()}.ms.domain.aggregate.Aggregate.AggregateState;`,
                ImportCategory.FRAMEWORK
            );
        }
    }
}

/**
 * Import builder for service generation
 */
export class ServiceImportBuilder extends BaseImportBuilder {
    protected addDefaultImports(): void {
        this.addImport('import org.springframework.beans.factory.annotation.Autowired;', ImportCategory.FRAMEWORK);
        this.addImport('import org.springframework.stereotype.Service;', ImportCategory.FRAMEWORK);
    }
}

/**
 * Import builder for coordination/functionalities generation
 */
export class FunctionalitiesImportBuilder extends BaseImportBuilder {
    protected addDefaultImports(): void {
        const basePackage = this.getBasePackage();

        this.addImport('import java.util.Arrays;', ImportCategory.JAVA_UTIL);
        this.addImport('import org.springframework.beans.factory.annotation.Autowired;', ImportCategory.FRAMEWORK);
        this.addImport('import org.springframework.core.env.Environment;', ImportCategory.FRAMEWORK);
        this.addImport('import org.springframework.stereotype.Service;', ImportCategory.FRAMEWORK);
        this.addImport('import jakarta.annotation.PostConstruct;', ImportCategory.FRAMEWORK);
        this.addImport(`import ${basePackage}.ms.TransactionalModel;`, ImportCategory.FRAMEWORK);
        this.addImport(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWork;`, ImportCategory.FRAMEWORK);
        this.addImport(`import ${basePackage}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`, ImportCategory.FRAMEWORK);
    }
}
