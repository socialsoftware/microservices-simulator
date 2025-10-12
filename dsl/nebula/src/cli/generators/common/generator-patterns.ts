import { Aggregate } from "../../../language/generated/ast.js";
import { OrchestrationBase } from "./orchestration-base.js";


export interface BaseGenerationOptions {
    projectName: string;
    architecture?: string;
    features?: string[];
    outputDirectory?: string;
}


export interface BaseContext {
    aggregateName: string;
    capitalizedAggregate: string;
    lowerAggregate: string;
    packageName: string;
    projectName: string;
    imports: string[];
}

export interface EntityContext extends BaseContext {
    rootEntity: any;
    entities: any[];
    properties: any[];
}


export interface Generator<TOptions, TResult> {
    generate(aggregate: Aggregate, options: TOptions): Promise<TResult>;
}

export interface FileGenerator extends Generator<BaseGenerationOptions, string> {
    generateFile(aggregate: Aggregate, options: BaseGenerationOptions): Promise<string>;
}

export interface MultiFileGenerator extends Generator<BaseGenerationOptions, { [key: string]: string }> {
    generateFiles(aggregate: Aggregate, options: BaseGenerationOptions): Promise<{ [key: string]: string }>;
}

export abstract class SingleFileGeneratorPattern extends OrchestrationBase implements FileGenerator {
    abstract generateFile(aggregate: Aggregate, options: BaseGenerationOptions): Promise<string>;

    async generate(aggregate: Aggregate, options: BaseGenerationOptions): Promise<string> {
        this.validateAggregate(aggregate);
        this.validateOptions(options, ['projectName']);
        return this.generateFile(aggregate, options);
    }

    protected createBaseContext(aggregate: Aggregate, options: BaseGenerationOptions, subPackage: string): BaseContext {
        const naming = this.createAggregateNaming(aggregate.name);
        const packageName = this.generatePackageName(options.projectName, aggregate.name, subPackage);

        return {
            aggregateName: naming.original,
            capitalizedAggregate: naming.capitalized,
            lowerAggregate: naming.lower,
            packageName,
            projectName: options.projectName,
            imports: this.buildStandardImports(options.projectName, aggregate.name)
        };
    }
}

export abstract class MultiFileGeneratorPattern extends OrchestrationBase implements MultiFileGenerator {
    abstract generateFiles(aggregate: Aggregate, options: BaseGenerationOptions): Promise<{ [key: string]: string }>;

    async generate(aggregate: Aggregate, options: BaseGenerationOptions): Promise<{ [key: string]: string }> {
        this.validateAggregate(aggregate);
        this.validateOptions(options, ['projectName']);
        return this.generateFiles(aggregate, options);
    }

    protected createEntityContext(aggregate: Aggregate, options: BaseGenerationOptions, subPackage: string): EntityContext {
        const baseContext = this.createBaseContext(aggregate, options, subPackage);
        const rootEntity = this.findRootEntity(aggregate);
        const properties = this.buildPropertyInfo(rootEntity);

        return {
            ...baseContext,
            rootEntity,
            entities: aggregate.entities,
            properties
        };
    }

    protected createBaseContext(aggregate: Aggregate, options: BaseGenerationOptions, subPackage: string): BaseContext {
        const naming = this.createAggregateNaming(aggregate.name);
        const packageName = this.generatePackageName(options.projectName, aggregate.name, subPackage);

        return {
            aggregateName: naming.original,
            capitalizedAggregate: naming.capitalized,
            lowerAggregate: naming.lower,
            packageName,
            projectName: options.projectName,
            imports: this.buildStandardImports(options.projectName, aggregate.name)
        };
    }
}

export abstract class ConfigurationGeneratorPattern extends OrchestrationBase {
    protected buildPropertyLine(key: string, value: string | number | boolean): string {
        return `${key}=${value}`;
    }

    protected buildYamlProperty(key: string, value: string | number | boolean, indent: number = 0): string {
        const indentation = '  '.repeat(indent);
        return `${indentation}${key}: ${value}`;
    }

    protected buildYamlSection(title: string, properties: string[], indent: number = 0): string {
        const indentation = '  '.repeat(indent);
        return `${indentation}${title}:\n${properties.join('\n')}`;
    }

    protected getPort(projectName: string): number {
        let hash = 0;
        for (let i = 0; i < projectName.length; i++) {
            const char = projectName.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash;
        }
        return Math.abs(hash % 1000) + 8080;
    }

    protected override getDatabaseName(projectName: string): string {
        return `${projectName.toLowerCase()}_db`;
    }

    protected getServiceName(projectName: string): string {
        return `${projectName.toLowerCase()}-service`;
    }
}

export class TemplatePatterns {
    static readonly JAVA_CLASS_HEADER = `package {{packageName}};

{{#each imports}}
{{this}}
{{/each}}`;

    static readonly SPRING_SERVICE_CLASS = `${TemplatePatterns.JAVA_CLASS_HEADER}

@Service
@Transactional
public class {{className}} {`;

    static readonly SPRING_CONTROLLER_CLASS = `${TemplatePatterns.JAVA_CLASS_HEADER}

@RestController
@RequestMapping("/api/{{lowerAggregate}}")
public class {{className}} {`;

    static readonly SPRING_REPOSITORY_INTERFACE = `${TemplatePatterns.JAVA_CLASS_HEADER}

@Repository
public interface {{className}} extends JpaRepository<{{entityName}}, Long> {`;

    static readonly JPA_ENTITY_CLASS = `${TemplatePatterns.JAVA_CLASS_HEADER}

@Entity
@Table(name = "{{tableName}}")
public class {{className}} {`;
}


export class GenerationError extends Error {
    constructor(
        message: string,
        public readonly generatorName: string,
        public readonly aggregateName?: string,
        public override readonly cause?: Error
    ) {
        super(message);
        this.name = 'GenerationError';
    }
}

export function handleGenerationError(error: Error, generatorName: string, aggregateName?: string): never {
    if (error instanceof GenerationError) {
        throw error;
    }
    throw new GenerationError(
        `Error in ${generatorName}: ${error.message}`,
        generatorName,
        aggregateName,
        error
    );
}
