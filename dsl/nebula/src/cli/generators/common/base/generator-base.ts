


import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { TemplateManager } from "../../../utils/template-manager.js";
import { UnifiedTypeResolver } from "../unified-type-resolver.js";
import { getGlobalConfig } from "../config.js";
import * as fs from 'fs';



export interface GeneratorOptions {
    projectName: string;
    basePackage?: string;
    architecture?: string;
    outputDir?: string;
}



export abstract class GeneratorBase {
    protected templateManager: TemplateManager;
    protected typeResolver: typeof UnifiedTypeResolver;

    constructor(protected options?: GeneratorOptions) {
        this.templateManager = TemplateManager.getInstance();
        this.typeResolver = UnifiedTypeResolver;
    }

    
    
    

    

    protected renderTemplate(templatePath: string, context: any): string {
        return this.templateManager.renderTemplate(templatePath, context);
    }

    

    protected getTemplateManager(): TemplateManager {
        return this.templateManager;
    }

    
    
    

    

    protected capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    

    protected lowercase(str: string): string {
        return str ? str.toLowerCase() : '';
    }

    

    protected uppercase(str: string): string {
        return str ? str.toUpperCase() : '';
    }

    

    protected toCamelCase(str: string): string {
        if (!str) return '';
        return str.charAt(0).toLowerCase() + str.slice(1);
    }

    

    protected toKebabCase(str: string): string {
        return str.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase();
    }

    

    protected toSnakeCase(str: string): string {
        return str.replace(/([a-z])([A-Z])/g, '$1_$2').toLowerCase();
    }

    
    
    

    

    protected findRootEntity(aggregate: Aggregate): Entity {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }
        return rootEntity;
    }

    

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

    
    
    

    

    protected getBasePackage(): string {
        if (this.options?.basePackage) {
            return this.options.basePackage;
        }
        const config = getGlobalConfig();
        return config.getBasePackage();
    }

    

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

    

    protected buildPackageName(...segments: string[]): string {
        return segments.filter(s => s).join('.');
    }

    
    
    

    

    protected resolveJavaType(type: any): string {
        return this.typeResolver.resolveJavaType(type);
    }

    

    protected isCollectionType(type: any): boolean {
        return this.typeResolver.isCollectionType(type);
    }

    

    protected isEntityType(type: any): boolean {
        return this.typeResolver.isEntityType(type);
    }

    

    protected getElementType(type: any): string | undefined {
        return this.typeResolver.getElementType(type);
    }

    
    
    

    

    protected async writeFile(filePath: string, content: string, description?: string): Promise<void> {
        await fs.promises.writeFile(filePath, content);
        if (description) {
            console.log(`\t- Generated ${description}`);
        }
    }

    

    protected async ensureDirectory(dirPath: string): Promise<void> {
        await fs.promises.mkdir(dirPath, { recursive: true });
    }

    

    protected async fileExists(filePath: string): Promise<boolean> {
        try {
            await fs.promises.access(filePath);
            return true;
        } catch {
            return false;
        }
    }

    

    protected async readFile(filePath: string): Promise<string> {
        return await fs.promises.readFile(filePath, 'utf-8');
    }

    
    
    

    

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

    
    
    

    

    protected combineImports(...importSets: string[][]): string[] {
        const allImports = new Set<string>();
        for (const importSet of importSets) {
            importSet.forEach(imp => allImports.add(imp));
        }
        return Array.from(allImports).sort();
    }

    

    protected hasAnnotation(aggregate: Aggregate, annotationName: string): boolean {
        return aggregate.annotations?.some((a: any) =>
            a === annotationName || a.name === annotationName
        ) || false;
    }

    

    protected getProjectName(): string {
        if (!this.options?.projectName) {
            throw new Error('Project name is required in generator options');
        }
        return this.options.projectName;
    }
}
