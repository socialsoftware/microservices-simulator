import * as path from 'path';
import { ConfigurationGeneratorPattern } from '../../common/generator-patterns.js';
import { ConfigurationGenerationOptions, ConfigContext } from './config-types.js';

export abstract class ConfigBaseGenerator extends ConfigurationGeneratorPattern {
    protected createConfigContext(options: ConfigurationGenerationOptions): ConfigContext {
        const { projectName, basePackage, architecture = 'default', outputDirectory } = options;
        const resourcesDir = path.join(outputDirectory, 'src', 'main', 'resources');
        return {
            projectName,
            basePackage,
            architecture,
            resourcesDir,
            outputDirectory
        };
    }
}
