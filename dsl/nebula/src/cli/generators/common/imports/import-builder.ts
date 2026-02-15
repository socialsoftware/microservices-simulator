


import { getGlobalConfig } from '../config.js';



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



export interface ImportBuilderConfig {
    projectName: string;
    basePackage?: string;
    aggregateName?: string;
    targetContext?: 'entity' | 'dto' | 'service' | 'webapi' | 'coordination' | 'saga';
}



export interface IImportBuilder {
    

    addImport(importStatement: string, category?: ImportCategory): void;

    

    addImports(imports: string[]): void;

    

    buildImports(): string[];

    

    clear(): void;
}



export abstract class BaseImportBuilder implements IImportBuilder {
    protected imports = new Map<string, ImportCategory>();
    protected config: ImportBuilderConfig;

    constructor(config: ImportBuilderConfig) {
        this.config = config;
        this.addDefaultImports();
    }

    

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
        
        const sortedImports = Array.from(this.imports.entries())
            .sort((a, b) => {
                
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

    
    
    

    protected getBasePackage(): string {
        return this.config.basePackage || getGlobalConfig().getBasePackage();
    }

    protected getProjectName(): string {
        return this.config.projectName.toLowerCase();
    }

    

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

    

    protected addFrameworkImports(isRoot: boolean = false): void {
        const basePackage = this.getBasePackage();

        if (isRoot) {
            this.addImport(`import ${basePackage}.ms.domain.aggregate.Aggregate;`, ImportCategory.FRAMEWORK);
        }
    }

    

    protected addDtoImport(dtoName: string): void {
        const basePackage = this.getBasePackage();
        const projectName = this.getProjectName();
        this.addImport(
            `import ${basePackage}.${projectName}.shared.dtos.${dtoName};`,
            ImportCategory.PROJECT_DTO
        );
    }

    

    protected addEnumImport(enumName: string): void {
        const basePackage = this.getBasePackage();
        const projectName = this.getProjectName();
        this.addImport(
            `import ${basePackage}.${projectName}.shared.enums.${enumName};`,
            ImportCategory.PROJECT_ENUM
        );
    }

    

    protected addServiceImport(serviceName: string, aggregateName?: string): void {
        const basePackage = this.getBasePackage();
        const projectName = this.getProjectName();
        const aggName = (aggregateName || this.config.aggregateName || 'unknown').toLowerCase();
        this.addImport(
            `import ${basePackage}.${projectName}.microservices.${aggName}.service.${serviceName};`,
            ImportCategory.PROJECT_SERVICE
        );
    }

    

    protected scanAndAddDtoImports(javaCode: string): void {
        const dtoPattern = /(\w+Dto)\b/g;
        const dtos = new Set<string>();
        let match;

        while ((match = dtoPattern.exec(javaCode)) !== null) {
            dtos.add(match[1]);
        }

        dtos.forEach(dto => this.addDtoImport(dto));
    }

    

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



export class EntityImportBuilder extends BaseImportBuilder {
    protected addDefaultImports(): void {
        
        
    }

    

    scanAndAddAllImports(javaCode: string, isRoot: boolean = false): void {
        this.scanAndAddJPAImports(javaCode);
        this.scanAndAddJavaImports(javaCode);
        this.scanAndAddDtoImports(javaCode);
        this.scanAndAddEnumImports(javaCode);

        if (isRoot) {
            this.addFrameworkImports(true);
        }

        
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

        
        if (javaCode.includes('AggregateState')) {
            this.addImport(
                `import ${this.getBasePackage()}.ms.domain.aggregate.Aggregate.AggregateState;`,
                ImportCategory.FRAMEWORK
            );
        }
    }
}



export class ServiceImportBuilder extends BaseImportBuilder {
    protected addDefaultImports(): void {
        this.addImport('import org.springframework.beans.factory.annotation.Autowired;', ImportCategory.FRAMEWORK);
        this.addImport('import org.springframework.stereotype.Service;', ImportCategory.FRAMEWORK);
    }
}



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
