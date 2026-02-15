import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { GeneratorContext, ProjectData, ConfigurationData, MetadataData } from "./template-data-types.js";
import { UnifiedTypeResolver } from "../unified-type-resolver.js";

export abstract class TemplateDataBase {
    
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

    protected hasFeature(features: string[] | undefined, feature: string): boolean {
        
        return true;
    }

    protected hasAnyFeature(features: string[] | undefined, ...checkFeatures: string[]): boolean {
        
        return true;
    }

    protected capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    protected toKebabCase(str: string): string {
        return str
            .replace(/([a-z])([A-Z])/g, '$1-$2')
            .replace(/[\s_]+/g, '-')
            .toLowerCase();
    }

    protected resolveJavaType(type: any): string {
        return UnifiedTypeResolver.resolve(type);
    }

    protected isCollectionType(type: any): boolean {
        return UnifiedTypeResolver.isCollectionType(type);
    }

    protected isEntityType(type: any): boolean {
        return UnifiedTypeResolver.isEntityType(type);
    }

    protected buildProjectData(context: GeneratorContext): ProjectData {
        return {
            name: context.projectName,
            packageName: context.packageName,
            architecture: context.architecture,
            javaPath: context.javaPath,
            version: '1.0.0',
            description: `${context.projectName} microservice`,
            author: 'DSL Generator',
            license: 'MIT',
            dependencies: this.buildDependencies(context),
            buildTool: 'maven',
            javaVersion: '17',
            springBootVersion: '3.2.0'
        };
    }

    protected buildConfigurationData(context: GeneratorContext): ConfigurationData {
        return {
            database: {
                driver: this.getDatabaseDriverClass(),
                url: this.getJdbcUrl(context.projectName),
                username: this.getDatabaseUsername(),
                password: this.getDatabasePassword(),
                dialect: this.getDatabaseDialect(),
                showSql: false,
                generateDdl: true
            },
            events: {
                enabled: this.hasFeature([], 'events'),
                broker: 'kafka',
                topics: this.generateEventTopics(context.projectName),
                serialization: 'json',
                compression: true
            },
            validation: {
                enabled: this.hasFeature([], 'validation'),
                failFast: false,
                customValidators: this.generateCustomValidators(context.projectName),
                groups: ['Default', 'Create', 'Update']
            },
            webApi: {
                enabled: this.hasFeature([], 'webapi'),
                basePath: `/api/v1/${context.projectName.toLowerCase()}`,
                version: '1.0',
                cors: true,
                swagger: true
            },
            coordination: {
                enabled: this.hasFeature([], 'coordination'),
                type: 'saga',
                timeout: 30000,
                retryAttempts: 3
            },
            saga: {
                enabled: this.hasFeature([], 'saga'),
                type: 'orchestration',
                compensationEnabled: true,
                timeout: 60000
            }
        };
    }

    protected buildMetadataData(aggregate: Aggregate, context: GeneratorContext): MetadataData {
        const entities = aggregate.entities || [];

        return {
            generatedAt: new Date().toISOString(),
            version: '1.0.0',
            generator: 'Nebula DSL Generator',
            statistics: {
                aggregates: 1,
                entities: entities.length,
                methods: this.countMethods(entities),
                workflows: this.countWorkflows(aggregate),
                validations: this.countValidations(entities)
            },
            checksums: this.generateChecksums(aggregate, context)
        };
    }

    protected buildDependencies(context: GeneratorContext): string[] {
        const baseDependencies = [
            'org.springframework.boot:spring-boot-starter',
            'org.springframework.boot:spring-boot-starter-web',
            'org.springframework.boot:spring-boot-starter-data-jpa',
            'org.springframework.boot:spring-boot-starter-validation'
        ];

        const featureDependencies: { [key: string]: string[] } = {
            'events': [
                'org.springframework.kafka:spring-kafka',
                'org.springframework.boot:spring-boot-starter-actuator'
            ],
            'webapi': [
                'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0',
                'org.springframework.boot:spring-boot-starter-security'
            ],
            'validation': [
                'org.hibernate.validator:hibernate-validator'
            ],
            'coordination': [
                'org.springframework.statemachine:spring-statemachine-core'
            ],
            'saga': [
                'org.axonframework:axon-spring-boot-starter'
            ],
            'testing': [
                'org.springframework.boot:spring-boot-starter-test',
                'org.testcontainers:postgresql',
                'org.testcontainers:kafka'
            ]
        };

        let dependencies = [...baseDependencies];

        
        Object.values(featureDependencies).forEach(deps => {
            dependencies.push(...deps);
        });

        return dependencies;
    }

