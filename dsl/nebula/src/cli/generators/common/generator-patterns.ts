import { Aggregate, Entity } from "../../../language/generated/ast.js";
import { UnifiedTypeResolver } from "./unified-type-resolver.js";
import { getGlobalConfig } from "./config.js";


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


abstract class GeneratorPatternBase {
    protected capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    protected createAggregateNaming(aggregateName: string) {
        return {
            original: aggregateName,
            capitalized: this.capitalize(aggregateName),
            lower: aggregateName.toLowerCase()
        };
    }

    protected generatePackageName(projectName: string, aggregateName: string, ...subPackages: string[]): string {
        const basePackage = getGlobalConfig().getBasePackage();
        const microservicePackage = `microservices.${aggregateName.toLowerCase()}`;
        const subPackageString = subPackages.filter(p => p).join('.');
        return `${basePackage}.${projectName.toLowerCase()}.${microservicePackage}.${subPackageString}`;
    }

    protected buildStandardImports(projectName: string, aggregateName: string): string[] {
        return [];
    }

    protected validateAggregate(aggregate: Aggregate): void {
        if (!aggregate) {
            throw new Error('Aggregate is required');
        }
        if (!aggregate.name) {
            throw new Error('Aggregate name is required');
        }
    }

    protected validateOptions(options: any, requiredFields: string[]): void {
        if (!options) {
            throw new Error('Options are required');
        }
        for (const field of requiredFields) {
            if (!options[field]) {
                throw new Error(`Required option '${field}' is missing`);
            }
        }
    }

    protected findRootEntity(aggregate: Aggregate): Entity {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }
        return rootEntity;
    }

    protected buildPropertyInfo(entity: Entity): any[] {
        if (!entity.properties) return [];

        return entity.properties.map(prop => ({
            name: prop.name,
            capitalizedName: this.capitalize(prop.name),
            type: UnifiedTypeResolver.resolve(prop.type),
            required: !(prop as any).isOptional,
            isCollection: UnifiedTypeResolver.isCollectionType(prop.type),
            isEntity: UnifiedTypeResolver.isEntityType(prop.type)
        }));
    }

    protected getDatabaseName(projectName: string): string {
        return projectName ? projectName.toLowerCase() : 'defaultdb';
    }

    protected getDatabaseConfig(): any {
        return {
            type: 'postgresql',
            port: 5432,
            host: 'localhost',
            name: 'defaultdb',
            username: 'postgres',
            password: 'postgres'
        };
    }

    protected getDatabaseType(): string {
        return this.getDatabaseConfig().type;
    }

    protected getDatabaseDriverClass(): string {
        const dbType = this.getDatabaseType();
        switch (dbType) {
            case 'postgresql':
                return 'org.postgresql.Driver';
            case 'mysql':
                return 'com.mysql.cj.jdbc.Driver';
            case 'h2':
                return 'org.h2.Driver';
            case 'mongodb':
                return 'mongodb.jdbc.MongoDriver';
            default:
                return 'org.postgresql.Driver';
        }
    }

    protected getDatabaseDialect(): string {
        const dbType = this.getDatabaseType();
        switch (dbType) {
            case 'postgresql':
                return 'org.hibernate.dialect.PostgreSQLDialect';
            case 'mysql':
                return 'org.hibernate.dialect.MySQLDialect';
            case 'h2':
                return 'org.hibernate.dialect.H2Dialect';
            case 'mongodb':
                return 'org.hibernate.dialect.MongoDialect';
            default:
                return 'org.hibernate.dialect.PostgreSQLDialect';
        }
    }

    protected getDatabaseUsername(): string {
        return this.getDatabaseConfig().username;
    }

    protected getDatabasePassword(): string {
        return this.getDatabaseConfig().password;
    }

    protected getJdbcUrl(projectName?: string): string {
        const config = this.getDatabaseConfig();
        const dbName = this.getDatabaseName(projectName || 'defaultdb');

        switch (config.type) {
            case 'postgresql':
                return `jdbc:postgresql://${config.host}:${config.port}/${dbName}`;
            case 'mysql':
                return `jdbc:mysql://${config.host}:${config.port}/${dbName}`;
            case 'h2':
                return `jdbc:h2:mem:${dbName}`;
            case 'mongodb':
                return `mongodb://${config.host}:${config.port}/${dbName}`;
            default:
                return `jdbc:postgresql://${config.host}:${config.port}/${dbName}`;
        }
    }

    protected writeFile(filePath: string, content: string, description?: string): void {
        
        
        throw new Error('writeFile must be implemented by subclass or handled by FileWriter service');
    }

    protected ensureDirectory(dirPath: string): void {
        
        
        throw new Error('ensureDirectory must be implemented by subclass or handled by FileWriter service');
    }
}

export abstract class SingleFileGeneratorPattern extends GeneratorPatternBase implements FileGenerator {
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

export abstract class MultiFileGeneratorPattern extends GeneratorPatternBase implements MultiFileGenerator {
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

export abstract class ConfigurationGeneratorPattern extends GeneratorPatternBase {
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
        const config = getGlobalConfig().getConfig();
        const minPort = config.portRange.min;
        const maxPort = config.portRange.max;
        const range = maxPort - minPort;

        let hash = 0;
        for (let i = 0; i < projectName.length; i++) {
            const char = projectName.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash;
        }
        return minPort + (Math.abs(hash) % range);
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
