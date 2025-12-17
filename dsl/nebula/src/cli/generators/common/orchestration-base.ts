import * as fs from 'fs';
import { Aggregate, Entity } from "../../../language/generated/ast.js";
import { UnifiedTypeResolver } from "./unified-type-resolver.js";
import Handlebars from "handlebars";
import { getGlobalConfig } from "./config.js";
import { TemplateManager } from "../../utils/template-manager.js";

export abstract class OrchestrationBase {
    constructor() {
        this.registerHandlebarsHelpers();
    }

    private registerHandlebarsHelpers(): void {
        Handlebars.registerHelper('eq', function (a: any, b: any): boolean {
            return a === b;
        });

        Handlebars.registerHelper('ne', function (a: any, b: any): boolean {
            return a !== b;
        });

        Handlebars.registerHelper('lt', function (a: any, b: any): boolean {
            return a < b;
        });

        Handlebars.registerHelper('gt', function (a: any, b: any): boolean {
            return a > b;
        });

        Handlebars.registerHelper('lte', function (a: any, b: any): boolean {
            return a <= b;
        });

        Handlebars.registerHelper('gte', function (a: any, b: any): boolean {
            return a >= b;
        });

        Handlebars.registerHelper('and', function (a: any, b: any): boolean {
            return !!(a && b);
        });

        Handlebars.registerHelper('or', function (a: any, b: any): boolean {
            return !!(a || b);
        });

        Handlebars.registerHelper('not', function (a: any): boolean {
            return !a;
        });

        Handlebars.registerHelper('capitalize', function (str: string): string {
            if (!str) return '';
            return str.charAt(0).toUpperCase() + str.slice(1);
        });

        Handlebars.registerHelper('lowercase', function (str: string): string {
            return str ? str.toLowerCase() : '';
        });

        Handlebars.registerHelper('uppercase', function (str: string): string {
            return str ? str.toUpperCase() : '';
        });

        Handlebars.registerHelper('json', function (context: any): string {
            return JSON.stringify(context);
        });

        Handlebars.registerHelper('length', function (array: any[]): number {
            return Array.isArray(array) ? array.length : 0;
        });
    }

    protected renderTemplate(template: string, context: any): string {
        const compiledTemplate = Handlebars.compile(template, { noEscape: true });
        return compiledTemplate(context);
    }

    /**
     * Render template using the cached template manager (preferred method)
     */
    protected renderCachedTemplate(templatePath: string, context: any): string {
        const templateManager = TemplateManager.getInstance();
        return templateManager.renderTemplate(templatePath, context);
    }

    protected renderSimpleTemplate(template: string, context: any): string {
        let result = template;

        result = result.replace(/\{\{(\w+)\}\}/g, (match, key) => {
            return context[key] || match;
        });

        result = result.replace(/\{\{#each (\w+)\}\}([\s\S]*?)\{\{\/each\}\}/g, (match, arrayKey, content) => {
            const array = context[arrayKey];
            if (!Array.isArray(array)) return '';

            return array.map(item => {
                let itemContent = content;
                Object.keys(item).forEach(key => {
                    const regex = new RegExp(`\\{\\{${key}\\}\\}`, 'g');
                    itemContent = itemContent.replace(regex, item[key]);
                });
                return itemContent;
            }).join('');
        });

        return result;
    }

    protected async writeFile(filePath: string, content: string, description: string): Promise<void> {
        await fs.promises.writeFile(filePath, content);
        console.log(`        - Generated ${description}`);
    }

    protected async ensureDirectory(dirPath: string): Promise<void> {
        await fs.promises.mkdir(dirPath, { recursive: true });
    }

    protected capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    protected toCamelCase(str: string): string {
        return str.replace(/[-_\s]+(.)?/g, (_, char) => char ? char.toUpperCase() : '');
    }

    protected toKebabCase(str: string): string {
        return str.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase();
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
            kebabCase: this.toKebabCase(aggregateName)
        };
    }

    protected generatePackageName(projectName: string, aggregateName: string, subPackage: string, ...additionalSubPackages: string[]): string {
        const basePackage = this.getBasePackage();
        const microservicePackage = `microservices.${aggregateName.toLowerCase()}`;
        const subPackages = [subPackage, ...additionalSubPackages].filter(p => p).join('.');
        return `${basePackage}.${projectName.toLowerCase()}.${microservicePackage}.${subPackages}`;
    }

