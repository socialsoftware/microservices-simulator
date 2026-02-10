import * as path from 'path';
import { ConfigBaseGenerator } from './config-base-generator.js';
import { ConfigContext } from './config-types.js';

export class ApplicationConfigGenerator extends ConfigBaseGenerator {
    async generateApplicationProperties(context: ConfigContext): Promise<void> {
        const content = this.getApplicationPropertiesTemplate(context.projectName, context.architecture);
        const filePath = path.join(context.resourcesDir, 'application.properties');
        await this.writeFile(filePath, content, 'application.properties');
    }

    async generateApplicationYml(context: ConfigContext): Promise<void> {
        const content = this.getApplicationYmlTemplate(context.projectName, context.architecture);
        const filePath = path.join(context.resourcesDir, 'application.yml');
        await this.writeFile(filePath, content, 'application.yml');
    }

    private getApplicationPropertiesTemplate(projectName: string, architecture: string): string {
        const port = this.getPort(projectName);
        const serviceName = this.getServiceName(projectName);

        const properties = [
            '# Profile Configuration',
            this.buildPropertyLine('spring.profiles.active', '@activatedProperties@'),
            '',
            '# Maven Properties',
            this.buildPropertyLine('maven.basedir', '@project.basedir@'),
            '',
            '# Application Configuration',
            this.buildPropertyLine('spring.application.name', serviceName),
            this.buildPropertyLine('server.port', port),
            '',
            '# Database Configuration',
            this.buildPropertyLine('spring.datasource.url', this.getJdbcUrl(projectName)),
            this.buildPropertyLine('spring.datasource.driver-class-name', this.getDatabaseDriverClass()),
            this.buildPropertyLine('spring.datasource.username', this.getDatabaseUsername()),
            this.buildPropertyLine('spring.datasource.password', this.getDatabasePassword()),
            '',
            '# JPA Configuration',
            this.buildPropertyLine('spring.jpa.database-platform', this.getDatabaseDialect()),
            this.buildPropertyLine('spring.jpa.hibernate.ddl-auto', 'create-drop'),
            this.buildPropertyLine('spring.jpa.show-sql', 'true'),
            this.buildPropertyLine('spring.jpa.properties.hibernate.format_sql', 'true'),
            this.buildPropertyLine('spring.jpa.properties.jakarta.persistence.sharedCache.mode', 'UNSPECIFIED'),
            '',
            '# Scheduling Configuration',
            this.buildPropertyLine('spring.task.scheduling.pool.size', '5'),
            '',
            '# Retry Configuration',
            this.buildPropertyLine('retry.db.maxAttempts', '3'),
            this.buildPropertyLine('retry.db.delay', '200'),
            this.buildPropertyLine('retry.db.multiplier', '2'),
            '',
            '# Template Configuration',
            this.buildPropertyLine('spring.groovy.template.check-template-location', 'false')
        ];

        if (this.getDatabaseType() === 'h2') {
            properties.push(
                '',
                '# H2 Console (for development)',
                this.buildPropertyLine('spring.h2.console.enabled', 'true'),
                this.buildPropertyLine('spring.h2.console.path', '/h2-console')
            );
        }

        if (architecture === 'causal-saga') {
            properties.push(
                '',
                '# Causal Saga Configuration',
                this.buildPropertyLine('saga.coordination.enabled', 'true'),
                this.buildPropertyLine('saga.causal.consistency', 'true')
            );
        }

        // Saga Pattern Configuration (always enabled)
        properties.push(
            '',
            '# Saga Pattern Configuration',
            this.buildPropertyLine('saga.enabled', 'true'),
            this.buildPropertyLine('saga.timeout', '30000'),
            this.buildPropertyLine('saga.retry.max-attempts', '3')
        );

        // Event Configuration (always enabled)
        properties.push(
            '',
            '# Event Configuration',
            this.buildPropertyLine('spring.events.async', 'true'),
            this.buildPropertyLine('spring.events.thread-pool-size', '10')
        );

        // Validation Configuration (always enabled)
        properties.push(
            '',
            '# Validation Configuration',
            this.buildPropertyLine('validation.enabled', 'true'),
            this.buildPropertyLine('validation.fail-fast', 'false')
        );

        return properties.join('\n') + '\n';
    }

    private getApplicationYmlTemplate(projectName: string, architecture: string): string {
        const port = this.getPort(projectName);
        const serviceName = this.getServiceName(projectName);

        const springProperties = [
            this.buildYamlProperty('application', '', 1),
            this.buildYamlProperty('name', serviceName, 2),
            '',
            this.buildYamlProperty('datasource', '', 1),
            this.buildYamlProperty('url', this.getJdbcUrl(projectName), 2),
            this.buildYamlProperty('driver-class-name', this.getDatabaseDriverClass(), 2),
            this.buildYamlProperty('username', this.getDatabaseUsername(), 2),
            this.buildYamlProperty('password', this.getDatabasePassword(), 2),
            '',
            this.buildYamlProperty('jpa', '', 1),
            this.buildYamlProperty('database-platform', this.getDatabaseDialect(), 2),
            this.buildYamlProperty('hibernate', '', 2),
            this.buildYamlProperty('ddl-auto', 'create-drop', 3),
            this.buildYamlProperty('show-sql', 'true', 2),
            this.buildYamlProperty('properties', '', 2),
            this.buildYamlProperty('hibernate', '', 3),
            this.buildYamlProperty('format_sql', 'true', 4)
        ];

        if (this.getDatabaseType() === 'h2') {
            springProperties.push(
                '',
                this.buildYamlProperty('h2', '', 1),
                this.buildYamlProperty('console', '', 2),
                this.buildYamlProperty('enabled', 'true', 3),
                this.buildYamlProperty('path', '/h2-console', 3)
            );
        }

        const sections = [
            '# Application Configuration (YAML format)',
            this.buildYamlSection('spring', springProperties),
            '',
            this.buildYamlSection('server', [
                this.buildYamlProperty('port', port, 1)
            ])
        ];

        if (architecture === 'causal-saga') {
            sections.push(
                '',
                this.buildYamlSection('saga', [
                    this.buildYamlProperty('coordination', '', 1),
                    this.buildYamlProperty('enabled', 'true', 2),
                    this.buildYamlProperty('causal', '', 1),
                    this.buildYamlProperty('consistency', 'true', 2)
                ])
            );
        }

        // Saga Configuration (always enabled)
        sections.push(
            '',
            this.buildYamlSection('saga', [
                this.buildYamlProperty('enabled', 'true', 1),
                this.buildYamlProperty('timeout', '30000', 1),
                this.buildYamlProperty('retry', '', 1),
                this.buildYamlProperty('max-attempts', '3', 2)
            ])
        );

        // Event Configuration (always enabled)
        sections.push(
            '',
            this.buildYamlSection('events', [
                this.buildYamlProperty('async', 'true', 1),
                this.buildYamlProperty('thread-pool-size', '10', 1)
            ])
        );

        return sections.join('\n') + '\n';
    }

    protected override getServiceName(projectName: string): string {
        return projectName.toLowerCase().replace(/[^a-z0-9]/g, '-');
    }

    protected override getPort(projectName: string): number {
        let hash = 0;
        for (let i = 0; i < projectName.length; i++) {
            const char = projectName.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash;
        }
        return 8080 + (Math.abs(hash) % 1920);
    }
}
