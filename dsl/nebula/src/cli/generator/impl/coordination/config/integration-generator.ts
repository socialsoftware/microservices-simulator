import { Aggregate, Entity } from "../../../../../language/generated/ast.js";
import { OrchestrationBase } from "../../../base/orchestration-base.js";

export interface IntegrationGenerationOptions {
    architecture?: string;
    features?: string[];
    projectName: string;
}

export class IntegrationGenerator extends OrchestrationBase {
    async generateIntegration(aggregate: Aggregate, options: IntegrationGenerationOptions): Promise<{ [key: string]: string }> {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const results: { [key: string]: string } = {};

        results['application'] = await this.generateApplication(aggregate, rootEntity, options);

        results['configuration'] = await this.generateConfiguration(aggregate, rootEntity, options);

        results['properties'] = await this.generateProperties(aggregate, rootEntity, options);

        results['profiles'] = await this.generateProfiles(aggregate, rootEntity, options);

        return results;
    }

    private async generateApplication(aggregate: Aggregate, rootEntity: Entity, options: IntegrationGenerationOptions): Promise<string> {
        const context = this.buildApplicationContext(aggregate, rootEntity, options);
        const template = this.getApplicationTemplate();
        return this.renderTemplate(template, context);
    }

    private async generateConfiguration(aggregate: Aggregate, rootEntity: Entity, options: IntegrationGenerationOptions): Promise<string> {
        const context = this.buildConfigurationContext(aggregate, rootEntity, options);
        const template = this.getConfigurationTemplate();
        return this.renderTemplate(template, context);
    }

    private async generateProperties(aggregate: Aggregate, rootEntity: Entity, options: IntegrationGenerationOptions): Promise<string> {
        const context = this.buildPropertiesContext(aggregate, rootEntity, options);
        const template = this.getPropertiesTemplate();
        return this.renderTemplate(template, context);
    }

    private async generateProfiles(aggregate: Aggregate, rootEntity: Entity, options: IntegrationGenerationOptions): Promise<string> {
        const context = this.buildProfilesContext(aggregate, rootEntity, options);
        const template = this.getProfilesTemplate();
        return this.renderTemplate(template, context);
    }

