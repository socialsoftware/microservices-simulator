


import { getGlobalConfig } from "../generators/common/config.js";



export enum ImportCategory {
    JAVA_STANDARD = 'java',
    JAVA_UTIL = 'java.util',
    JAVA_TIME = 'java.time',
    JAKARTA = 'jakarta',
    SPRING = 'org.springframework',
    THIRD_PARTY = 'third-party',
    PROJECT_BASE = 'project-base',
    PROJECT_MICROSERVICES = 'project-microservices',
    PROJECT_COORDINATION = 'project-coordination',
    PROJECT_SAGAS = 'project-sagas',
    PROJECT_SHARED = 'project-shared',
    CUSTOM = 'custom'
}



interface ImportEntry {
    fullImport: string;
    className: string;
    packagePath: string;
    category: ImportCategory;
    isStatic: boolean;
    isWildcard: boolean;
}



interface ImportDetectionConfig {
    projectName: string;
    basePackage?: string;
    enableAutoDetection: boolean;
    customMappings?: Map<string, string>;
}



export class ImportManager {
    private imports = new Map<string, ImportEntry>();
    private config: ImportDetectionConfig;
    private basePackage: string;

    constructor(config: ImportDetectionConfig) {
        this.config = config;
        this.basePackage = config.basePackage || getGlobalConfig().getBasePackage();
    }

    

    addJavaImport(className: string): this {
        const fullImport = `import java.${className};`;
        this.addImportEntry(fullImport, className, `java.${className}`, ImportCategory.JAVA_STANDARD);
        return this;
    }

    

    addJavaUtilImport(className: string): this {
        const fullImport = `import java.util.${className};`;
        this.addImportEntry(fullImport, className, `java.util.${className}`, ImportCategory.JAVA_UTIL);
        return this;
    }

    

    addJavaTimeImport(className: string): this {
        const fullImport = `import java.time.${className};`;
        this.addImportEntry(fullImport, className, `java.time.${className}`, ImportCategory.JAVA_TIME);
        return this;
    }

    

    addJakartaImport(path: string): this {
        const fullImport = `import jakarta.${path};`;
        const className = this.extractClassName(path);
        this.addImportEntry(fullImport, className, `jakarta.${path}`, ImportCategory.JAKARTA);
        return this;
    }

    

    addSpringImport(path: string): this {
        const fullImport = `import org.springframework.${path};`;
        const className = this.extractClassName(path);
        this.addImportEntry(fullImport, className, `org.springframework.${path}`, ImportCategory.SPRING);
        return this;
    }

    

    addMicroserviceImport(aggregate: string, component: string, className: string): this {
        const packagePath = `${this.basePackage}.${this.config.projectName.toLowerCase()}.microservices.${aggregate.toLowerCase()}.${component}`;
        const fullImport = `import ${packagePath}.${className};`;
        this.addImportEntry(fullImport, className, packagePath, ImportCategory.PROJECT_MICROSERVICES);
        return this;
    }

    

    addCoordinationImport(component: string, className: string): this {
        const packagePath = `${this.basePackage}.${this.config.projectName.toLowerCase()}.coordination.${component}`;
        const fullImport = `import ${packagePath}.${className};`;
        this.addImportEntry(fullImport, className, packagePath, ImportCategory.PROJECT_COORDINATION);
        return this;
    }

    

    addSagaImport(component: string, className: string): this {
        const packagePath = `${this.basePackage}.${this.config.projectName.toLowerCase()}.sagas.${component}`;
        const fullImport = `import ${packagePath}.${className};`;
        this.addImportEntry(fullImport, className, packagePath, ImportCategory.PROJECT_SAGAS);
        return this;
    }

    

    addSharedImport(component: string, className: string): this {
        const packagePath = `${this.basePackage}.${this.config.projectName.toLowerCase()}.shared.${component}`;
        const fullImport = `import ${packagePath}.${className};`;
        this.addImportEntry(fullImport, className, packagePath, ImportCategory.PROJECT_SHARED);
        return this;
    }

    

    addBaseFrameworkImport(path: string): this {
        const fullImport = `import ${this.basePackage}.${path};`;
        const className = this.extractClassName(path);
        this.addImportEntry(fullImport, className, `${this.basePackage}.${path}`, ImportCategory.PROJECT_BASE);
        return this;
    }

    

    addStaticImport(packagePath: string, member: string): this {
        const fullImport = `import static ${packagePath}.${member};`;
        this.addImportEntry(fullImport, member, packagePath, this.categorizePackage(packagePath), true);
        return this;
    }

    

    addWildcardImport(packagePath: string): this {
        const fullImport = `import ${packagePath}.*;`;
        this.addImportEntry(fullImport, '*', packagePath, this.categorizePackage(packagePath), false, true);
        return this;
    }

    

