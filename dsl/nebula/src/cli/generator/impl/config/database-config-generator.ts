import * as path from 'path';
import { ConfigBaseGenerator } from './config-base-generator.js';
import { ConfigContext } from './config-types.js';

export class DatabaseConfigGenerator extends ConfigBaseGenerator {
    async generateDatabaseProperties(context: ConfigContext): Promise<void> {
        const content = this.getDatabasePropertiesTemplate();
        const filePath = path.join(context.resourcesDir, 'database.properties');
        await this.writeFile(filePath, content, 'database.properties');
    }

    private getDatabasePropertiesTemplate(): string {
        const properties = [
            '# Database Configuration Properties',
            '# This file contains database-specific settings',
            '',
            '# Connection Pool Settings',
            this.buildPropertyLine('spring.datasource.hikari.connection-timeout', '20000'),
            this.buildPropertyLine('spring.datasource.hikari.minimum-idle', '5'),
            this.buildPropertyLine('spring.datasource.hikari.maximum-pool-size', '20'),
            this.buildPropertyLine('spring.datasource.hikari.idle-timeout', '300000'),
            this.buildPropertyLine('spring.datasource.hikari.max-lifetime', '1200000'),
            this.buildPropertyLine('spring.datasource.hikari.auto-commit', 'true'),
            '',
            '# JPA/Hibernate Advanced Settings',
            this.buildPropertyLine('spring.jpa.properties.hibernate.dialect', 'org.hibernate.dialect.PostgreSQLDialect'),
            this.buildPropertyLine('spring.jpa.properties.hibernate.id.new_generator_mappings', 'true'),
            this.buildPropertyLine('spring.jpa.properties.hibernate.connection.provider_disables_autocommit', 'false'),
            this.buildPropertyLine('spring.jpa.properties.hibernate.cache.use_second_level_cache', 'false'),
            this.buildPropertyLine('spring.jpa.properties.hibernate.cache.use_query_cache', 'false'),
            this.buildPropertyLine('spring.jpa.properties.hibernate.generate_statistics', 'false'),
            this.buildPropertyLine('spring.jpa.properties.hibernate.jdbc.batch_size', '20'),
            this.buildPropertyLine('spring.jpa.properties.hibernate.order_inserts', 'true'),
            this.buildPropertyLine('spring.jpa.properties.hibernate.order_updates', 'true'),
            this.buildPropertyLine('spring.jpa.properties.hibernate.jdbc.batch_versioned_data', 'true'),
            '',
            '# Transaction Settings',
            this.buildPropertyLine('spring.transaction.default-timeout', '30'),
            this.buildPropertyLine('spring.transaction.rollback-on-commit-failure', 'true'),
            '',
            '# Database Migration Settings (if using Flyway/Liquibase)',
            this.buildPropertyLine('spring.flyway.enabled', 'false'),
            this.buildPropertyLine('spring.liquibase.enabled', 'false'),
            '',
            '# Development/Testing Settings',
            this.buildPropertyLine('spring.jpa.defer-datasource-initialization', 'true'),
            this.buildPropertyLine('spring.sql.init.mode', 'always'),
            '',
            '# Performance Monitoring',
            this.buildPropertyLine('spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS', '1000')
        ];

        return properties.join('\n') + '\n';
    }
}