    private buildApplicationContext(aggregate: Aggregate, rootEntity: Entity, options: IntegrationGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const imports = this.buildApplicationImports(aggregate, options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            projectName: options.projectName,
            lowerProjectName: options.projectName.toLowerCase(),
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}`,
            basePackage: this.getBasePackage(),
            imports,
            hasEventService: (aggregate as any).metadata?.hasEventService || !!options.features?.includes('events'),
            hasScheduling: (aggregate as any).metadata?.hasScheduling || !!options.features?.includes('scheduling'),
            hasRetry: (aggregate as any).metadata?.hasRetry || !!options.features?.includes('retry'),
            hasJpa: (aggregate as any).metadata?.hasJpa || !!options.features?.includes('jpa'),
            hasTransactions: (aggregate as any).metadata?.hasTransactions || !!options.features?.includes('transactions')
        };
    }

    private buildConfigurationContext(aggregate: Aggregate, rootEntity: Entity, options: IntegrationGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const imports = this.buildConfigurationImports(aggregate, options);
        const beans = this.buildConfigurationBeans(aggregate, options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            projectName: options.projectName,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.configuration`,
            basePackage: this.getBasePackage(),
            imports,
            beans,
            hasSagas: options.architecture === 'causal-saga',
            hasExternalDtos: options.architecture === 'default'
        };
    }

    private buildPropertiesContext(aggregate: Aggregate, rootEntity: Entity, options: IntegrationGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const properties = this.buildApplicationProperties(aggregate, options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            projectName: options.projectName,
            properties,
            hasSagas: options.architecture === 'causal-saga',
            hasExternalDtos: options.architecture === 'default'
        };
    }

    private buildProfilesContext(aggregate: Aggregate, rootEntity: Entity, options: IntegrationGenerationOptions): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const profiles = this.buildProfileConfigurations(aggregate, options);
        const imports = this.buildConfigurationImports(aggregate, options);

        return {
            aggregateName: capitalizedAggregate,
            lowerAggregate,
            projectName: options.projectName,
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.configuration`,
            basePackage: this.getBasePackage(),
            profiles,
            imports,
            hasSagas: options.architecture === 'causal-saga'
        };
    }

    private buildApplicationImports(aggregate: Aggregate, options: IntegrationGenerationOptions): string[] {
        const imports: string[] = [];

        imports.push('import org.springframework.beans.factory.InitializingBean;');
        imports.push('import org.springframework.beans.factory.annotation.Autowired;');
        imports.push('import org.springframework.boot.SpringApplication;');
        imports.push('import org.springframework.boot.autoconfigure.SpringBootApplication;');
        imports.push('import org.springframework.boot.autoconfigure.domain.EntityScan;');
        imports.push('import org.springframework.context.annotation.PropertySource;');
        imports.push('import org.springframework.data.jpa.repository.config.EnableJpaAuditing;');
        imports.push('import org.springframework.data.jpa.repository.config.EnableJpaRepositories;');
        imports.push('import org.springframework.transaction.annotation.EnableTransactionManagement;');
        if (options.features?.includes('retry')) {
            imports.push('import org.springframework.retry.annotation.EnableRetry;');
        }
        if (options.features?.includes('scheduling')) {
            imports.push('import org.springframework.scheduling.annotation.EnableScheduling;');
        }

        imports.push(`import ${this.getBasePackage()}.ms.domain.event.EventService;`);

        return imports;
    }

    private buildConfigurationImports(aggregate: Aggregate, options: IntegrationGenerationOptions): string[] {
        const imports: string[] = [];

        imports.push('import org.springframework.context.annotation.Bean;');
        imports.push('import org.springframework.context.annotation.Configuration;');
        imports.push('import org.springframework.context.annotation.Profile;');
        imports.push('import org.springframework.beans.factory.annotation.Autowired;');

        if (options.architecture === 'causal-saga') {
            imports.push(`import ${this.getBasePackage()}.ms.sagas.unitOfWork.SagaUnitOfWorkService;`);
            imports.push(`import ${this.getBasePackage()}.ms.causal.unitOfWork.CausalUnitOfWorkService;`);
        }

        return imports;
    }

    private buildConfigurationBeans(aggregate: Aggregate, options: IntegrationGenerationOptions): any[] {
        const beans: any[] = [];

        if (options.architecture === 'causal-saga') {
            beans.push({
                name: 'sagaUnitOfWorkService',
                type: 'SagaUnitOfWorkService',
                profile: 'sagas',
                description: 'Saga unit of work service for distributed transactions'
            });

        }

        return beans;
    }

    private buildApplicationProperties(aggregate: Aggregate, options: IntegrationGenerationOptions): any[] {
        const properties: any[] = [];

        properties.push({
            key: 'spring.application.name',
            value: options.projectName.toLowerCase(),
            description: 'Application name'
        });

        properties.push({
            key: 'spring.jpa.hibernate.ddl-auto',
            value: 'update',
            description: 'Hibernate DDL auto mode'
        });

        properties.push({
            key: 'spring.jpa.show-sql',
            value: 'true',
            description: 'Show SQL queries'
        });

        properties.push({
            key: 'spring.jpa.properties.hibernate.format_sql',
            value: 'true',
            description: 'Format SQL queries'
        });

        if (options.architecture === 'causal-saga') {
            properties.push({
                key: 'spring.profiles.active',
                value: 'sagas',
                description: 'Active Spring profiles'
            });
        }

        return properties;
    }

    private buildProfileConfigurations(aggregate: Aggregate, options: IntegrationGenerationOptions): any[] {
        const profiles: any[] = [];

        profiles.push({
            name: 'default',
            description: 'Default configuration profile',
            beans: [
                {
                    name: 'defaultConfiguration',
                    type: 'DefaultConfiguration',
                    implementation: 'DefaultConfigurationImpl'
                }
            ]
        });

        if (options.architecture === 'causal-saga') {
            profiles.push({
                name: 'sagas',
                description: 'Saga-based distributed transaction configuration',
                beans: [
                    {
                        name: 'sagaUnitOfWorkService',
                        type: 'SagaUnitOfWorkService',
                        implementation: 'SagaUnitOfWorkServiceImpl'
                    }
                ]
            });

        }

        return profiles;
    }

    private getApplicationTemplate(): string {
        return this.loadTemplate('config/application.hbs');
    }

    private getConfigurationTemplate(): string {
        return this.loadTemplate('config/configuration.hbs');
    }

    private getPropertiesTemplate(): string {
        return this.loadTemplate('config/properties.hbs');
    }

    private getProfilesTemplate(): string {
        return this.loadTemplate('config/profiles.hbs');
    }

    protected override renderTemplate(template: string, context: any): string {
        let result = template;

        if (context.imports) {
            const importsText = context.imports.map((imp: string) => imp).join('\n');
            result = result.replace(/\{\{#each imports\}\}[\s\S]*?\{\{\/each\}\}/g, importsText);
        }

        result = result.replace(/\{\{packageName\}\}/g, context.packageName);
        result = result.replace(/\{\{aggregateName\}\}/g, context.aggregateName);
        result = result.replace(/\{\{lowerAggregate\}\}/g, context.lowerAggregate);
        result = result.replace(/\{\{projectName\}\}/g, this.capitalize(context.projectName));
        result = result.replace(/\{\{lowerProjectName\}\}/g, context.projectName);

        result = result.replace(/\{\{#if hasEventService\}\}([\s\S]*?)\{\{\/if\}\}/g, context.hasEventService ? '$1' : '');
        result = result.replace(/\{\{#if hasScheduling\}\}([\s\S]*?)\{\{\/if\}\}/g, context.hasScheduling ? '$1' : '');
        result = result.replace(/\{\{#if hasRetry\}\}([\s\S]*?)\{\{\/if\}\}/g, context.hasRetry ? '$1' : '');
        result = result.replace(/\{\{#if hasJpa\}\}([\s\S]*?)\{\{\/if\}\}/g, context.hasJpa ? '$1' : '');
        result = result.replace(/\{\{#if hasTransactions\}\}([\s\S]*?)\{\{\/if\}\}/g, context.hasTransactions ? '$1' : '');
        result = result.replace(/\{\{#if hasSagas\}\}([\s\S]*?)\{\{\/if\}\}/g, context.hasSagas ? '$1' : '');
        result = result.replace(/\{\{#if hasExternalDtos\}\}([\s\S]*?)\{\{\/if\}\}/g, context.hasExternalDtos ? '$1' : '');

        if (context.beans) {
            const beansText = context.beans.map((bean: any) =>
                `    @Bean\n    @Profile("${bean.profile}")\n    public ${bean.type} ${bean.name}() {\n        // ${bean.description}\n        return new ${bean.type}Impl();\n    }`
            ).join('\n\n');
            result = result.replace(/\{\{#each beans\}\}[\s\S]*?\{\{\/each\}\}/g, beansText);
        }

        if (context.properties) {
            const propertiesText = context.properties.map((prop: any) =>
                `# ${prop.description}\n${prop.key}=${prop.value}`
            ).join('\n\n');
            result = result.replace(/\{\{#each properties\}\}[\s\S]*?\{\{\/each\}\}/g, propertiesText);
        }

        if (context.profiles) {
            const profilesText = context.profiles.map((profile: any) =>
                `@Configuration\n@Profile("${profile.name}")\npublic class ${this.capitalize(profile.name)}Configuration {\n    // ${profile.description}\n    \n${profile.beans.map((bean: any) => `    @Bean\n    public ${bean.type} ${bean.name}() {\n        return new ${bean.implementation}();\n    }`).join('\n\n')}\n}`
            ).join('\n\n');
            result = result.replace(/\{\{#each profiles\}\}[\s\S]*?\{\{\/each\}\}/g, profilesText);
        }

        result = result.replace(/\{\{[^}]*\}\}/g, '');



        return result;
    }

}