    protected getDatabaseDriver(architecture: string): string {
        return this.getDatabaseDriverClass();
    }

    protected generateEventTopics(projectName: string): string[] {
        const baseName = projectName.toLowerCase().replace(/[^a-z0-9]/g, '-');
        return [
            `${baseName}-events`,
            `${baseName}-commands`,
            `${baseName}-notifications`,
            `${baseName}-saga-events`
        ];
    }

    protected generateCustomValidators(projectName: string): string[] {
        const packageBase = projectName.toLowerCase();
        return [
            `${packageBase}.validation.BusinessRuleValidator`,
            `${packageBase}.validation.UniqueValueValidator`,
            `${packageBase}.validation.DateRangeValidator`
        ];
    }

    protected countMethods(entities: Entity[]): number {
        return entities.reduce((count, entity) => {
            const methods = (entity as any).methods || [];
            return count + methods.length;
        }, 0);
    }

    protected countWorkflows(aggregate: Aggregate): number {
        return (aggregate as any).workflows?.length || 0;
    }

    protected countValidations(entities: Entity[]): number {
        return entities.reduce((count, entity) => {
            if (!entity.properties) return count;
            return count + entity.properties.filter(prop => this.hasValidationRules(prop)).length;
        }, 0);
    }

    protected hasValidationRules(property: any): boolean {
        return property.required ||
            property.minLength ||
            property.maxLength ||
            property.min ||
            property.max ||
            property.pattern ||
            property.customValidator;
    }

    protected generateChecksums(aggregate: Aggregate, context: GeneratorContext): { [filename: string]: string } {
        const checksums: { [filename: string]: string } = {};

        const aggregateName = aggregate.name;
        const entities = aggregate.entities || [];

        checksums[`${aggregateName}.java`] = this.generateChecksum(aggregate.name);

        entities.forEach(entity => {
            checksums[`${entity.name}.java`] = this.generateChecksum(entity.name);
            checksums[`${entity.name}Service.java`] = this.generateChecksum(`${entity.name}Service`);
            checksums[`${entity.name}Repository.java`] = this.generateChecksum(`${entity.name}Repository`);
        });

        if (this.hasAnyFeature([], 'webapi')) {
            entities.forEach(entity => {
                checksums[`${entity.name}Controller.java`] = this.generateChecksum(`${entity.name}Controller`);
            });
        }

        return checksums;
    }

    protected generateChecksum(content: string): string {
        let hash = 0;
        for (let i = 0; i < content.length; i++) {
            const char = content.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash;
        }
        return Math.abs(hash).toString(16);
    }

    protected getPropertyType(property: any): string {
        if (!property.type) return 'String';

        const type = property.type;

        if (typeof type === 'string') {
            return this.mapPrimitiveType(type);
        }

        if (type.$type === 'PrimitiveType') {
            return this.mapPrimitiveType(type.typeName);
        }

        if (type.$type === 'EntityType') {
            return type.entity.name;
        }

        if (type.$type === 'ListType') {
            const elementType = this.getPropertyType({ type: type.elementType });
            return `List<${elementType}>`;
        }

        return 'Object';
    }

    protected mapPrimitiveType(typeName: string): string {
        const typeMap: { [key: string]: string } = {
            'string': 'String',
            'int': 'Integer',
            'long': 'Long',
            'double': 'Double',
            'float': 'Float',
            'boolean': 'Boolean',
            'date': 'LocalDate',
            'datetime': 'LocalDateTime',
            'uuid': 'UUID',
            'decimal': 'BigDecimal'
        };

        return typeMap[typeName.toLowerCase()] || 'String';
    }

    protected isOptionalType(type: string): boolean {
        return type.includes('Optional') || type.endsWith('?');
    }

    protected getMinLength(type: string): number | undefined {
        const match = type.match(/minLength\((\d+)\)/);
        return match ? parseInt(match[1]) : undefined;
    }

    protected getMaxLength(type: string): number | undefined {
        const match = type.match(/maxLength\((\d+)\)/);
        return match ? parseInt(match[1]) : undefined;
    }

    protected getMinValue(type: string): number | undefined {
        const match = type.match(/min\((\d+)\)/);
        return match ? parseInt(match[1]) : undefined;
    }

    protected getMaxValue(type: string): number | undefined {
        const match = type.match(/max\((\d+)\)/);
        return match ? parseInt(match[1]) : undefined;
    }

    protected getPattern(type: string): string | undefined {
        const match = type.match(/pattern\("([^"]+)"\)/);
        return match ? match[1] : undefined;
    }

    protected getCustomValidator(type: string): string | undefined {
        const match = type.match(/validator\("([^"]+)"\)/);
        return match ? match[1] : undefined;
    }
}
