export interface ConfigurationGenerationOptions {
    projectName: string;
    basePackage: string;
    architecture?: string;
    outputDirectory: string;
}

export interface ConfigContext {
    projectName: string;
    basePackage: string;
    architecture: string;
    resourcesDir: string;
    outputDirectory: string;
}
