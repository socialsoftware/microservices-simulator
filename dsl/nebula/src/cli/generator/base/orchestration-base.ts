import * as fs from 'fs';
import * as path from 'path';
import { Aggregate, Entity } from "../../../language/generated/ast.js";
import { TypeResolver } from "./type-resolver.js";
import Handlebars from "handlebars";
import { getGlobalConfig } from "./config.js";

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

    protected generatePackageName(projectName: string, aggregateName: string, subPackage: string): string {
        const basePackage = this.getBasePackage();
        const microservicePackage = `microservices.${aggregateName.toLowerCase()}`;
        return `${basePackage}.${projectName.toLowerCase()}.${microservicePackage}.${subPackage}`;
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
        return TypeResolver.resolveJavaType(type);
    }

    protected isEntityType(type: any): boolean {
        return TypeResolver.isEntityType(type);
    }

    protected isCollectionType(type: any): boolean {
        if (!type) return false;
        if (typeof type === 'string') {
            return type.toLowerCase().includes('list') || type.toLowerCase().includes('set');
        }
        return type.$type === 'CollectionType' || type.type === 'Collection';
    }

    protected buildPropertyInfo(entity: Entity): any[] {
        if (!entity.properties) return [];

        return entity.properties.map((property: any) => {
            const propertyName = property.name;
            const propertyType = this.resolveJavaType(property.type);
            const capitalizedName = this.capitalize(propertyName);

            return {
                name: propertyName,
                type: propertyType,
                capitalizedName,
                getter: `get${capitalizedName}`,
                setter: `set${capitalizedName}`,
                isId: propertyName.toLowerCase() === 'id',
                isCollection: this.isCollectionType(property.type),
                isEntity: this.isEntityType(property.type)
            };
        });
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
        const currentFileUrl = import.meta.url;
        const currentFilePath = new URL(currentFileUrl).pathname;
        const currentDir = path.dirname(currentFilePath);
        const templateRoot = path.join(currentDir, '../../templates');
        const fullPath = path.join(templateRoot, templatePath);

        try {
            return fs.readFileSync(fullPath, 'utf-8');
        } catch (error) {
            throw new Error(`Failed to load template ${templatePath}: ${error instanceof Error ? error.message : String(error)}`);
        }
    }
}
