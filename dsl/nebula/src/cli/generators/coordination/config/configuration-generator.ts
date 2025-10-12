import { BaseGenerationOptions } from '../../shared/types.js';
import { ConfigurationGenerationOptions } from './config-types.js';
import { ConfigBaseGenerator } from './config-base-generator.js';
import { ApplicationConfigGenerator } from './application-config-generator.js';
import { DatabaseConfigGenerator } from './database-config-generator.js';
import { LoggingConfigGenerator } from './logging-config-generator.js';
import { DockerConfigGenerator } from './docker-config-generator.js';

export { ConfigurationGenerationOptions } from './config-types.js';

export class ConfigurationGenerator extends ConfigBaseGenerator {
    private applicationConfigGenerator = new ApplicationConfigGenerator();
    private databaseConfigGenerator = new DatabaseConfigGenerator();
    private loggingConfigGenerator = new LoggingConfigGenerator();
    private dockerConfigGenerator = new DockerConfigGenerator();

    async generate(options: ConfigurationGenerationOptions, context: BaseGenerationOptions): Promise<void> {
        const configContext = this.createConfigContext(options);

        await this.ensureDirectory(configContext.resourcesDir);

        await this.applicationConfigGenerator.generateApplicationProperties(configContext);
        await this.applicationConfigGenerator.generateApplicationYml(configContext);

        await this.databaseConfigGenerator.generateDatabaseProperties(configContext);

        await this.loggingConfigGenerator.generateLoggingProperties(configContext);

        await this.dockerConfigGenerator.generateDockerfile(configContext);
        await this.dockerConfigGenerator.generateDockerCompose(configContext);
    }
}