    addCustomImport(fullPath: string): this {
        const fullImport = fullPath.startsWith('import ') ? fullPath : `import ${fullPath};`;
        const className = this.extractClassName(fullPath);
        const packagePath = this.extractPackagePath(fullPath);
        this.addImportEntry(fullImport, className, packagePath, this.categorizePackage(packagePath));
        return this;
    }

    

    addImports(imports: string[]): this {
        imports.forEach(imp => this.addCustomImport(imp));
        return this;
    }

    

    resolveAndAddImports(code: string): this {
        if (!this.config.enableAutoDetection) {
            return this;
        }

        
        const patterns = [
            
            /@(Controller|Service|Repository|Component|Autowired|Transactional|RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PathVariable|RequestBody|Valid)/g,
            
            /@(Entity|Table|Id|GeneratedValue|Column|ManyToOne|OneToMany|JoinColumn|Valid|NotNull|NotBlank|Size)/g,
            
            /\b(List|Set|Map|Optional|LocalDateTime|BigDecimal|UUID)\b/g,
            
            /\b(\w+Exception)\b/g,
        ];

        
        const springAnnotations: Record<string, string> = {
            'Controller': 'stereotype.Controller',
            'Service': 'stereotype.Service',
            'Repository': 'stereotype.Repository',
            'Component': 'stereotype.Component',
            'Autowired': 'beans.factory.annotation.Autowired',
            'Transactional': 'transaction.annotation.Transactional',
            'RequestMapping': 'web.bind.annotation.RequestMapping',
            'GetMapping': 'web.bind.annotation.GetMapping',
            'PostMapping': 'web.bind.annotation.PostMapping',
            'PutMapping': 'web.bind.annotation.PutMapping',
            'DeleteMapping': 'web.bind.annotation.DeleteMapping',
            'PathVariable': 'web.bind.annotation.PathVariable',
            'RequestBody': 'web.bind.annotation.RequestBody',
            'Valid': 'validation.annotation.Validated'
        };

        
        const jakartaAnnotations: Record<string, string> = {
            'Entity': 'persistence.Entity',
            'Table': 'persistence.Table',
            'Id': 'persistence.Id',
            'GeneratedValue': 'persistence.GeneratedValue',
            'Column': 'persistence.Column',
            'ManyToOne': 'persistence.ManyToOne',
            'OneToMany': 'persistence.OneToMany',
            'JoinColumn': 'persistence.JoinColumn',
            'Valid': 'validation.Valid',
            'NotNull': 'validation.constraints.NotNull',
            'NotBlank': 'validation.constraints.NotBlank',
            'Size': 'validation.constraints.Size'
        };

        
        const javaTypes: Record<string, string> = {
            'List': 'util.List',
            'Set': 'util.Set',
            'Map': 'util.Map',
            'Optional': 'util.Optional',
            'LocalDateTime': 'time.LocalDateTime',
            'BigDecimal': 'math.BigDecimal',
            'UUID': 'util.UUID'
        };

        
        let match;
        patterns[0].lastIndex = 0;
        while ((match = patterns[0].exec(code)) !== null) {
            const annotation = match[1];
            if (springAnnotations[annotation]) {
                this.addSpringImport(springAnnotations[annotation]);
            }
        }

        
        patterns[1].lastIndex = 0;
        while ((match = patterns[1].exec(code)) !== null) {
            const annotation = match[1];
            if (jakartaAnnotations[annotation]) {
                this.addJakartaImport(jakartaAnnotations[annotation]);
            }
        }

        
        patterns[2].lastIndex = 0;
        while ((match = patterns[2].exec(code)) !== null) {
            const type = match[1];
            if (javaTypes[type]) {
                if (type === 'LocalDateTime' || type === 'BigDecimal') {
                    this.addJavaTimeImport(type);
                } else {
                    this.addJavaUtilImport(type);
                }
            }
        }

        return this;
    }

    

    formatImports(): string[] {
        const grouped = this.groupImportsByCategory();
        const result: string[] = [];

        
        const categoryOrder = [
            ImportCategory.JAVA_STANDARD,
            ImportCategory.JAVA_UTIL,
            ImportCategory.JAVA_TIME,
            ImportCategory.JAKARTA,
            ImportCategory.SPRING,
            ImportCategory.THIRD_PARTY,
            ImportCategory.PROJECT_BASE,
            ImportCategory.PROJECT_MICROSERVICES,
            ImportCategory.PROJECT_COORDINATION,
            ImportCategory.PROJECT_SAGAS,
            ImportCategory.PROJECT_SHARED,
            ImportCategory.CUSTOM
        ];

        categoryOrder.forEach((category, index) => {
            const imports = grouped.get(category);
            if (imports && imports.length > 0) {
                
                const sortedImports = imports
                    .sort((a, b) => a.fullImport.localeCompare(b.fullImport))
                    .map(imp => imp.fullImport);

                result.push(...sortedImports);

                
                if (index < categoryOrder.length - 1 && this.hasMoreCategories(grouped, categoryOrder, index)) {
                    result.push('');
                }
            }
        });

        return result;
    }

    