    protected getBasePackage(): string {
        const config = getGlobalConfig();
        return config.getBasePackage();
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
            restController: '@RestController'
        };
    }

    protected getTransactionModel(): string {
        return 'SAGAS';
    }

    protected getDatabaseConfig(): any {
        return {
            type: 'postgresql',
            port: 5432,
            host: 'postgres',
            name: 'defaultdb',
            username: 'postgres',
            password: 'password'
        };
    }

    protected getDatabaseType(): string {
        return this.getDatabaseConfig().type;
    }

    protected getDatabasePort(): number {
        return this.getDatabaseConfig().port;
    }

    protected getDatabaseHost(): string {
        return this.getDatabaseConfig().host;
    }

    protected getDatabaseName(projectName?: string): string {
        return projectName ? projectName.toLowerCase() : this.getDatabaseConfig().name;
    }

    protected getDatabaseUsername(): string {
        return this.getDatabaseConfig().username;
    }

    protected getDatabasePassword(): string {
        return this.getDatabaseConfig().password;
    }

    protected getJdbcUrl(projectName?: string): string {
        const config = this.getDatabaseConfig();
        const dbName = this.getDatabaseName(projectName);

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

    protected hasFeature(features: string[], feature: string): boolean {
        return features.includes(feature);
    }

    protected hasAnyFeature(features: string[], ...checkFeatures: string[]): boolean {
        return checkFeatures.some(feature => this.hasFeature(features, feature));
    }

    protected resolveJavaType(type: any): string {
        return UnifiedTypeResolver.resolve(type);
    }

    protected isEntityType(type: any): boolean {
        return UnifiedTypeResolver.isEntityType(type);
    }

    protected isCollectionType(type: any): boolean {
        return UnifiedTypeResolver.isCollectionType(type);
    }

    protected buildPropertyInfo(entity: Entity): any[] {
        if (!entity.properties) return [];

        return entity.properties.map((property: any) => {
            const propertyName = property.name;
            const propertyType = this.resolveJavaType(property.type);
            const capitalizedName = this.capitalize(propertyName);
            const isFinal = property.isFinal || false;

            let isEnum = false;
            let enumType: string | undefined;
            if (property.type && typeof property.type === 'object' && property.type.$type === 'EntityType' && property.type.type) {
                const ref = property.type.type.ref;
                if (ref && typeof ref === 'object' && '$type' in ref && (ref as any).$type === 'EnumDefinition') {
                    isEnum = true;
                    enumType = (ref as any).name;
                } else if (property.type.type.$refText) {
                    const refText = property.type.type.$refText;
                    const javaType = this.resolveJavaType(property.type);
                    if (!this.isPrimitiveType(javaType) && !this.isEntityType(javaType) &&
                        !javaType.startsWith('List<') && !javaType.startsWith('Set<')) {
                        isEnum = true;
                        enumType = refText;
                    }
                }
            }

            return {
                name: propertyName,
                type: propertyType,
                capitalizedName,
                getter: `get${capitalizedName}`,
                setter: `set${capitalizedName}`,
                isId: propertyName.toLowerCase() === 'id',
                isCollection: this.isCollectionType(property.type),
                isEntity: this.isEntityType(property.type),
                isEnum,
                enumType,
                isFinal
            };
        });
    }

    protected isPrimitiveType(type: string): boolean {
        const primitiveTypes = ['String', 'Integer', 'Long', 'Boolean', 'Double', 'Float', 'LocalDateTime', 'LocalDate', 'BigDecimal', 'void'];
        return primitiveTypes.includes(type);
    }

    protected buildStandardImports(projectName: string, aggregateName: string): string[] {
        const lowerProject = projectName.toLowerCase();
        const lowerAggregate = aggregateName.toLowerCase();

        return [
            'import org.springframework.beans.factory.annotation.Autowired;',
            'import org.springframework.stereotype.Component;',
            'import org.springframework.stereotype.Service;',
            'import org.springframework.transaction.annotation.Transactional;',
            '',
            `import ${this.getBasePackage()}.${lowerProject}.microservices.${lowerAggregate}.aggregate.*;`,
            ''
        ];
    }

    protected combineImports(...importArrays: string[][]): string[] {
        const allImports = importArrays.flat();
        const uniqueImports = [...new Set(allImports.filter(imp => imp.trim() !== ''))];

        const javaImports = uniqueImports.filter(imp => imp.startsWith('import java.'));
        const springImports = uniqueImports.filter(imp => imp.startsWith('import org.springframework'));
        const basePackage = this.getBasePackage();
        const projectImports = uniqueImports.filter(imp => imp.startsWith(`import ${basePackage}`));
        const otherImports = uniqueImports.filter(imp =>
            !imp.startsWith('import java.') &&
            !imp.startsWith('import org.springframework') &&
            !imp.startsWith(`import ${basePackage}`)
        );

        const result = [];
        if (javaImports.length > 0) {
            result.push(...javaImports.sort(), '');
        }
        if (springImports.length > 0) {
            result.push(...springImports.sort(), '');
        }
        if (otherImports.length > 0) {
            result.push(...otherImports.sort(), '');
        }
        if (projectImports.length > 0) {
            result.push(...projectImports.sort(), '');
        }

        return result;
    }

    protected validateOptions(options: any, requiredFields: string[]): void {
        const missing = requiredFields.filter(field => !options[field]);
        if (missing.length > 0) {
            throw new Error(`Missing required options: ${missing.join(', ')}`);
        }
    }

    protected validateAggregate(aggregate: Aggregate): void {
        if (!aggregate.name) {
            throw new Error('Aggregate must have a name');
        }
        if (!aggregate.entities || aggregate.entities.length === 0) {
            throw new Error('Aggregate must have at least one entity');
        }
        const rootEntities = aggregate.entities.filter((e: any) => e.isRoot);
        if (rootEntities.length === 0) {
            throw new Error('Aggregate must have a root entity');
        }
        if (rootEntities.length > 1) {
            throw new Error('Aggregate can have only one root entity');
        }
    }

    protected loadTemplate(templatePath: string): string {
        const templateManager = TemplateManager.getInstance();
        return templateManager.loadRawTemplate(templatePath);
    }

    /**
     * Load and compile template using the cached template manager (preferred method)
     */
    protected loadCompiledTemplate(templatePath: string): HandlebarsTemplateDelegate<any> {
        const templateManager = TemplateManager.getInstance();
        return templateManager.loadTemplate(templatePath);
    }
}
