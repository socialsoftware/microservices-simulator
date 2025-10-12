import * as path from 'path';
import { ConfigurationGeneratorPattern } from '../../../base/generator-patterns.js';
import { ConfigurationGenerationOptions, ConfigContext } from './config-types.js';

export abstract class ConfigBaseGenerator extends ConfigurationGeneratorPattern {
    protected createConfigContext(options: ConfigurationGenerationOptions): ConfigContext {
        const { projectName, architecture = 'default', features = [], outputDirectory } = options;
        const resourcesDir = path.join(outputDirectory, 'src', 'main', 'resources');

        return {
            projectName,
            architecture,
            features,
            resourcesDir,
            outputDirectory
        };
    }
}