    getStats(): { total: number; byCategory: Map<ImportCategory, number> } {
        const byCategory = new Map<ImportCategory, number>();

        this.imports.forEach(entry => {
            const count = byCategory.get(entry.category) || 0;
            byCategory.set(entry.category, count + 1);
        });

        return {
            total: this.imports.size,
            byCategory
        };
    }

    

    hasImport(className: string): boolean {
        return Array.from(this.imports.values()).some(entry => entry.className === className);
    }

    

    removeImport(className: string): this {
        const toRemove = Array.from(this.imports.entries())
            .find(([_, entry]) => entry.className === className);

        if (toRemove) {
            this.imports.delete(toRemove[0]);
        }

        return this;
    }

    

    clear(): this {
        this.imports.clear();
        return this;
    }

    

    clone(): ImportManager {
        const clone = new ImportManager(this.config);
        this.imports.forEach((entry, key) => {
            clone.imports.set(key, { ...entry });
        });
        return clone;
    }

    

    private addImportEntry(
        fullImport: string,
        className: string,
        packagePath: string,
        category: ImportCategory,
        isStatic: boolean = false,
        isWildcard: boolean = false
    ): void {
        const key = fullImport.trim();
        if (!this.imports.has(key)) {
            this.imports.set(key, {
                fullImport: key,
                className,
                packagePath,
                category,
                isStatic,
                isWildcard
            });
        }
    }

    private extractClassName(path: string): string {
        
        const cleanPath = path.replace(/^import\s+/, '').replace(/;$/, '');

        
        if (cleanPath.includes('static ')) {
            const parts = cleanPath.replace('static ', '').split('.');
            return parts[parts.length - 1];
        }

        
        if (cleanPath.endsWith('.*')) {
            return '*';
        }

        
        const parts = cleanPath.split('.');
        return parts[parts.length - 1];
    }

    private extractPackagePath(fullPath: string): string {
        const cleanPath = fullPath.replace(/^import\s+/, '').replace(/;$/, '');
        const parts = cleanPath.split('.');
        return parts.slice(0, -1).join('.');
    }

    private categorizePackage(packagePath: string): ImportCategory {
        if (packagePath.startsWith('java.util')) return ImportCategory.JAVA_UTIL;
        if (packagePath.startsWith('java.time')) return ImportCategory.JAVA_TIME;
        if (packagePath.startsWith('java.')) return ImportCategory.JAVA_STANDARD;
        if (packagePath.startsWith('jakarta.')) return ImportCategory.JAKARTA;
        if (packagePath.startsWith('org.springframework')) return ImportCategory.SPRING;
        if (packagePath.startsWith(this.basePackage)) {
            if (packagePath.includes('.microservices.')) return ImportCategory.PROJECT_MICROSERVICES;
            if (packagePath.includes('.coordination.')) return ImportCategory.PROJECT_COORDINATION;
            if (packagePath.includes('.sagas.')) return ImportCategory.PROJECT_SAGAS;
            if (packagePath.includes('.shared.')) return ImportCategory.PROJECT_SHARED;
            return ImportCategory.PROJECT_BASE;
        }
        return ImportCategory.CUSTOM;
    }

    private groupImportsByCategory(): Map<ImportCategory, ImportEntry[]> {
        const grouped = new Map<ImportCategory, ImportEntry[]>();

        this.imports.forEach(entry => {
            const existing = grouped.get(entry.category) || [];
            existing.push(entry);
            grouped.set(entry.category, existing);
        });

        return grouped;
    }

    private hasMoreCategories(grouped: Map<ImportCategory, ImportEntry[]>, categoryOrder: ImportCategory[], currentIndex: number): boolean {
        for (let i = currentIndex + 1; i < categoryOrder.length; i++) {
            if (grouped.has(categoryOrder[i]) && grouped.get(categoryOrder[i])!.length > 0) {
                return true;
            }
        }
        return false;
    }
}



export class ImportManagerFactory {
    

    static createForMicroservice(projectName: string, enableAutoDetection: boolean = true): ImportManager {
        return new ImportManager({
            projectName,
            enableAutoDetection,
            customMappings: new Map()
        });
    }

    

    static createForCoordination(projectName: string, enableAutoDetection: boolean = true): ImportManager {
        return new ImportManager({
            projectName,
            enableAutoDetection,
            customMappings: new Map()
        });
    }

    

    static createForSagas(projectName: string, enableAutoDetection: boolean = true): ImportManager {
        return new ImportManager({
            projectName,
            enableAutoDetection,
            customMappings: new Map()
        });
    }

    

    static createCustom(config: ImportDetectionConfig): ImportManager {
        return new ImportManager(config);
    }
}
